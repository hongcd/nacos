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

import java.util.Collections;
import java.util.Set;

/**
 * namespace auth config.
 *
 * @author candong.hong
 * @since 2021-7-28
 */
public class NamespaceAuthConfig {

    private String namespaceId;

    private String namespaceName;

    private String module;

    private Set<String> ipWhiteList = Collections.emptySet();

    private Set<String> ipBlackList = Collections.emptySet();

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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Set<String> getIpWhiteList() {
        return ipWhiteList;
    }

    public void setIpWhiteList(Set<String> ipWhiteList) {
        this.ipWhiteList = ipWhiteList;
    }

    public Set<String> getIpBlackList() {
        return ipBlackList;
    }

    public void setIpBlackList(Set<String> ipBlackList) {
        this.ipBlackList = ipBlackList;
    }
}