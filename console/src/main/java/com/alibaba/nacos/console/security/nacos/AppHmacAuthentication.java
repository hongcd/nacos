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

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Objects;

/**
 * app hmac(ak/sk) authentication.
 * @author candong.hong
 * @since 2021-7-20
 */
public class AppHmacAuthentication extends AbstractAuthenticationToken {

    private final String appName;

    private final transient AppHmacAuthConfig appHmacAuthConfig;

    public AppHmacAuthentication(String appName, String env, String timestamp, String accessKey, String signature) {
        this(appName, new AppHmacAuthConfig(env, timestamp, accessKey, signature));
    }

    public AppHmacAuthentication(String appName, AppHmacAuthConfig appHmacAuthConfig) {
        super(null);
        this.appName = appName;
        this.appHmacAuthConfig = appHmacAuthConfig;
    }

    @Override
    public Object getCredentials() {
        return appHmacAuthConfig;
    }

    @Override
    public Object getPrincipal() {
        return appName;
    }

    public static class AppHmacAuthConfig {

        private final String env;

        private final String timestamp;

        private final String accessKey;

        private final String signature;

        private AppHmacAuthConfig(String env, String timestamp, String accessKey, String signature) {
            this.env = env;
            this.timestamp = timestamp;
            this.accessKey = accessKey;
            this.signature = signature;
        }

        public String getEnv() {
            return env;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public String getSignature() {
            return signature;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppHmacAuthentication)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AppHmacAuthentication that = (AppHmacAuthentication) o;
        return Objects.equals(appName, that.appName) && Objects.equals(appHmacAuthConfig, that.appHmacAuthConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appName, appHmacAuthConfig);
    }
}