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
import com.alibaba.nacos.config.server.model.DetailsUser;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.console.utils.PasswordEncoderUtil;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.model.AppEnv;
import com.alibaba.nacos.core.model.AppPermission;
import com.alibaba.nacos.core.model.BaseUser;
import com.alibaba.nacos.core.selector.AppAuthConfigSelector;
import com.alibaba.nacos.core.utils.Loggers;
import com.alibaba.nacos.core.utils.NacosUserService;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.nacos.api.common.Constants.ALL_PATTERN;
import static com.alibaba.nacos.api.common.Constants.COLON;
import static com.alibaba.nacos.api.common.Constants.COMMA;
import static com.alibaba.nacos.config.server.constant.Constants.DEFAULT_ADMIN_USER_NAME;

/**
 * Custem user service.
 *
 * @author wfnuser
 * @author nkorange
 */
@Service
public class NacosUserDetailsServiceImpl implements UserDetailsService, NacosUserService {
    
    private Map<String, DetailsUser> userMap = new ConcurrentHashMap<>();
    
    @Autowired
    private UserPersistService userPersistService;

    @Autowired
    private PermissionPersistService permissionPersistService;
    
    @Autowired
    private AuthConfigs authConfigs;

    @Autowired
    private AppAuthConfigSelector appAuthConfigSelector;

    private void reloadFromAppAuthConfig(Map<String, DetailsUser> userMap) throws NacosException {
        List<AppAuthConfig> appAuthConfigs = appAuthConfigSelector.selectAll();
        for (AppAuthConfig appAuthConfig : appAuthConfigs) {
            Map<String, AppEnv> envs = appAuthConfig.getEnvs();
            envs.forEach((envName, appEnv) -> {
                DetailsUser appDetailsUser = new DetailsUser();
                appDetailsUser.setUsername(appAuthConfig.getAppName() + COLON + envName);
                appDetailsUser.setAppPermissions(appAuthConfig.getAppPermissions());
                userMap.put(appDetailsUser.getUsername(), appDetailsUser);
            });
        }
    }
    
    @Scheduled(initialDelay = 5000, fixedDelay = 30000)
    private void reload() {
        try {
            Map<String, DetailsUser> map = new ConcurrentHashMap<>(16);
            reloadFromAppAuthConfig(map);
            Page<DetailsUser> users = getUsersFromDatabase(1, Integer.MAX_VALUE);
            if (users == null) {
                if (!map.isEmpty()) {
                    userMap = map;
                }
                return;
            }
            Map<String, List<AppPermission>> permissionMap = permissionPersistService.findAllUserAppPermissions().stream()
                    .collect(Collectors.groupingBy(UserAppPermission::getUsername, Collectors
                            .mapping(p -> new AppPermission(p.getApp(), p.getModules(), p.getAction()), Collectors.toList())));

            for (DetailsUser detailsUser : users.getPageItems()) {
                detailsUser.setAppPermissions(permissionMap.getOrDefault(detailsUser.getUsername(), Collections.emptyList()));
                map.put(detailsUser.getUsername(), detailsUser);
            }
            userMap = map;
        } catch (Exception e) {
            Loggers.AUTH.warn("[LOAD-USERS] load failed", e);
        }
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("username cannot be null");
        }
        DetailsUser detailsUser = userMap.get(username);
        if (!authConfigs.isCachingEnabled()) {
            detailsUser = getUserFromDatabase(username);
        }
        
        if (detailsUser == null) {
            throw new UsernameNotFoundException(username);
        }
        return new NacosUserDetails(detailsUser);
    }

    @Override
    public Optional<BaseUser> getUser(String username) {
        return Optional.ofNullable(getDetailsUser(username));
    }

    @Override
    public Optional<List<AppPermission>> listUserAppPermission(String username) {
        return getUser(username).map(BaseUser::getAppPermissions).filter(CollectionUtils::isNotEmpty);
    }

    @Override
    public Optional<List<String>> listUserAppsByModuleAndAction(String username, String module, String action) {
        if (DEFAULT_ADMIN_USER_NAME.equals(username)) {
            return Optional.of(Collections.emptyList());
        }
        Optional<List<AppPermission>> appPermissionsOptional = listUserAppPermission(username);
        if (!appPermissionsOptional.isPresent()) {
            return Optional.empty();
        }
        List<String> userApps = Lists.newArrayList();
        for (AppPermission ap : appPermissionsOptional.get()) {
            boolean modulePattern = ALL_PATTERN.equals(ap.getModules()) || ap.getModules().contains(module);
            if (!ap.getAction().contains(action) || !modulePattern) {
                continue;
            }
            if (ALL_PATTERN.equals(ap.getAppName())) {
                return Optional.of(Collections.emptyList());
            }
            userApps.add(ap.getAppName());
        }
        return Optional.of(userApps).filter(list -> !list.isEmpty());
    }

    private List<AppPermission> getUserAppPermissions(String username) {
        return permissionPersistService.findUserAppPermissions(username).stream()
                .map(p -> new AppPermission(p.getApp(), p.getModules(), p.getAction()))
                .collect(Collectors.toList());
    }
    
    public void updateUserPassword(String username, String password) {
        userPersistService.updateUserPassword(username, password);
    }

    /**
     * update user.
     * @param username username.
     * @param password password.
     * @param kps kps.
     */
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
    
    public Page<DetailsUser> getUsersFromDatabase(int pageNo, int pageSize) {
        return userPersistService.getUsers(pageNo, pageSize);
    }
    
    public DetailsUser getDetailsUser(String username) {
        DetailsUser detailsUser = userMap.get(username);
        if (!authConfigs.isCachingEnabled()) {
            detailsUser = getUserFromDatabase(username);
        }
        return detailsUser;
    }
    
    public DetailsUser getUserFromDatabase(String username) {
        DetailsUser detailsUser = userPersistService.findUserByUsername(username);
        if (detailsUser != null) {
            detailsUser.setAppPermissions(getUserAppPermissions(username));
        }
        return detailsUser;
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
