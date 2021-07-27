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

package com.alibaba.nacos.core.model;

import java.io.Serializable;

/**
 * app permission model.
 *
 * @author candong.hong
 * @since 2021-7-26
 */
public class AppPermission implements Serializable {

    private static final long serialVersionUID = -8023159901078225478L;

    public AppPermission() {}

    public AppPermission(String appName, String action) {
        this.appName = appName;
        this.action = action;
    }

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