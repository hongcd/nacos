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

package com.alibaba.nacos.config.server.service;

import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.config.server.model.ConfigKey;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.config.server.service.repository.PersistService;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.selector.AppAuthConfigSelector;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * get app auth config from config center
 * @author candong.hong
 * @since 2021-7-16
 */
@Component
public class ConfigAppAuthConfigSelector implements AppAuthConfigSelector {

    private final ConfigQueryRequestHandler configQueryRequestHandler;

    private final PersistService persistService;

    /**
     * thread safe.
     */
    private final Gson gson = new Gson();

    @Value("${mtc.nacos.core.auth.namespace.id}")
    private String authNamespaceId;

    @Value("${mtc.nacos.core.auth.app.dataId.group:DEFAULT_GROUP}")
    private String authAppGroup;

    public ConfigAppAuthConfigSelector(ConfigQueryRequestHandler configQueryRequestHandler, PersistService persistService) {
        this.configQueryRequestHandler = configQueryRequestHandler;
        this.persistService = persistService;
    }

    @Override
    public AppAuthConfig select(String appId) throws NacosException {
        ConfigQueryRequest request = ConfigQueryRequest.build(appId, authAppGroup, authNamespaceId);
        RequestMeta meta = new RequestMeta();
        meta.setClientIp(NetUtils.localIP());

        ConfigQueryResponse response = configQueryRequestHandler.handle(request, meta);
        return gson.fromJson(response.getContent(), AppAuthConfig.class);
    }

    @Override
    public List<AppAuthConfig> selectAll() throws NacosException {
        Page<ConfigKey> page = persistService.findAllConfigKey(1, Integer.MAX_VALUE, authNamespaceId);
        List<AppAuthConfig> resultConfigs = new ArrayList<>(page.getTotalCount());
        for (ConfigKey configKey : page.getPageItems()) {
            if (!authAppGroup.equals(configKey.getGroup())) {
                continue;
            }
            AppAuthConfig appAuthConfig = select(configKey.getDataId());
            if (appAuthConfig != null) {
                resultConfigs.add(appAuthConfig);
            }
        }
        return resultConfigs;
    }
}