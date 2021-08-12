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
import com.alibaba.nacos.auth.common.AuthConfigs;
import com.alibaba.nacos.common.model.RestResult;
import com.alibaba.nacos.common.model.RestResultUtils;
import com.alibaba.nacos.config.server.utils.RequestUtil;
import com.alibaba.nacos.console.security.nacos.NacosAuthConfig;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.selector.AppAuthConfigSelector;
import com.alibaba.nacos.core.utils.NacosUserService;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.alibaba.nacos.api.common.Constants.ALL_PATTERN;
import static com.alibaba.nacos.config.server.constant.Constants.DEFAULT_ADMIN_USER_NAME;

/**
 * Application operation controller.
 *
 * @author candong.hong
 * @since 2.0.2.HX
 */
@RestController
@RequestMapping("/v1/auth/app")
public class ApplicationController {
    
    @Autowired(required = false)
    private AppAuthConfigSelector appAuthConfigSelector;

    @Autowired
    private AuthConfigs authConfigs;

    @Autowired
    private NacosUserService nacosUserService;

    public static final int THRESHOLD = 10;

    /**
     * get all app list.
     *
     * @return all app list.
     */
    @GetMapping
    @Secured(resource = NacosAuthConfig.CONSOLE_RESOURCE_NAME_PREFIX + "app", action = ActionTypes.READ)
    public RestResult<List<String>> listAll() throws NacosException {
        List<String> appList = Collections.emptyList();
        if (authConfigs.isAppAuthEnabled()) {
            appList =  appAuthConfigSelector.selectAll().stream().map(AppAuthConfig::getAppName).collect(Collectors.toList());
        }
        String username = RequestUtil.getSrcUserName(RequestUtil.getRequest());
        if (!appList.isEmpty()) {
            Optional<List<String>> appsOptional = nacosUserService.listUserAppsByModuleAndAction(username, ALL_PATTERN, "rw");
            if (!appsOptional.isPresent()) {
                return RestResultUtils.success(Collections.emptyList());
            } else if (!appsOptional.get().isEmpty()) {
                Collection<String> hasAppPermissions = appsOptional.get();
                if (hasAppPermissions.size() > THRESHOLD) {
                    hasAppPermissions = Sets.newHashSet(hasAppPermissions);
                }
                appList = appList.stream().filter(hasAppPermissions::contains).collect(Collectors.toList());
            }
        }
        if (DEFAULT_ADMIN_USER_NAME.equals(username)) {
            if (Collections.emptyList().equals(appList)) {
                return RestResultUtils.success(Collections.singletonList(ALL_PATTERN));
            }
            appList.add(ALL_PATTERN);
        }
        return RestResultUtils.success(appList);
    }
}