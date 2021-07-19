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

    @Value("${mtc.nacos.core.auth.namespace.id}")
    private String namespaceId;

    @Value("${mtc.nacos.core.auth.namespace.kp:0}")
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