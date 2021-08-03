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

import com.alibaba.nacos.config.server.model.TenantInfo;
import com.alibaba.nacos.config.server.service.repository.PersistService;
import com.alibaba.nacos.config.server.utils.LogUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * nacos命名空间初始化.
 * @author candong.hong
 * @since 2021-7-16
 */
public class NacosAuthNamespaceCreateRunner implements ApplicationRunner {

    private static final String NAMESPACE_NAME = "nacos-auth";

    private final PersistService persistService;

    public NacosAuthNamespaceCreateRunner(PersistService persistService) {
        this.persistService = persistService;
    }

    @Value("${hx.nacos.core.auth.namespace.id}")
    private String namespaceId;

    @Value("${hx.nacos.core.auth.namespace.kp:0}")
    private String kp;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TenantInfo tenantInfo = persistService.findTenantByKp(kp, namespaceId);
        if (tenantInfo == null) {
            try {
                persistService.insertTenantInfoAtomic(kp, namespaceId, NAMESPACE_NAME, "nacos鉴权所用命名空间",
                        "nacos", System.currentTimeMillis());
            } catch (Exception e) {
                LogUtil.FATAL_LOG.warn("insert auth namespace failure!", e);
            }
        }
    }
}