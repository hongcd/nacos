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
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.config.server.model.selector.ConfigNamespaceAuthConfig;
import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.config.server.utils.LogUtil;
import com.alibaba.nacos.core.model.NamespaceAuthConfig;
import com.alibaba.nacos.core.selector.NamespaceAuthConfigSelector;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * config center implement {@link NamespaceAuthConfigSelector}.
 *
 * @author candong.hong
 * @since 2021-7-29
 */
public class ConfigNamespaceAuthConfigSelector implements NamespaceAuthConfigSelector {

    private static final String AUTH_NAMESPACE_DATA_ID = "auth-namespace-config";

    @Value("${hx.nacos.core.auth.namespace.id}")
    private String authNamespaceId;

    @Value("${hx.nacos.core.auth.namespace.dataId.group:DEFAULT_GROUP}")
    private String authNamespaceGroup;

    private final ConfigQueryRequestHandler configQueryRequestHandler;

    private final Gson gson = new Gson();

    private Map<String, ConfigNamespaceAuthConfig> configMap = Collections.emptyMap();

    public ConfigNamespaceAuthConfigSelector(ConfigQueryRequestHandler configQueryRequestHandler) {
        this.configQueryRequestHandler = configQueryRequestHandler;
    }

    @Override
    public NamespaceAuthConfig select(String namespaceId, String module) {
        return select(namespaceId, module, null);
    }

    @Override
    public NamespaceAuthConfig select(String namespaceId, String module, String action) {
        if (MapUtils.isEmpty(configMap) || !configMap.containsKey(namespaceId)) {
            return null;
        }
        ConfigNamespaceAuthConfig configNamespaceAuthConfig = configMap.get(namespaceId);
        ConfigNamespaceAuthConfig.Config config = configNamespaceAuthConfig.getDefaultConf();
        Map<String, ConfigNamespaceAuthConfig.Config> moduleConf = configNamespaceAuthConfig.getModuleConf();
        if (moduleConf != null && moduleConf.containsKey(module)) {
            config = moduleConf.get(module);
            ConfigNamespaceAuthConfig.Config defaultConf = configNamespaceAuthConfig.getDefaultConf();
            Boolean extendsDefault = defaultIfNull(config.getExtendsDefault(), defaultConf.getExtendsDefault());
            config.setExtendsDefault(defaultIfNull(extendsDefault, true));

            if (Boolean.TRUE.equals(config.getExtendsDefault())) {
                config = mergeDefaultConfig(config, defaultConf);
            }
        }

        NamespaceAuthConfig namespaceAuthConfig = new NamespaceAuthConfig();
        namespaceAuthConfig.setModule(module);
        namespaceAuthConfig.setNamespaceId(configNamespaceAuthConfig.getNamespaceId());
        namespaceAuthConfig.setNamespaceName(configNamespaceAuthConfig.getNamespaceName());
        if (config != null) {
            Pair<Set<String>, Set<String>> whiteBlackListPair = config.getIpConfigs().stream()
                    .filter(ipConfig -> ipConfig != null && (action == null || ipConfig.getAction().contains(action)))
                    .map(ipConfig -> Pair.of(ipConfig.getWhiteList(), ipConfig.getBlackList()))
                    .reduce(Pair.of(Sets.newHashSet(), Sets.newHashSet()), (mainPair, listPair) -> {
                        mainPair.getLeft().addAll(listPair.getLeft());
                        mainPair.getRight().addAll(listPair.getRight());
                        return mainPair;
                    });
            namespaceAuthConfig.setIpWhiteList(whiteBlackListPair.getLeft());
            namespaceAuthConfig.setIpBlackList(whiteBlackListPair.getRight());
        }
        return namespaceAuthConfig;
    }

    private ConfigNamespaceAuthConfig.Config mergeDefaultConfig(ConfigNamespaceAuthConfig.Config config,
                                                         ConfigNamespaceAuthConfig.Config defaultConfig) {
        ConfigNamespaceAuthConfig.Config newConfig = new ConfigNamespaceAuthConfig.Config();

        List<ConfigNamespaceAuthConfig.IpConfig> mergedIpConfigs = Lists.newArrayList(config.getIpConfigs());
        if (CollectionUtils.isNotEmpty(defaultConfig.getIpConfigs())) {
            mergedIpConfigs.addAll(defaultConfig.getIpConfigs());
        }
        Boolean extendsDefault = config.getExtendsDefault();
        if (extendsDefault == null) {
            extendsDefault = defaultIfNull(defaultConfig.getExtendsDefault(), true);
        }

        newConfig.setExtendsDefault(extendsDefault);
        newConfig.setIpConfigs(mergedIpConfigs);
        return newConfig;
    }

    /**
     * timer load namespace auth config.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Scheduled(cron = "* */1 * * * ?")
    public synchronized void loadConfig() {
        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setTenant(authNamespaceId);
        configQueryRequest.setGroup(authNamespaceGroup);
        configQueryRequest.setDataId(AUTH_NAMESPACE_DATA_ID);

        try {
            RequestMeta meta = new RequestMeta();
            meta.setClientIp(NetUtils.localIP());
            ConfigQueryResponse response = configQueryRequestHandler.handle(configQueryRequest, meta);
            if (!response.isSuccess()) {
                return;
            }
            Type type = new TypeToken<ArrayList<ConfigNamespaceAuthConfig>>()  { } .getType();
            List<ConfigNamespaceAuthConfig> authNamespaceConfigs = gson.fromJson(response.getContent(), type);
            configMap = Maps.uniqueIndex(authNamespaceConfigs, ConfigNamespaceAuthConfig::getNamespaceId);
            configMap = Collections.unmodifiableMap(configMap);
        } catch (Exception e) {
            LogUtil.DEFAULT_LOG.error("reload namespace auth config failure! " + e.getMessage(), e);
        }
    }
}