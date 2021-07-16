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

package com.alibaba.nacos.console.security.nacos;

import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.console.security.nacos.users.NacosUserDetailsServiceImpl;
import com.alibaba.nacos.core.auth.AppAuthConfigSelector;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * app auth.
 *
 * @author candong.hong
 * @since 2021-7-9
 */
@Component
public class AppSecretAuthenticationProvider implements AuthenticationProvider {

    private final NacosUserDetailsServiceImpl userDetailsService;

    private final ConfigQueryRequestHandler configQueryRequestHandler;

    private final AppAuthConfigSelector appAuthConfigSelector;

    public AppSecretAuthenticationProvider(NacosUserDetailsServiceImpl userDetailsService,
                                           ConfigQueryRequestHandler configQueryRequestHandler,
                                           AppAuthConfigSelector appAuthConfigSelector) {
        this.userDetailsService = userDetailsService;
        this.configQueryRequestHandler = configQueryRequestHandler;
        this.appAuthConfigSelector = appAuthConfigSelector;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}