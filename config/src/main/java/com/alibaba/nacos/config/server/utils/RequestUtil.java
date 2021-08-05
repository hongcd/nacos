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

package com.alibaba.nacos.config.server.utils;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.auth.model.User;
import com.alibaba.nacos.core.utils.WebUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Request util.
 *
 * @author Nacos
 */
public class RequestUtil {
    
    public static final String CLIENT_APPNAME_HEADER = "Client-AppName";
    
    public static final String NACOS_USER_KEY = "nacosuser";
    
    /**
     * get real client ip
     *
     * <p>first use X-Forwarded-For header    https://zh.wikipedia.org/wiki/X-Forwarded-For next nginx X-Real-IP last
     * {@link HttpServletRequest#getRemoteAddr()}
     *
     * @param request {@link HttpServletRequest}
     * @return remote ip address.
     */
    public static String getRemoteIp(HttpServletRequest request) {
        return WebUtils.getRemoteIp(request);
    }
    
    /**
     * Gets the name of the client application in the header.
     *
     * @param request {@link HttpServletRequest}
     * @return may be return null
     */
    public static String getAppName(HttpServletRequest request) {
        return request.getHeader(CLIENT_APPNAME_HEADER);
    }
    
    /**
     * Gets the user of the client application in the Attribute.
     *
     * @param request {@link HttpServletRequest}
     * @return may be return null
     */
    public static User getUser(HttpServletRequest request) {
        Object userObj = request.getAttribute(NACOS_USER_KEY);
        if (userObj == null) {
            return null;
        }
    
        return (User) userObj;
    }

    /**
     * get the user of the client application in the Attribute.
     * @return may be return null
     */
    public static User getUser() {
        return getUser(getRequest());
    }

    /**
     * get request.
     * @return request
     */
    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
    }

    /**
     * Gets the username of the client application in the Attribute.
     *
     * @param request {@link HttpServletRequest}
     * @return may be return null
     */
    public static String getSrcUserName(HttpServletRequest request) {
        User user = getUser(request);
        // If auth is disabled, get username from parameters by agreed key
        return user == null ? request.getParameter(Constants.USERNAME) : user.getUsername();
    }
    
}
