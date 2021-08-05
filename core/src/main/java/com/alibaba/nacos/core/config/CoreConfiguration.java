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

package com.alibaba.nacos.core.config;

import com.alibaba.nacos.core.admission.AdmissionControl;
import com.alibaba.nacos.core.admission.basic.IpAdmissionControl;
import com.alibaba.nacos.core.auth.AuthFilter;
import com.alibaba.nacos.core.cluster.ServerMemberManager;
import com.alibaba.nacos.core.selector.NamespaceAuthConfigSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * core configuration.
 *
 * @author candong.hong
 * @since 2021-7-29
 */
@Configuration
public class CoreConfiguration {
    
    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration() {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authFilter());
        registration.addUrlPatterns("/*");
        registration.setName("authFilter");
        registration.setOrder(6);
        
        return registration;
    }
    
    @Bean
    public AuthFilter authFilter() {
        return new AuthFilter();
    }

    @Bean
    @ConditionalOnBean(NamespaceAuthConfigSelector.class)
    AdmissionControl ipAdmissionControl(NamespaceAuthConfigSelector namespaceAuthConfigSelector, ServerMemberManager serverMemberManager) {
        return new IpAdmissionControl(serverMemberManager, namespaceAuthConfigSelector);
    }
}