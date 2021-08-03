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

package com.alibaba.nacos.core.selector;

import com.alibaba.nacos.core.model.NamespaceAuthConfig;

/**
 * namespace config selector.
 *
 * @author candong.hong
 * @since 2021-7-28
 */
public interface NamespaceAuthConfigSelector {
    /**
     * select config.
     * @param namespaceId namespaceId
     * @param module module
     * @return {@link NamespaceAuthConfig}
     */
    NamespaceAuthConfig select(String namespaceId, String module);

    /**
     * select config with action.
     * @param namespaceId namespaceId
     * @param module module
     * @param action action
     * @return {@link NamespaceAuthConfig}
     */
    NamespaceAuthConfig select(String namespaceId, String module, String action);
}