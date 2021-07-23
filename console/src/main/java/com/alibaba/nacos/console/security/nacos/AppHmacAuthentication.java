package com.alibaba.nacos.console.security.nacos;

import org.springframework.security.authentication.AbstractAuthenticationToken;

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
}