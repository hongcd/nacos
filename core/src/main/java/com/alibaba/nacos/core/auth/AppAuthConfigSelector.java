package com.alibaba.nacos.core.auth;

import java.util.List;

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
     */
    AppAuthConfig select(String appId);
}