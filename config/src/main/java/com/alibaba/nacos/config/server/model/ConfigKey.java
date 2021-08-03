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

package com.alibaba.nacos.config.server.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * ConfigKey.
 *
 * @author Nacos
 */
public class ConfigKey implements Serializable {
    
    private static final long serialVersionUID = -1748953484511867580L;
    
    private String appName;
    
    private String dataId;
    
    private String group;

    private String tenant;
    
    public ConfigKey() {
    }
    
    public ConfigKey(String appName, String dataId, String group, String tenant) {
        this(dataId, group, tenant);
        this.appName = appName;
    }

    public ConfigKey(String dataId, String group, String tenant) {
        this.dataId = dataId;
        this.group = group;
        this.tenant = tenant;
    }

    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public String getDataId() {
        return dataId;
    }
    
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigKey configKey = (ConfigKey) o;
        return Objects.equals(appName, configKey.appName)
                && dataId.equals(configKey.dataId)
                && group.equals(configKey.group) && tenant.equals(configKey.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, dataId, group, tenant);
    }
}
