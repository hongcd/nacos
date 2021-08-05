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

import com.alibaba.nacos.auth.model.User;

import java.io.Serializable;
import java.util.List;

/**
 * basic user info.
 * @author candong.hong
 * @since 2021-8-4
 */
public class BaseUser extends User implements Serializable {

    private static final long serialVersionUID = 7126717366793210881L;

    private boolean globalAdmin = false;

    private List<AppPermission> appPermissions;

    public boolean isGlobalAdmin() {
        return globalAdmin;
    }

    public void setGlobalAdmin(boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
    }

    public List<AppPermission> getAppPermissions() {
        return appPermissions;
    }

    public void setAppPermissions(List<AppPermission> appPermissions) {
        this.appPermissions = appPermissions;
    }
}