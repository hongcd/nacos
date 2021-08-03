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

package com.alibaba.nacos.config.server.model.selector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * namespace auth config.
 *
 * @author candong.hong
 * @since 2021-7-29
 */
public class ConfigNamespaceAuthConfig {

    private String namespaceId;

    private String namespaceName;

    private Config defaultConf;

    private Map<String, Config> moduleConf;

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public Config getDefaultConf() {
        return defaultConf;
    }

    public void setDefaultConf(Config defaultConf) {
        this.defaultConf = defaultConf;
    }

    public Map<String, Config> getModuleConf() {
        return moduleConf;
    }

    public void setModuleConf(Map<String, Config> moduleConf) {
        this.moduleConf = moduleConf;
    }

    public static class Config {

        private Boolean extendsDefault;

        private List<IpConfig> ipConfigs = Collections.emptyList();

        public Boolean getExtendsDefault() {
            return extendsDefault;
        }

        public void setExtendsDefault(Boolean extendsDefault) {
            this.extendsDefault = extendsDefault;
        }

        public List<IpConfig> getIpConfigs() {
            return ipConfigs;
        }

        public void setIpConfigs(List<IpConfig> ipConfigs) {
            this.ipConfigs = ipConfigs;
        }
    }

    public static class IpConfig {

        private Set<String> whiteList;

        private Set<String> blackList;

        private String action;

        private String desc;

        public Set<String> getWhiteList() {
            return whiteList;
        }

        public void setWhiteList(Set<String> whiteList) {
            this.whiteList = whiteList;
        }

        public Set<String> getBlackList() {
            return blackList;
        }

        public void setBlackList(Set<String> blackList) {
            this.blackList = blackList;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }
}