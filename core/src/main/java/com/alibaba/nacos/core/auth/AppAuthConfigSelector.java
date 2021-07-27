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

package com.alibaba.nacos.core.auth;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.core.model.AppAuthConfig;

import java.util.List;

/**
 * application auth config selector.
 * @author candong.hong
 * @since 2021-7-16
 */
public interface AppAuthConfigSelector {
    /**
     * select auth configuration.
     * @param appId appId
     * @return {@link AppAuthConfig} List
     * @throws NacosException nacosException
     */
    AppAuthConfig select(String appId) throws NacosException;

    /**
     * select all auth configuration.
     * @return {@link AppAuthConfig} list
     * @throws NacosException nacosException
     */
    List<AppAuthConfig> selectAll() throws NacosException;
}