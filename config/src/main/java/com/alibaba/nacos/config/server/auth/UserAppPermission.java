package com.alibaba.nacos.config.server.auth;

import java.io.Serializable;

/**
 * user app permission.
 *
 * @author candong.hong
 * @since 2021-7-23
 */
public class UserAppPermission implements Serializable {

    private static final long serialVersionUID = -8127620186519999685L;

    private Long id;

    private String username;

    private String app;

    private String action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}