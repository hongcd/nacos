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

package com.alibaba.nacos.config.server.auth;

import com.alibaba.nacos.config.server.configuration.ConditionOnExternalStorage;
import com.alibaba.nacos.config.server.model.Page;
import com.alibaba.nacos.config.server.service.repository.PaginationHelper;
import com.alibaba.nacos.config.server.service.repository.extrnal.ExternalStoragePersistServiceImpl;
import com.alibaba.nacos.config.server.utils.LogUtil;
import com.alibaba.nacos.config.server.utils.TimeUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.alibaba.nacos.config.server.service.repository.RowMapperManager.PERMISSION_ROW_MAPPER;
import static com.alibaba.nacos.config.server.service.repository.RowMapperManager.USER_APP_PERMISSION_ROW_MAPPER;

/**
 * Implemetation of ExternalPermissionPersistServiceImpl.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@Conditional(value = ConditionOnExternalStorage.class)
@Component
public class ExternalPermissionPersistServiceImpl implements PermissionPersistService {
    
    @Autowired
    private ExternalStoragePersistServiceImpl persistService;
    
    private JdbcTemplate jt;
    
    @PostConstruct
    protected void init() {
        jt = persistService.getJdbcTemplate();
    }
    
    @Override
    public Page<PermissionInfo> getPermissions(String role, int pageNo, int pageSize) {
        PaginationHelper<PermissionInfo> helper = persistService.createPaginationHelper();
        
        String sqlCountRows = "select count(*) from permissions where ";
        String sqlFetchRows = "select role,resource,action from permissions where ";
    
        String where = " role= ? ";
        List<String> params = new ArrayList<>();
        if (StringUtils.isNotBlank(role)) {
            params = Collections.singletonList(role);
        } else {
            where = " 1=1 ";
        }
        
        try {
            Page<PermissionInfo> pageInfo = helper
                    .fetchPage(sqlCountRows + where, sqlFetchRows + where, params.toArray(), pageNo,
                            pageSize, PERMISSION_ROW_MAPPER);
            
            if (pageInfo == null) {
                pageInfo = new Page<>();
                pageInfo.setTotalCount(0);
                pageInfo.setPageItems(new ArrayList<>());
            }
            
            return pageInfo;
            
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    /**
     * Execute add permission operation.
     *
     * @param role role string value.
     * @param resource resource string value.
     * @param action action string value.
     */
    @Override
    public void addPermission(String role, String resource, String action) {
        
        String sql = "INSERT into permissions (role, resource, action) VALUES (?, ?, ?)";
        
        try {
            jt.update(sql, role, resource, action);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }
    
    /**
     * Execute delete permission operation.
     *
     * @param role role string value.
     * @param resource resource string value.
     * @param action action string value.
     */
    @Override
    public void deletePermission(String role, String resource, String action) {
        
        String sql = "DELETE from permissions WHERE role=? and resource=? and action=?";
        try {
            jt.update(sql, role, resource, action);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e.toString(), e);
            throw e;
        }
    }

    @Override
    public List<UserAppPermission> findUserAppPermissions(String username) {
        String sql = "SELECT id, username, app, modules, action, src_user, gmt_create, gmt_modified "
                + "FROM users_app_permission WHERE username = ?";
        try {
            return jt.query(sql, USER_APP_PERMISSION_ROW_MAPPER, username);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e, e);
            throw e;
        }
    }

    @Override
    public List<UserAppPermission> findAllUserAppPermissions() {
        String sql = "SELECT id, username, app, modules, action, src_user, gmt_create, gmt_modified "
                + "FROM users_app_permission";
        try {
            return jt.query(sql, USER_APP_PERMISSION_ROW_MAPPER);
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e, e);
            throw e;
        }
    }

    @Override
    public Page<UserAppPermission> searchUserAppPermission(String username, String appName, int pageNo, int pageSize) {
        return executeThrowExLog(() -> {
            String countSql = "select count(*) from users_app_permission";
            String querySql = "select id, username, app, modules, action, src_user, gmt_create, gmt_modified from users_app_permission";
            List<Object> params = Lists.newArrayList();

            StringBuilder where = new StringBuilder();
            if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(appName)) {
                where.append(" where 1=1 ");
            }

            if (StringUtils.isNotBlank(username)) {
                where.append(" and username like ? ");
                params.add("%" + username + "%");
            }
            if (StringUtils.isNotBlank(appName)) {
                where.append(" and app like ? ");
                params.add("%" + appName + "%");
            }

            if (where.length() > 0) {
                countSql += where.toString();
                querySql += where.toString();
            }
            PaginationHelper<UserAppPermission> paginationHelper = persistService.createPaginationHelper();
            return paginationHelper.fetchPage(countSql, querySql, params.toArray(), pageNo, pageSize, USER_APP_PERMISSION_ROW_MAPPER);
        });
    }

    @Override
    public UserAppPermission getUserAppPermission(String username, String app) {
        return executeThrowExLog(() -> {
            String sql = "select id, username, app, modules, action, src_user, gmt_create, gmt_modified "
                    + "from users_app_permission where username = ? and app = ?";
            return DataAccessUtils.singleResult(jt.query(sql, USER_APP_PERMISSION_ROW_MAPPER, username, app));
        });
    }

    @Override
    public void addUserAppPermission(String username, String app, String modules, String action, String srcUser) {
        executeThrowExLog(() -> {
            Timestamp currentTimestamp = TimeUtils.getCurrentTime();
            String sql = "insert into users_app_permission(username, app, modules, action, src_user, gmt_create, gmt_modified) "
                    + "values(?, ?, ?, ?, ?, ?, ?)";
            return jt.update(sql, username, app, modules, action, srcUser, currentTimestamp, currentTimestamp);
        });
    }

    @Override
    public void updateUserAppPermission(String username, String app, String modules, String action, String srcUser) {
        executeThrowExLog(() -> {
            Timestamp currentTimestamp = TimeUtils.getCurrentTime();
            String sql = "update users_app_permission set modules = ?, action = ?, src_user = ?, gmt_modified = ? "
                    + "where username = ? and app = ?";
            return jt.update(sql, modules, action, srcUser, currentTimestamp, username, app);
        });
    }

    @Override
    public void deleteUserAppPermission(String username, String app) {
        executeThrowExLog(() -> {
            String sql = "delete from users_app_permission where username = ? and app = ?";
            return jt.update(sql, username, app);
        });
    }

    private <T> T executeThrowExLog(Supplier<T> task) {
        try {
            return task.get();
        } catch (CannotGetJdbcConnectionException e) {
            LogUtil.FATAL_LOG.error("[db-error] " + e, e);
            throw e;
        } catch (Exception e) {
            LogUtil.FATAL_LOG.error("[db-logic-error] " + e, e);
            throw e;
        }
    }
}
