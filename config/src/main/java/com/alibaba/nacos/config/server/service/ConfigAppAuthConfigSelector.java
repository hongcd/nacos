package com.alibaba.nacos.config.server.service;

import com.alibaba.nacos.core.auth.AppAuthConfig;
import com.alibaba.nacos.core.auth.AppAuthConfigSelector;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * get app auth config from config center
 * @author candong.hong
 * @since 2021-7-16
 */
@Component
public class ConfigAppAuthConfigSelector implements AppAuthConfigSelector {
    @Override
    public AppAuthConfig select(String appId) {
        return null;
    }
}