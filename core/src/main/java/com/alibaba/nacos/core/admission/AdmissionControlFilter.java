/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
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

package com.alibaba.nacos.core.admission;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.auth.annotation.Secured;
import com.alibaba.nacos.auth.common.AuthConfigs;
import com.alibaba.nacos.common.utils.ExceptionUtil;
import com.alibaba.nacos.core.code.ControllerMethodsCache;
import com.alibaba.nacos.core.remote.AbstractRequestFilter;
import com.alibaba.nacos.core.utils.Loggers;
import com.alibaba.nacos.core.utils.RequestType;
import com.alibaba.nacos.core.utils.WebUtils;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;

import static com.alibaba.nacos.auth.model.Resource.SPLITTER;

/**
 * admission control filter.
 *
 * @author candong.hong
 * @since 2021-7-27
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdmissionControlFilter extends AbstractRequestFilter implements Filter {

    private final ApplicationContext applicationContext;

    private final Collection<AdmissionControl> admissionControls;

    private final AuthConfigs authConfigs;

    private final ControllerMethodsCache methodsCache;

    public AdmissionControlFilter(ApplicationContext applicationContext, AuthConfigs authConfigs, ControllerMethodsCache methodsCache) {
        this.applicationContext = applicationContext;
        admissionControls = applicationContext.getBeansOfType(AdmissionControl.class).values();
        this.authConfigs = authConfigs;
        this.methodsCache = methodsCache;
    }

    @Override
    protected Response filter(Request request, RequestMeta meta, Class handlerClazz) throws NacosException {
        if (!authConfigs.isAuthEnabled()) {
            return null;
        }
        Method method = getHandleMethod(handlerClazz);
        try {
            AdmissionInfo admissionInfo = createAdmissionInfoFromMethod(request, method);
            admissionInfo.setClientIp(meta.getClientIp());
            admissionInfo.setRequestType(RequestType.GRPC);
            admissionInfo.setHeaderMap(request.getHeaders());
            if (filter(admissionInfo)) {
                return null;
            }
        } catch (Exception e) {
            Response defaultResponseInstance = getDefaultResponseInstance(handlerClazz);
            defaultResponseInstance.setErrorInfo(NacosException.SERVER_ERROR, ExceptionUtil.getAllExceptionMsg(e));
            return defaultResponseInstance;
        }
        if (Loggers.AUTH.isDebugEnabled()) {
            Loggers.AUTH.debug("access denied, request: {}", request.getClass().getSimpleName());
        }
        Response defaultResponseInstance = getDefaultResponseInstance(handlerClazz);
        defaultResponseInstance.setErrorInfo(NacosException.NO_RIGHT, "access denied, [admission]");
        return defaultResponseInstance;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!authConfigs.isAuthEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        Method method = methodsCache.getMethod(req);
        try {
            AdmissionInfo admissionInfo = createAdmissionInfoFromMethod(req, method);
            admissionInfo.setClientIp(WebUtils.getRemoteIp(req));
            admissionInfo.setRequestType(RequestType.HTTP);
            admissionInfo.setParamMap(req.getParameterMap());

            Enumeration<String> headerNames = req.getHeaderNames();
            ImmutableMap.Builder<String, String> headerMapBuilder = new ImmutableMap.Builder<>();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headerMapBuilder.put(headerName, req.getHeader(headerName));
            }
            admissionInfo.setHeaderMap(headerMapBuilder.build());
            if (filter(admissionInfo)) {
                chain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed," + e.getMessage());
            return;
        }
        if (Loggers.AUTH.isDebugEnabled()) {
            Loggers.AUTH.debug("access denied, request: {} {}", req.getMethod(), req.getRequestURI());
        }
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "access denied, [admission]");
    }

    private boolean filter(AdmissionInfo admissionInfo) {
        return admissionControls.stream().allMatch(ac -> ac.access(admissionInfo));
    }

    private AdmissionInfo createAdmissionInfoFromMethod(Object request, Method method) {
        AdmissionInfo admissionInfo = new AdmissionInfo();
        admissionInfo.setRequest(request);
        if (method == null || !method.isAnnotationPresent(Secured.class)) {
            return admissionInfo;
        }

        Secured secured = method.getAnnotation(Secured.class);
        admissionInfo.setAction(secured.action().toString());

        int resourceLength = 3;
        String[] resourceParts = Optional.of(secured.resource()).filter(StringUtils::isNotBlank)
                .orElseGet(() -> applicationContext.getBean(secured.parser()).parseName(request)).split(SPLITTER);

        if (resourceParts.length != resourceLength) {
            return admissionInfo;
        }
        admissionInfo.setTenant(resourceParts[0]);
        admissionInfo.setGroup(resourceParts[1]);

        int lastResourceLength = 2;
        String[] lastResourceParts = resourceParts[2].split("/");
        if (lastResourceParts.length != lastResourceLength) {
            return admissionInfo;
        }
        admissionInfo.setModule(lastResourceParts[0]);
        admissionInfo.setResource(lastResourceParts[1]);
        return admissionInfo;
    }
}