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

package com.alibaba.nacos.core.admission.basic;

import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.core.admission.AdmissionControl;
import com.alibaba.nacos.core.admission.AdmissionInfo;
import com.alibaba.nacos.core.cluster.Member;
import com.alibaba.nacos.core.cluster.ServerMemberManager;
import com.alibaba.nacos.core.model.NamespaceAuthConfig;
import com.alibaba.nacos.core.selector.NamespaceAuthConfigSelector;
import com.alibaba.nacos.sys.env.EnvUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IP admission control.
 *
 * @author candong.hong
 * @since 2021-7-28
 */
public class IpAdmissionControl implements AdmissionControl {

    private final Set<String> globalWhiteIps;

    private final NamespaceAuthConfigSelector namespaceAuthConfigSelector;

    private final LoadingCache<String, IpAddressMatcher> ipAddressMatcherCache;

    public IpAdmissionControl(ServerMemberManager serverMemberManager, NamespaceAuthConfigSelector namespaceAuthConfigSelector) {
        this.namespaceAuthConfigSelector = namespaceAuthConfigSelector;
        this.globalWhiteIps = Sets.newHashSet("127.0.0.1", "localhost", "0:0:0:0:0:0:0:1");
        for (Member member : serverMemberManager.allMembers()) {
            globalWhiteIps.add(member.getIp());
        }
        ipAddressMatcherCache = CacheBuilder.newBuilder()
                .concurrencyLevel(EnvUtil.getAvailableProcessors())
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, IpAddressMatcher>() {
                    @Override
                    public IpAddressMatcher load(@Nonnull String ip) {
                        return new IpAddressMatcher(ip);
                    }
                });
    }

    @Override
    public boolean access(AdmissionInfo admissionInfo) {
        String clientIp = admissionInfo.getClientIp();
        if (globalWhiteIps.contains(clientIp)) {
            return true;
        }
        String module = admissionInfo.getModule();
        String action = admissionInfo.getAction();
        NamespaceAuthConfig config = namespaceAuthConfigSelector.select(admissionInfo.getTenant(), module, action);
        if (config == null) {
            return true;
        }
        Set<String> whiteList = config.getIpWhiteList();
        Set<String> blackList = config.getIpBlackList();
        if (whiteList.isEmpty()) {
            return false;
        }
        boolean blackIp = blackList.stream().anyMatch(ip -> ipAddressMatcherCache.getUnchecked(ip).matches(clientIp));
        if (blackIp) {
            return false;
        }
        return whiteList.stream().anyMatch(ip -> ipAddressMatcherCache.getUnchecked(ip).matches(clientIp));
    }
}