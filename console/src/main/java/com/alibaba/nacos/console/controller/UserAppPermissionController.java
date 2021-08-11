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

package com.alibaba.nacos.console.controller;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.auth.annotation.Secured;
import com.alibaba.nacos.auth.common.ActionTypes;
import com.alibaba.nacos.auth.exception.AccessException;
import com.alibaba.nacos.common.model.RestResultUtils;
import com.alibaba.nacos.config.server.auth.RoleInfo;
import com.alibaba.nacos.config.server.auth.UserAppPermission;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.utils.RequestUtil;
import com.alibaba.nacos.console.security.nacos.NacosAuthConfig;
import com.alibaba.nacos.console.security.nacos.roles.NacosRoleServiceImpl;
import com.alibaba.nacos.core.utils.NacosUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.alibaba.nacos.api.common.Constants.ALL_PATTERN;
import static com.alibaba.nacos.config.server.constant.Constants.DEFAULT_ADMIN_USER_NAME;
import static com.alibaba.nacos.console.security.nacos.roles.NacosRoleServiceImpl.GLOBAL_ADMIN_ROLE;

/**
 * User App Permission operation controller.
 *
 * @author candong.hong
 * @since 2.0.2.HX
 */
@RestController
@RequestMapping("/v1/auth/userAppPermissions")
public class UserAppPermissionController {
    
    @Autowired
    private NacosRoleServiceImpl nacosRoleService;

    @Autowired
    private NacosUserService nacosUserService;
    
    /**
     * Query permissions of a role.
     *
     * @param username the username
     * @param app      the app
     * @param pageNo   page index
     * @param pageSize page size
     * @return permission of a role
     */
    @GetMapping
    @Secured(resource = NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX + "userAppPermissions", action = ActionTypes.READ)
    public Page<UserAppPermission> getPermissions(@RequestParam int pageNo, @RequestParam int pageSize,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String app) {
        return nacosRoleService.searchUserAppPermissionsFromDatabase(username, app, pageNo, pageSize);
    }
    
    /**
     * Add a app permission to a user.
     *
     * @param username     the username
     * @param app          the app
     * @param module       the related module
     * @param action       the related action
     * @return ok if succeed
     */
    @PostMapping
    @Secured(resource = NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX + "userAppPermissions", action = ActionTypes.WRITE)
    public Object addPermission(@RequestParam String username,
                                @RequestParam String app,
                                @RequestParam String modules,
                                @RequestParam String action) throws NacosException {
        String loginUser = RequestUtil.getSrcUserName(RequestUtil.getRequest());
        if (!hasPermission(loginUser, username, app)) {
            throw new AccessException("Permission denied");
        }
        nacosRoleService.addUserAppPermission(username, app, modules, action, loginUser);
        return RestResultUtils.success("add user app permission ok!");
    }

    /**
     * update a app permission.
     *
     * @param username     the username
     * @param app          the app
     * @param module       the related module
     * @param action       the related action
     * @return ok if succeed
     */
    @PutMapping
    @Secured(resource = NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX + "userAppPermissions", action = ActionTypes.WRITE)
    public Object updatePermission(@RequestParam String username,
                                   @RequestParam String app,
                                   @RequestParam String modules,
                                   @RequestParam String action) throws NacosException {
        String loginUser = RequestUtil.getSrcUserName(RequestUtil.getRequest());
        if (!hasPermission(loginUser, username, app)) {
            throw new AccessException("Permission denied");
        }
        nacosRoleService.updateUserAppPermission(username, app, modules, action, loginUser);
        return RestResultUtils.success("update user app permission ok!");
    }
    
    /**
     * Delete a app permission from a user.
     *
     * @param username     the username
     * @param app          the app
     * @return ok if succeed
     */
    @DeleteMapping
    @Secured(resource = NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX + "userAppPermissions", action = ActionTypes.WRITE)
    public Object deletePermission(@RequestParam String username, @RequestParam String app) throws AccessException {
        nacosRoleService.deleteUserAppPermission(username, app);
        String loginUser = RequestUtil.getSrcUserName(RequestUtil.getRequest());
        if (!hasPermission(loginUser, username, app)) {
            throw new AccessException("Permission denied");
        }
        return RestResultUtils.success("delete user app permission ok!");
    }

    private boolean hasPermission(String loginUser, String username, String app) {
        if (!DEFAULT_ADMIN_USER_NAME.equals(loginUser) && !loginUser.equals(username)) {
            Predicate<List<RoleInfo>> isAdminUser =
                    roles -> roles.stream().anyMatch(role -> GLOBAL_ADMIN_ROLE.equals(role.getRole()));
            if (!isAdminUser.test(nacosRoleService.getRoles(loginUser))) {
                return false;
            }
            if (isAdminUser.test(nacosRoleService.getRoles(username))) {
                return false;
            }
        }

        Optional<List<String>> appsOptional = nacosUserService.listUserAppsByModuleAndAction(loginUser, ALL_PATTERN, "rw");
        if (!appsOptional.isPresent()) {
            return false;
        }
        final List<String> apps = appsOptional.get();
        if (apps.isEmpty()) {
            return true;
        }
        return apps.stream().anyMatch(that -> that.equals(app));
    }
}
