package com.alibaba.nacos.core.auth;

import com.alibaba.nacos.api.exception.NacosException;

/**
 * application auth config selector.
 * @author candong.hong
 * @since 2021-7-16
 */
public interface AppAuthConfigSelector {
    /**
     * select auto configuration.
     * @param appId appId
     * @return {@link AppAuthConfig} List
     * @throws NacosException nacosException
     */
    AppAuthConfig select(String appId) throws NacosException;
}