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

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.auth.AuthManager;
import com.alibaba.nacos.auth.exception.AccessException;
import com.alibaba.nacos.auth.model.Permission;
import com.alibaba.nacos.auth.model.User;
import com.alibaba.nacos.config.server.auth.RoleInfo;
import com.alibaba.nacos.config.server.model.ConfigKey;
import com.alibaba.nacos.config.server.model.DetailsUser;
import com.alibaba.nacos.config.server.service.repository.PersistService;
import com.alibaba.nacos.config.server.utils.LogUtil;
import com.alibaba.nacos.config.server.utils.RequestUtil;
import com.alibaba.nacos.console.security.nacos.roles.NacosRoleServiceImpl;
import com.alibaba.nacos.console.security.nacos.users.NacosUser;
import com.alibaba.nacos.console.security.nacos.users.NacosUserDetailsServiceImpl;
import com.alibaba.nacos.core.model.AppPermission;
import com.alibaba.nacos.core.utils.Loggers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nacos.api.common.Constants.ALL_PATTERN;
import static com.alibaba.nacos.api.common.Constants.COLON;
import static com.alibaba.nacos.api.remote.RemoteConstants.LABEL_MODULE_CONFIG;
import static com.alibaba.nacos.api.remote.RemoteConstants.LABEL_MODULE_NAMING;
import static com.alibaba.nacos.config.server.constant.Constants.DEFAULT_ADMIN_USER_NAME;

/**
 * Builtin access control entry of Nacos.
 *
 * @author nkorange
 * @since 1.2.0
 */
@Component
public class NacosAuthManager implements AuthManager {
    
    private static final String TOKEN_PREFIX = "Bearer ";
    
    private static final String PARAM_USERNAME = "username";
    
    private static final String PARAM_PASSWORD = "password";

    private static final String HEADER_AUTH_TYPE = "authType";

    private static final String HEADER_AUTH_TYPE_APP = "app";

    @Autowired
    private JwtTokenManager tokenManager;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private NacosRoleServiceImpl roleService;

    @Autowired
    private NacosUserDetailsServiceImpl nacosUserDetailsService;

    @Autowired
    private PersistService persistService;

    private LoadingCache<ConfigKey, Optional<String>> configAppNameCache;

    @PostConstruct
    void init() {
        configAppNameCache = CacheBuilder.newBuilder()
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(new CacheLoader<ConfigKey, Optional<String>>() {
                    @Override
                    public Optional<String> load(@Nonnull ConfigKey key) {
                        try {
                            ConfigKey configKey = persistService.findConfigKey(key.getDataId(), key.getGroup(), key.getTenant());
                            if (configKey == null) {
                                return Optional.empty();
                            }
                            return Optional.ofNullable(configKey.getAppName());
                        } catch (Exception e) {
                            Loggers.AUTH.error("[init] query data from config center, config: " + key + ", msg: " + e.getMessage(), e);
                            return Optional.empty();
                        }
                    }
                });
    }
    
    @Override
    public User login(Object request) throws AccessException {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = resolveToken(req);
        if (StringUtils.isBlank(token)) {
            throw new AccessException("user not found!");
        }
        
        try {
            tokenManager.validateToken(token);
        } catch (ExpiredJwtException e) {
            throw new AccessException("token expired!");
        } catch (Exception e) {
            throw new AccessException("token invalid!");
        }
        
        Authentication authentication = tokenManager.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String username = authentication.getName();
        NacosUser user = new NacosUser();
        user.setUsername(username);
        user.setToken(token);
        List<RoleInfo> roleInfoList = roleService.getRoles(username);
        if (roleInfoList != null) {
            for (RoleInfo roleInfo : roleInfoList) {
                if (roleInfo.getRole().equals(NacosRoleServiceImpl.GLOBAL_ADMIN_ROLE)) {
                    user.setGlobalAdmin(true);
                    break;
                }
            }
        }
        req.setAttribute(RequestUtil.NACOS_USER_KEY, user);
        return user;
    }
    
    @Override
    public User loginRemote(Object request) throws AccessException {
        Request req = (Request) request;
        String token = resolveToken(req);
        if (StringUtils.isBlank(token)) {
            throw new AccessException("user not found!");
        }
        
        try {
            tokenManager.validateToken(token);
        } catch (ExpiredJwtException e) {
            throw new AccessException("token expired!");
        } catch (Exception e) {
            throw new AccessException("token invalid!");
        }
        
        Authentication authentication = tokenManager.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String username = authentication.getName();
        NacosUser user = new NacosUser();
        user.setUsername(username);
        user.setToken(token);
        List<RoleInfo> roleInfoList = roleService.getRoles(username);
        if (roleInfoList != null) {
            for (RoleInfo roleInfo : roleInfoList) {
                if (roleInfo.getRole().equals(NacosRoleServiceImpl.GLOBAL_ADMIN_ROLE)) {
                    user.setGlobalAdmin(true);
                    break;
                }
            }
        }
        return user;
    }

    @Override
    public User unionLogin(Object request) throws AccessException {
        String authType = getRequestHeader(request, HEADER_AUTH_TYPE);
        if (!HEADER_AUTH_TYPE_APP.equals(authType)) {
            return request instanceof HttpServletRequest ? login(request) : loginRemote(request);
        }
        return loginHmac(request);
    }

    @Override
    public User loginHmac(Object request) throws AccessException {
        String env = getRequestHeader(request, "env");
        String appName = getRequestHeader(request, "AppName");
        String timestamp = getRequestHeader(request, "Timestamp");
        String accessKey = getRequestHeader(request, "Spas-AccessKey");
        String signature = getRequestHeader(request, "Spas-Signature");

        Authentication authenticate;
        try {
            Authentication authentication = new AppHmacAuthentication(appName, env, timestamp, accessKey, signature);
            authenticate = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            throw new AccessException("unknown user!");
        }
        if (authenticate == null) {
            throw new AccessException("unknown user!");
        }
        String token = tokenManager.createToken(authenticate.getName());
        Authentication authentication = tokenManager.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        NacosUser nacosUser = new NacosUser();
        nacosUser.setUsername(authentication.getName());
        nacosUser.setToken(token);
        return nacosUser;
    }

    @Override
    public void auth(Permission permission, User user) throws AccessException {
        if (Loggers.AUTH.isDebugEnabled()) {
            Loggers.AUTH.debug("auth permission: {}, user: {}", permission, user);
        }
        
        if (!roleService.hasPermission(user.getUsername(), permission)) {
            throw new AccessException("authorization failed!");
        }
        if (DEFAULT_ADMIN_USER_NAME.equals(user.getUsername())) {
            return;
        }

        int partLength = 3;
        String[] resourceParts = permission.getResource().split(COLON);
        if (resourceParts.length != partLength) {
            throw new IllegalArgumentException("resource: [" + permission.getResource() + "] invalid!");
        }
        String namespace = resourceParts[0];
        String group = resourceParts[1];
        String lastResource = resourceParts[2];

        if (ALL_PATTERN.equals(lastResource)) {
            return;
        }
        int moduleDataLength = 2;
        String[] moduleDataIdParts = lastResource.split("/");
        if (moduleDataIdParts.length != moduleDataLength) {
            throw new IllegalArgumentException("lastResource: [" + lastResource + "] invalid!");
        }
        String appName = null;
        String module = moduleDataIdParts[0];
        if (ALL_PATTERN.equals(moduleDataIdParts[1])) {
            return;
        }
        if (LABEL_MODULE_CONFIG.equals(module)) {
            String dataId = moduleDataIdParts[1];
            Optional<String> appOptional = configAppNameCache.getUnchecked(new ConfigKey(dataId, group, namespace));
            if (!appOptional.isPresent()) {
                LogUtil.DEFAULT_LOG.warn("config not found! dataId: {}, group: {}, tenant: {}", dataId, group, namespace);
                return;
            }
            appName = appOptional.get();
        } else if (LABEL_MODULE_NAMING.equals(module)) {
            appName = moduleDataIdParts[1];
        }

        DetailsUser detailsUserDetails = nacosUserDetailsService.getUser(user.getUsername());
        for (AppPermission ap : detailsUserDetails.getAppPermissions()) {
            if (ap.getAppName() != null && ap.getAppName().equals(appName) && ap.getAction().contains(permission.getAction())) {
                return;
            }
        }
        throw new AccessException("authorization failed!");
    }

    /**
     * get header from request.
     * @param request request, HttpServletRequest/Request
     * @param headerName header name
     * @return header value
     * @throws IllegalStateException request is not HttpServletRequest or Request
     */
    private String getRequestHeader(Object request, String headerName) {
        if (request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getHeader(headerName);
        } else if (request instanceof Request) {
            return ((Request) request).getHeader(headerName);
        }
        throw new IllegalStateException("illegal request type, request: " + request);
    }
    
    /**
     * Get token from header.
     */
    private String resolveToken(HttpServletRequest request) throws AccessException {
        String bearerToken = request.getHeader(NacosAuthConfig.AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        bearerToken = request.getParameter(Constants.ACCESS_TOKEN);
        if (StringUtils.isBlank(bearerToken)) {
            String userName = request.getParameter(PARAM_USERNAME);
            String password = request.getParameter(PARAM_PASSWORD);
            bearerToken = resolveTokenFromUser(userName, password);
        }
        
        return bearerToken;
    }
    
    /**
     * Get token from header.
     */
    private String resolveToken(Request request) throws AccessException {
        String bearerToken = request.getHeader(NacosAuthConfig.AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        bearerToken = request.getHeader(Constants.ACCESS_TOKEN);
        if (StringUtils.isBlank(bearerToken)) {
            String userName = request.getHeader(PARAM_USERNAME);
            String password = request.getHeader(PARAM_PASSWORD);
            bearerToken = resolveTokenFromUser(userName, password);
        }
        
        return bearerToken;
    }
    
    private String resolveTokenFromUser(String userName, String rawPassword) throws AccessException {
        String finalName;
        Authentication authenticate;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName,
                    rawPassword);
            authenticate = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            throw new AccessException("unknown user!");
        }
        
        if (null == authenticate || StringUtils.isBlank(authenticate.getName())) {
            finalName = userName;
        } else {
            finalName = authenticate.getName();
        }
        
        return tokenManager.createToken(finalName);
    }
}
