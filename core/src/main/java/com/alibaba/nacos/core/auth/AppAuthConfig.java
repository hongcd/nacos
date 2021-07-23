package com.alibaba.nacos.core.auth;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * application auth configuration.
 * @author candong.hong
 * @since 2021-7-16
 */
public class AppAuthConfig implements Serializable {

    private static final long serialVersionUID = -1980470523283845523L;

    private String appName;

    private String desc;

    private String pm;

    private List<AppPermission> appPermissions;

    private List<Permission> defaultPermissions;

    private Map<String, Env> envs;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPm() {
        return pm;
    }

    public void setPm(String pm) {
        this.pm = pm;
    }

    public List<AppPermission> getAppPermissions() {
        return appPermissions;
    }

    public void setAppPermissions(List<AppPermission> appPermissions) {
        this.appPermissions = appPermissions;
    }

    public List<Permission> getDefaultPermissions() {
        return defaultPermissions;
    }

    public void setDefaultPermissions(List<Permission> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public Map<String, Env> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, Env> envs) {
        this.envs = envs;
    }

    public static class AppPermission implements Serializable {

        private static final long serialVersionUID = -8023159901078225478L;

        private String appName;

        private String action;

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }


    public static class Permission extends com.alibaba.nacos.auth.model.Permission {

        private static final long serialVersionUID = -3682153401364313564L;

        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Env implements Serializable {

        private static final long serialVersionUID = 1693493542087638565L;

        private String accessKey;

        private String secretKey;

        private boolean extendsDefaultPermissions = true;

        private List<Permission> permissions;

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isExtendsDefaultPermissions() {
            return extendsDefaultPermissions;
        }

        public void setExtendsDefaultPermissions(boolean extendsDefaultPermissions) {
            this.extendsDefaultPermissions = extendsDefaultPermissions;
        }

        public List<Permission> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<Permission> permissions) {
            this.permissions = permissions;
        }
    }
}