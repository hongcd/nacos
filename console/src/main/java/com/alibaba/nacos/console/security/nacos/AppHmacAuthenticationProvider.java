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

package com.alibaba.nacos.console.security.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.impl.SpasAdapter;
import com.alibaba.nacos.config.server.utils.LogUtil;
import com.alibaba.nacos.core.selector.AppAuthConfigSelector;
import com.alibaba.nacos.core.model.AppAuthConfig;
import com.alibaba.nacos.core.model.AppEnv;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static com.alibaba.nacos.api.common.Constants.COLON;
import static com.alibaba.nacos.api.common.Constants.SERVICE_INFO_SPLITER;

/**
 * app auth.
 *
 * @author candong.hong
 * @since 2021-7-9
 */
@Component
public class AppHmacAuthenticationProvider implements AuthenticationProvider {

    private final AppAuthConfigSelector appAuthConfigSelector;

    public AppHmacAuthenticationProvider(AppAuthConfigSelector appAuthConfigSelector) {
        this.appAuthConfigSelector = appAuthConfigSelector;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String appName = (String) authentication.getPrincipal();
        AppAuthConfig appAuthConfig;
        try {
            appAuthConfig = appAuthConfigSelector.select(appName);
        } catch (NacosException e) {
            LogUtil.DEFAULT_LOG.error("[authenticate] failure select app auth config, errorMsg: " + e.getMessage(), e);
            return null;
        }
        if (appAuthConfig == null) {
            return null;
        }
        AppHmacAuthentication.AppHmacAuthConfig config = (AppHmacAuthentication.AppHmacAuthConfig) authentication.getCredentials();
        AppEnv appEnv = appAuthConfig.getEnvs().get(config.getEnv());
        if (appEnv == null) {
            return null;
        }
        if (!StringUtils.equals(appEnv.getAccessKey(), config.getAccessKey())) {
            return null;
        }
        long requestTimestamp = Long.parseLong(config.getTimestamp());
        long currentTimestamp = System.currentTimeMillis();
        int maxOffsetMilliseconds = 60000;
        if (requestTimestamp < (currentTimestamp - maxOffsetMilliseconds)
                || requestTimestamp > (currentTimestamp + maxOffsetMilliseconds)) {
            LogUtil.DEFAULT_LOG.warn("[authenticate] request expired, requestTimestamp: {}, currentTimestamp: {}",
                    requestTimestamp, currentTimestamp);
            throw new CredentialsExpiredException("[app hmac] request expired!");
        }
        long minuteTimestamp = Date.from(Instant.ofEpochMilli(requestTimestamp)
                .atZone(ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0)
                .toLocalDateTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()).getTime();
        String signatureData = appName + SERVICE_INFO_SPLITER + minuteTimestamp;
        String clientSign = config.getSignature();
        String serverSign = SpasAdapter.signWithHmacSha1Encrypt(signatureData, appEnv.getSecretKey());
        if (!StringUtils.equals(clientSign, serverSign)) {
            throw new CredentialsExpiredException("[app hmac] invalid sign");
        }
        String username = appName + COLON + config.getEnv();
        return new AppHmacAuthentication(username, config);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AppHmacAuthentication.class);
    }
}