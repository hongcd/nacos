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

import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.config.server.utils.LogUtil;
import com.alibaba.nacos.core.admission.AdmissionControl;
import com.alibaba.nacos.core.admission.AdmissionInfo;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IP admission control.
 *
 * @author candong.hong
 * @since 2021-7-28
 */
public class IpAdmissionControl implements AdmissionControl {

    private static final String AUTH_NAMESPACE_DATA_ID = "auth-namespace-config.yaml";

    @Value("${mtc.nacos.core.auth.namespace.id}")
    private String authNamespaceId;

    @Value("${mtc.nacos.core.auth.namespace.dataId.group:DEFAULT_GROUP}")
    private String authNamespaceGroup;

    private final ConfigQueryRequestHandler configQueryRequestHandler;

    private final Gson gson = new Gson();

    private Map<String, List<AuthNamespaceConfig>> authNamespaceIpMatcherMap = Collections.emptyMap();

    private final Set<String> localIps;

    public IpAdmissionControl(ConfigQueryRequestHandler configQueryRequestHandler) {
        this.configQueryRequestHandler = configQueryRequestHandler;
        this.localIps = Sets.newHashSet("127.0.0.1", "localhost", NetUtils.localIP());
        // this.localIps = Sets.newHashSet("127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", NetUtils.localIP());

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true).setNameFormat("ip-admission-control-thread-%d").build();
        ThreadPoolExecutor.DiscardPolicy discardPolicy = new ThreadPoolExecutor.DiscardPolicy();
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, threadFactory, discardPolicy);
        executor.scheduleWithFixedDelay(this::reload, 10L, 60, TimeUnit.SECONDS);
    }

    @Override
    public boolean access(AdmissionInfo admissionInfo) {
        if (localIps.contains(admissionInfo.getClientIp())) {
            return true;
        }
        List<AuthNamespaceConfig> authNamespaceConfigs = authNamespaceIpMatcherMap.get(admissionInfo.getTenant());
        if (CollectionUtils.isEmpty(authNamespaceConfigs)) {
            return true;
        }

        Predicate<Function<AuthNamespaceConfig, Stream<IpAddressMatcher>>> ipMatcher =
                mapMatcherStream -> authNamespaceConfigs.stream()
                        .flatMap(mapMatcherStream)
                        .anyMatch(ipAddressMatcher -> ipAddressMatcher.matches(admissionInfo.getClientIp()));
        if (ipMatcher.test(anc -> anc.getIpBlackMatcherList().stream())) {
            return false;
        }
        return ipMatcher.test(anc -> anc.getIpWhiteMatcherList().stream());
    }

    private synchronized void reload() {
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
            Type type = new TypeToken<ArrayList<AuthNamespaceConfig>>() {}.getType();
            List<AuthNamespaceConfig> authNamespaceConfigs = gson.fromJson(response.getContent(), type);
            authNamespaceIpMatcherMap = authNamespaceConfigs.stream().collect(Collectors.groupingBy(AuthNamespaceConfig::getNamespaceId));
        } catch (Exception e) {
            LogUtil.DEFAULT_LOG.error("reload namespace auth config failure! " + e.getMessage(), e);
        }
    }

    private static class AuthNamespaceConfig {

        private String namespaceId;

        private String namespaceName;

        private Set<String> ipWhiteList = Collections.emptySet();

        private Set<String> ipBlackList = Collections.emptySet();

        private List<IpAddressMatcher> ipWhiteMatcherList = Collections.emptyList();

        private List<IpAddressMatcher> ipBlackMatcherList = Collections.emptyList();

        public String getNamespaceId() {
            return namespaceId;
        }

        public void setNamespaceId(String namespaceId) {
            this.namespaceId = namespaceId;
        }

        public String getNamespaceName() {
            return namespaceName;
        }

        public void setNamespaceName(String namespaceName) {
            this.namespaceName = namespaceName;
        }

        public Set<String> getIpWhiteList() {
            return ipWhiteList;
        }

        public void setIpWhiteList(Set<String> ipWhiteList) {
            this.ipWhiteList = ipWhiteList;
        }

        public Set<String> getIpBlackList() {
            return ipBlackList;
        }

        public void setIpBlackList(Set<String> ipBlackList) {
            this.ipBlackList = ipBlackList;
        }

        public List<IpAddressMatcher> getIpWhiteMatcherList() {
            if (!ipWhiteList.isEmpty() && ipWhiteMatcherList.isEmpty()) {
                this.ipWhiteMatcherList = ipWhiteList.stream().map(IpAddressMatcher::new).collect(Collectors.toList());
            }
            return ipWhiteMatcherList;
        }

        public List<IpAddressMatcher> getIpBlackMatcherList() {
            if (!ipBlackList.isEmpty() && ipBlackMatcherList.isEmpty()) {
                this.ipBlackMatcherList = ipBlackList.stream().map(IpAddressMatcher::new).collect(Collectors.toList());
            }
            return ipBlackMatcherList;
        }
    }
}