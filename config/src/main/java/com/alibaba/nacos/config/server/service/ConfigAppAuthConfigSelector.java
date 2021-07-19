package com.alibaba.nacos.config.server.service;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigQueryResponse;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.core.auth.AppAuthConfig;
import com.alibaba.nacos.core.auth.AppAuthConfigSelector;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * get app auth config from config center
 * @author candong.hong
 * @since 2021-7-16
 */
@Component
public class ConfigAppAuthConfigSelector implements AppAuthConfigSelector {

    private final ConfigQueryRequestHandler configQueryRequestHandler;

    /**
     * thread safe.
     */
    private final Gson gson = new Gson();

    @Value("${mtc.nacos.core.auth.namespace.id}")
    private String authNamespaceId;

    public ConfigAppAuthConfigSelector(ConfigQueryRequestHandler configQueryRequestHandler) {
        this.configQueryRequestHandler = configQueryRequestHandler;
    }

    @Override
    public AppAuthConfig select(String appId) throws NacosException {
        ConfigQueryRequest request = ConfigQueryRequest.build(appId, Constants.DEFAULT_GROUP, authNamespaceId);
        RequestMeta meta = new RequestMeta();
        meta.setClientIp(NetUtils.localIP());

        ConfigQueryResponse response = configQueryRequestHandler.handle(request, meta);
        return gson.fromJson(response.getContent(), AppAuthConfig.class);
    }
}