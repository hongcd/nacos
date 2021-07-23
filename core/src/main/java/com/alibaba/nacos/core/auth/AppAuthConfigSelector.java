package com.alibaba.nacos.core.auth;

import com.alibaba.nacos.api.exception.NacosException;

import java.util.List;

/**
 * application auth config selector.
 * @author candong.hong
 * @since 2021-7-16
 */
public interface AppAuthConfigSelector {
    /**
     * select auth configuration.
     * @param appId appId
     * @return {@link AppAuthConfig} List
     * @throws NacosException nacosException
     */
    AppAuthConfig select(String appId) throws NacosException;

    /**
     * select all auth configuration.
     * @return {@link AppAuthConfig} list
     * @throws NacosException nacosException
     */
    List<AppAuthConfig> selectAll() throws NacosException;
}