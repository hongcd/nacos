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

package com.alibaba.nacos.client.utils;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.identify.IdentifyConstants;
import com.alibaba.nacos.common.utils.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.alibaba.nacos.api.common.Constants.APPNAME;
import static com.alibaba.nacos.api.exception.NacosException.CLIENT_INVALID_PARAM;

/**
 * env util.
 *
 * @author Nacos
 */
public class EnvUtil {
    
    public static final Logger LOGGER = LogUtils.logger(EnvUtil.class);
    
    public static void setSelfEnv(Map<String, List<String>> headers) {
        if (headers != null) {
            List<String> amorayTagTmp = headers.get(Constants.AMORY_TAG);
            if (amorayTagTmp == null) {
                if (selfAmorayTag != null) {
                    selfAmorayTag = null;
                    LOGGER.warn("selfAmoryTag:null");
                }
            } else {
                String amorayTagTmpStr = listToString(amorayTagTmp);
                if (!amorayTagTmpStr.equals(selfAmorayTag)) {
                    selfAmorayTag = amorayTagTmpStr;
                    LOGGER.warn("selfAmoryTag:{}", selfAmorayTag);
                }
            }
            
            List<String> vipserverTagTmp = headers.get(Constants.VIPSERVER_TAG);
            if (vipserverTagTmp == null) {
                if (selfVipserverTag != null) {
                    selfVipserverTag = null;
                    LOGGER.warn("selfVipserverTag:null");
                }
            } else {
                String vipserverTagTmpStr = listToString(vipserverTagTmp);
                if (!vipserverTagTmpStr.equals(selfVipserverTag)) {
                    selfVipserverTag = vipserverTagTmpStr;
                    LOGGER.warn("selfVipserverTag:{}", selfVipserverTag);
                }
            }
            List<String> locationTagTmp = headers.get(Constants.LOCATION_TAG);
            if (locationTagTmp == null) {
                if (selfLocationTag != null) {
                    selfLocationTag = null;
                    LOGGER.warn("selfLocationTag:null");
                }
            } else {
                String locationTagTmpStr = listToString(locationTagTmp);
                if (!locationTagTmpStr.equals(selfLocationTag)) {
                    selfLocationTag = locationTagTmpStr;
                    LOGGER.warn("selfLocationTag:{}", selfLocationTag);
                }
            }
        }
    }

    /**
     * init env and set properties.
     * @param properties properties
     * @throws NacosException missing mtc.properties
     */
    public static void initSystemAndPropertiesEnv(Properties properties) throws NacosException {
        Properties mtcProps = new Properties();
        try {
            mtcProps.load(EnvUtil.class.getClassLoader().getResourceAsStream("mtc.properties"));
        } catch (IOException e) {
            LOGGER.error("[initSystemProperties] failure load mtc.properties, please check, msg: " + e.getMessage(), e);
            throw new NacosException(CLIENT_INVALID_PARAM, e.getMessage(), e);
        }
        String appName = mtcProps.getProperty("app.name");
        if (StringUtils.isNotBlank(appName)) {
            System.setProperty(IdentifyConstants.PROJECT_NAME_PROPERTY, appName);
            properties.setProperty(APPNAME, appName);
        }
        String env = mtcProps.getProperty("env");
        if (StringUtils.isNotBlank(appName)) {
            System.setProperty("env", env);
            properties.setProperty("env", env);
        }
        String accessKey = mtcProps.getProperty("access-key");
        if (StringUtils.isNotBlank(accessKey)) {
            properties.setProperty(PropertyKeyConst.ACCESS_KEY, accessKey);
        }
        String secretKey = mtcProps.getProperty("secret-key");
        if (StringUtils.isNotBlank(secretKey)) {
            properties.setProperty(PropertyKeyConst.SECRET_KEY, secretKey);
        }
    }

    public static String getEnv() {
        return System.getProperty("env");
    }
    
    public static String getSelfAmorayTag() {
        return selfAmorayTag;
    }
    
    public static String getSelfVipserverTag() {
        return selfVipserverTag;
    }
    
    public static String getSelfLocationTag() {
        return selfLocationTag;
    }
    
    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String string : list) {
            result.append(string);
            result.append(",");
        }
        return result.toString().substring(0, result.length() - 1);
    }
    
    private static String selfAmorayTag;
    
    private static String selfVipserverTag;
    
    private static String selfLocationTag;
    
}