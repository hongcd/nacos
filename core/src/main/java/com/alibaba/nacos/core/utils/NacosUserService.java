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

package com.alibaba.nacos.core.utils;

import com.alibaba.nacos.core.model.AppPermission;
import com.alibaba.nacos.core.model.BaseUser;

import java.util.List;
import java.util.Optional;

/**
 * nacos user service interface.
 * @author candong.hong
 * @since 2021-8-5
 */
public interface NacosUserService {

    /**
     * get user by username.
     * @param username username.
     * @return base user
     */
    Optional<BaseUser> getUser(String username);

    /**
     * get user app permission list.
     * @param username username.
     * @return user app permission list.
     */
    Optional<List<AppPermission>> listUserAppPermission(String username);

    /**
     * get user app list by modules and action.
     * @param username username.
     * @param modules modules.
     * @param action action.
     * @return user app list.
     */
    Optional<List<String>> listUserAppsByModuleAndAction(String username, String modules, String action);
}