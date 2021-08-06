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

package com.alibaba.nacos.console.security.nacos.roles;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.auth.common.AuthConfigs;
import com.alibaba.nacos.auth.model.Permission;
import com.alibaba.nacos.auth.model.User;
import com.alibaba.nacos.config.server.auth.PermissionInfo;
import com.alibaba.nacos.config.server.auth.PermissionPersistService;
import com.alibaba.nacos.config.server.auth.RoleInfo;
import com.alibaba.nacos.config.server.auth.RolePersistService;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.utils.RequestUtil;
import com.alibaba.nacos.console.security.nacos.NacosAuthConfig;
import com.alibaba.nacos.console.security.nacos.users.NacosUserDetailsServiceImpl;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.selector.AppAuthConfigSelector;
import com.alibaba.nacos.core.utils.Loggers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.alibaba.nacos.api.common.Constants.COLON;
import static com.alibaba.nacos.config.server.constant.Constants.DEFAULT_ADMIN_USER_NAME;

/**
 * Nacos builtin role service.
 *
 * @author nkorange
 * @since 1.2.0
 */
@Service
public class NacosRoleServiceImpl {
    
    public static final String GLOBAL_ADMIN_ROLE = "ROLE_ADMIN";
    
    private static final int DEFAULT_PAGE_NO = 1;
    
    @Autowired
    private AuthConfigs authConfigs;
    
    @Autowired
    private RolePersistService rolePersistService;
    
    @Autowired
    private NacosUserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private PermissionPersistService permissionPersistService;

    @Autowired
    private AppAuthConfigSelector appAuthConfigSelector;
    
    private volatile Set<String> roleSet = new ConcurrentHashSet<>();
    
    private volatile Map<String, List<RoleInfo>> roleInfoMap = new ConcurrentHashMap<>();
    
    private volatile Map<String, List<PermissionInfo>> permissionInfoMap = new ConcurrentHashMap<>();

    private void reloadFromAppAuthConfig(Set<String> roleSet, Map<String, List<RoleInfo>> roleInfoMap,
                                         Map<String, List<PermissionInfo>> permissionInfoMap) throws NacosException {
        List<AppAuthConfig> appAuthConfigs = appAuthConfigSelector.selectAll();
        for (AppAuthConfig appAuthConfig : appAuthConfigs) {
            String appName = appAuthConfig.getAppName();

            appAuthConfig.getEnvs().forEach((envName, env) -> {
                RoleInfo roleInfo = new RoleInfo();
                // like api-gateway:test
                roleInfo.setUsername(appName + COLON + envName);
                roleInfo.setRole("APP_ROLE" + COLON + roleInfo.getUsername());
                roleSet.add(roleInfo.getRole());
                roleInfoMap.put(roleInfo.getUsername(), Lists.newArrayList(roleInfo));

                String envResource = env.getNamespaceId() + ":*:*";
                PermissionInfo envPermissionInfo = new PermissionInfo(roleInfo.getRole(), envResource, "rw");
                permissionInfoMap.put(roleInfo.getRole(), Collections.singletonList(envPermissionInfo));
            });
        }
    }
    
    @Scheduled(initialDelay = 5000, fixedDelay = 30000)
    private void reload() {
        Set<String> tmpRoleSet = Sets.newHashSet();
        Map<String, List<RoleInfo>> tmpRoleInfoMap = new ConcurrentHashMap<>(16);
        Map<String, List<PermissionInfo>> tmpPermissionInfoMap = new ConcurrentHashMap<>(16);
        try {
            reloadFromAppAuthConfig(tmpRoleSet, tmpRoleInfoMap, tmpPermissionInfoMap);
            Page<RoleInfo> roleInfoPage = rolePersistService
                    .getRolesByUserName(StringUtils.EMPTY, DEFAULT_PAGE_NO, Integer.MAX_VALUE);
            if (roleInfoPage == null) {
                if (!tmpRoleSet.isEmpty() && !tmpRoleInfoMap.isEmpty() && !tmpPermissionInfoMap.isEmpty()) {
                    roleSet = tmpRoleSet;
                    roleInfoMap = tmpRoleInfoMap;
                    permissionInfoMap = tmpPermissionInfoMap;
                }
                return;
            }
            for (RoleInfo roleInfo : roleInfoPage.getPageItems()) {
                if (!tmpRoleInfoMap.containsKey(roleInfo.getUsername())) {
                    tmpRoleInfoMap.put(roleInfo.getUsername(), Lists.newArrayList());
                }
                tmpRoleInfoMap.get(roleInfo.getUsername()).add(roleInfo);
                tmpRoleSet.add(roleInfo.getRole());
            }

            for (String role : tmpRoleSet) {
                Page<PermissionInfo> permissionInfoPage = permissionPersistService
                        .getPermissions(role, DEFAULT_PAGE_NO, Integer.MAX_VALUE);
                if (!tmpPermissionInfoMap.containsKey(role) || !permissionInfoPage.getPageItems().isEmpty()) {
                    tmpPermissionInfoMap.put(role, permissionInfoPage.getPageItems());
                }
            }
            
            roleSet = tmpRoleSet;
            roleInfoMap = tmpRoleInfoMap;
            permissionInfoMap = tmpPermissionInfoMap;
        } catch (Exception e) {
            Loggers.AUTH.warn("[LOAD-ROLES] load failed", e);
        }
    }
    
    /**
     * Determine if the user has permission of the resource.
     *
     * <p>Note if the user has many roles, this method returns true if any one role of the user has the desired
     * permission.
     *
     * @param username   user info
     * @param permission permission to auth
     * @return true if granted, false otherwise
     */
    public boolean hasPermission(String username, Permission permission) {
        //update password
        if (NacosAuthConfig.UPDATE_PASSWORD_ENTRY_POINT.equals(permission.getResource())) {
            return true;
        }

        List<RoleInfo> roleInfoList = getRoles(username);
        if (CollectionUtils.isEmpty(roleInfoList)) {
            return false;
        }
        
        // Global admin pass:
        for (RoleInfo roleInfo : roleInfoList) {
            if (GLOBAL_ADMIN_ROLE.equals(roleInfo.getRole())) {
                return true;
            }
        }
        
        // Old global admin can pass resource 'console/':
        if (permission.getResource().startsWith(NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX)) {
            return false;
        }
        
        // For other roles, use a pattern match to decide if pass or not.
        for (RoleInfo roleInfo : roleInfoList) {
            List<PermissionInfo> permissionInfoList = getPermissions(roleInfo.getRole());
            if (CollectionUtils.isEmpty(permissionInfoList)) {
                continue;
            }
            for (PermissionInfo permissionInfo : permissionInfoList) {
                String permissionResource = permissionInfo.getResource().replaceAll("\\*", ".*");
                String permissionAction = permissionInfo.getAction();
                if (permissionAction.contains(permission.getAction()) && Pattern
                        .matches(permissionResource, permission.getResource())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public List<RoleInfo> getRoles(String username) {
        if (username == null) {
            return Collections.emptyList();
        }
        List<RoleInfo> roleInfoList = roleInfoMap.get(username);
        if (!authConfigs.isCachingEnabled()) {
            Page<RoleInfo> roleInfoPage = getRolesFromDatabase(username, DEFAULT_PAGE_NO, Integer.MAX_VALUE);
            if (roleInfoPage != null) {
                roleInfoList = roleInfoPage.getPageItems();
            }
        }
        return roleInfoList;
    }
    
    public Page<RoleInfo> getRolesFromDatabase(String userName, int pageNo, int pageSize) {
        Page<RoleInfo> roles = rolePersistService.getRolesByUserName(userName, pageNo, pageSize);
        if (roles == null) {
            return new Page<>();
        }
        return roles;
    }
    
    public List<PermissionInfo> getPermissions(String role) {
        if (role == null) {
            return Collections.emptyList();
        }
        List<PermissionInfo> permissionInfoList = permissionInfoMap.get(role);
        if (!authConfigs.isCachingEnabled()) {
            Page<PermissionInfo> permissionInfoPage = getPermissionsFromDatabase(role, DEFAULT_PAGE_NO, Integer.MAX_VALUE);
            if (permissionInfoPage != null) {
                permissionInfoList = permissionInfoPage.getPageItems();
            }
        }
        return permissionInfoList;
    }
    
    public Page<PermissionInfo> getPermissionsByRoleFromDatabase(String role, int pageNo, int pageSize) {
        return permissionPersistService.getPermissions(role, pageNo, pageSize);
    }
    
    /**
     * Add role.
     *
     * @param role     role name
     * @param username user name
     */
    public void addRole(String role, String username) {
        if (userDetailsService.getUserFromDatabase(username) == null) {
            throw new IllegalArgumentException("user '" + username + "' not found!");
        }
        User currentUser = RequestUtil.getUser();
        if (GLOBAL_ADMIN_ROLE.equals(role) && !DEFAULT_ADMIN_USER_NAME.equals(currentUser.getUsername())) {
            throw new IllegalArgumentException("role '" + GLOBAL_ADMIN_ROLE + "' is not permitted to create!");
        }
        rolePersistService.addRole(role, username);
        roleSet.add(role);
    }
    
    public void deleteRole(String role, String userName) {
        rolePersistService.deleteRole(role, userName);
    }
    
    public void deleteRole(String role) {
        rolePersistService.deleteRole(role);
        roleSet.remove(role);
    }
    
    public Page<PermissionInfo> getPermissionsFromDatabase(String role, int pageNo, int pageSize) {
        Page<PermissionInfo> pageInfo = permissionPersistService.getPermissions(role, pageNo, pageSize);
        if (pageInfo == null) {
            return new Page<>();
        }
        return pageInfo;
    }
    
    /**
     * Add permission.
     *
     * @param role     role name
     * @param resource resource
     * @param action   action
     */
    public void addPermission(String role, String resource, String action) {
        if (!roleSet.contains(role)) {
            throw new IllegalArgumentException("role " + role + " not found!");
        }
        permissionPersistService.addPermission(role, resource, action);
    }
    
    public void deletePermission(String role, String resource, String action) {
        permissionPersistService.deletePermission(role, resource, action);
    }
    
    public List<String> findRolesLikeRoleName(String role) {
        return rolePersistService.findRolesLikeRoleName(role);
    }
}
