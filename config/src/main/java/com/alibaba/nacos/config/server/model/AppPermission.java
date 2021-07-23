package com.alibaba.nacos.config.server.model;

import java.io.Serializable;

/**
 * app permission model.
 *
 * @author candong.hong
 * @since 2021-7-23
 */
public class AppPermission implements Serializable {

    private static final long serialVersionUID = -3481301514964868957L;

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