/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.console.security.nacos.users;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.auth.common.AuthConfigs;
import com.alibaba.nacos.config.server.auth.PermissionPersistService;
import com.alibaba.nacos.config.server.auth.UserAppPermission;
import com.alibaba.nacos.config.server.auth.UserPersistService;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.model.User;
import com.alibaba.nacos.console.utils.PasswordEncoderUtil;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.auth.AppAuthConfigSelector;
import com.alibaba.nacos.core.model.AppPermission;
import com.alibaba.nacos.core.model.Env;
import com.alibaba.nacos.core.utils.Loggers;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.nacos.api.common.Constants.COLON;
import static com.alibaba.nacos.api.common.Constants.COMMA;

/**
 * Custem user service.
 *
 * @author wfnuser
 * @author nkorange
 */
@Service
public class NacosUserDetailsServiceImpl implements UserDetailsService {
    
    private Map<String, User> userMap = new ConcurrentHashMap<>();
    
    @Autowired
    private UserPersistService userPersistService;

    @Autowired
    private PermissionPersistService permissionPersistService;
    
    @Autowired
    private AuthConfigs authConfigs;

    @Autowired
    private AppAuthConfigSelector appAuthConfigSelector;

    private void reloadFromAppAuthConfig(Map<String, User> userMap) throws NacosException {
        List<AppAuthConfig> appAuthConfigs = appAuthConfigSelector.selectAll();
        for (AppAuthConfig appAuthConfig : appAuthConfigs) {
            Map<String, Env> envs = appAuthConfig.getEnvs();
            envs.forEach((envName, env) -> {
                User appUser = new User();
                appUser.setUsername(appAuthConfig.getAppName() + COLON + envName);
                appUser.setAppPermissions(appAuthConfig.getAppPermissions());
                userMap.put(appUser.getUsername(), appUser);
            });
        }
    }
    
    @Scheduled(initialDelay = 5000, fixedDelay = 30000)
    private void reload() {
        try {
            Map<String, User> map = new ConcurrentHashMap<>(16);
            reloadFromAppAuthConfig(map);
            Page<User> users = getUsersFromDatabase(1, Integer.MAX_VALUE);
            if (users == null) {
                if (!map.isEmpty()) {
                    userMap = map;
                }
                return;
            }

            Map<String, List<AppPermission>> permissionMap = permissionPersistService.findAllUserAppPermissions().stream()
                    .collect(Collectors.groupingBy(UserAppPermission::getUsername, Collectors
                            .mapping(p -> new AppPermission(p.getApp(), p.getAction()), Collectors.toList())));

            for (User user : users.getPageItems()) {
                user.setAppPermissions(permissionMap.getOrDefault(user.getUsername(), Collections.emptyList()));
                map.put(user.getUsername(), user);
            }
            userMap = map;
        } catch (Exception e) {
            Loggers.AUTH.warn("[LOAD-USERS] load failed", e);
        }
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        User user = userMap.get(username);
        if (!authConfigs.isCachingEnabled()) {
            user = getUserFromDatabase(username);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return new NacosUserDetails(user);
    }

    private List<AppPermission> getUserAppPermissions(String username) {
        return permissionPersistService.findUserAppPermissions(username).stream()
                .map(userAppPermission -> new AppPermission(userAppPermission.getApp(), userAppPermission.getAction()))
                .collect(Collectors.toList());
    }
    
    public void updateUserPassword(String username, String password) {
        userPersistService.updateUserPassword(username, password);
    }

    public void updateUser(String username, String password, List<String> kps) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        if (StringUtils.isBlank(password) && CollectionUtils.isEmpty(kps)) {
            return;
        }
        if (CollectionUtils.isEmpty(kps)) {
            userPersistService.updateUserPassword(username, PasswordEncoderUtil.encode(password));
            return;
        }
        StringJoiner sj = new StringJoiner(COMMA);
        kps.forEach(sj::add);
        userPersistService.updateKps(username, sj.toString());
    }
    
    public Page<User> getUsersFromDatabase(int pageNo, int pageSize) {
        return userPersistService.getUsers(pageNo, pageSize);
    }
    
    public User getUser(String username) {
        User user = userMap.get(username);
        if (!authConfigs.isCachingEnabled()) {
            user = getUserFromDatabase(username);
        }
        return user;
    }
    
    public User getUserFromDatabase(String username) {
        User user = userPersistService.findUserByUsername(username);
        if (user != null) {
            user.setAppPermissions(getUserAppPermissions(username));
        }
        return user;
    }

    public List<String> findUserLikeUsername(String username) {
        return userPersistService.findUserLikeUsername(username);
    }

    public void createUser(String username, String password) {
        userPersistService.createUser(username, password);
    }

    public void createUser(String username, String password, List<String> kps) {
        userPersistService.createUser(username, password, kps);
    }
    
    public void deleteUser(String username) {
        userPersistService.deleteUser(username);
    }
}
