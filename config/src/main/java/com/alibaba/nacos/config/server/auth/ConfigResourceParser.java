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

package com.alibaba.nacos.config.server.auth;

import com.alibaba.nacos.api.config.remote.request.ConfigBatchListenRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.auth.model.Resource;
import com.alibaba.nacos.auth.parser.ResourceParser;
import com.alibaba.nacos.common.utils.ReflectUtils;
import com.alibaba.nacos.common.utils.NamespaceUtil;
import com.alibaba.nacos.config.server.model.ConfigKey;
import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.config.server.service.repository.PersistService;
import com.alibaba.nacos.core.utils.Loggers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nacos.api.common.Constants.ALL_PATTERN;

/**
 * Config resource parser.
 *
 * @author nkorange
 * @since 1.2.0
 */
@Component
public class ConfigResourceParser implements ResourceParser {
    
    private static final String AUTH_CONFIG_PREFIX = "config/";

    @Autowired
    private PersistService persistService;

    private Cache<Triple<String, String, String>, String> dataAppNameCache;

    @PostConstruct
    private void init() {
        dataAppNameCache = CacheBuilder.newBuilder()
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(new CacheLoader<Triple<String, String, String>, String>() {
                    @Override
                    public String load(Triple<String, String, String> triple) {
                        try {
                            ConfigKey configKey = persistService.findConfigKey(triple.getLeft(), triple.getMiddle(), triple.getRight());
                            if (configKey == null) {
                                return ALL_PATTERN;
                            }
                            return configKey.getAppName();
                        } catch (Exception e) {
                            Loggers.AUTH.error("[init] query data from config center, config: " + triple + ", msg: " + e.getMessage(), e);
                            throw new InvalidCacheLoadException(e.getMessage());
                        }
                    }
                });
    }

    @Override
    public String parseName(Object requestObj) {
        
        String namespaceId = null;
        String groupName = null;
        String dataId = null;
        if (requestObj instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) requestObj;
            namespaceId = NamespaceUtil.processNamespaceParameter(req.getParameter("tenant"));
            groupName = req.getParameter("group");
            dataId = req.getParameter("dataId");
        } else if (requestObj instanceof Request) {
            Request request = (Request) requestObj;
            if (request instanceof ConfigBatchListenRequest) {
                List<ConfigBatchListenRequest.ConfigListenContext> configListenContexts = ((ConfigBatchListenRequest) request)
                        .getConfigListenContexts();
                if (!configListenContexts.isEmpty()) {
                    namespaceId = ((ConfigBatchListenRequest) request).getConfigListenContexts().get(0).getTenant();
                }
            } else {
                namespaceId = (String) ReflectUtils.getFieldValue(request, "tenant", "");
                
            }
            groupName = (String) ReflectUtils.getFieldValue(request, "group", "");
            dataId = (String) ReflectUtils.getFieldValue(request, "dataId", "");
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.isNotBlank(namespaceId)) {
            sb.append(namespaceId);
        }
        
        if (StringUtils.isBlank(groupName)) {
            sb.append(Resource.SPLITTER).append("*");
        } else {
            sb.append(Resource.SPLITTER).append(groupName);
        }


        if (StringUtils.isBlank(dataId)) {
            sb.append(Resource.SPLITTER).append(AUTH_CONFIG_PREFIX).append("*");
        } else {
            sb.append(Resource.SPLITTER).append(AUTH_CONFIG_PREFIX).append(dataId);
        }
        
        return sb.toString();
    }
}
