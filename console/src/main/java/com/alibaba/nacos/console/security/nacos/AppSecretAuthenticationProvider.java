package com.alibaba.nacos.console.security.nacos;

import com.alibaba.nacos.config.server.remote.ConfigQueryRequestHandler;
import com.alibaba.nacos.console.security.nacos.users.NacosUserDetailsServiceImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 应用鉴权提供者
 * @author candong.hong
 * @since 2021-7-9
 */
@Component
public class AppSecretAuthenticationProvider implements AuthenticationProvider {
    private final NacosUserDetailsServiceImpl userDetailsService;
    private final ConfigQueryRequestHandler configQueryRequestHandler;

    public AppSecretAuthenticationProvider(NacosUserDetailsServiceImpl userDetailsService,
                                           ConfigQueryRequestHandler configQueryRequestHandler) {
        this.userDetailsService = userDetailsService;
        this.configQueryRequestHandler = configQueryRequestHandler;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        if ("hongcd".equals(username) && "hongcd".equals(password)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername("nacos");
            return new UsernamePasswordAuthenticationToken(userDetails, "nacos", userDetails.getAuthorities());
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
