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

import React from 'react';
import PropTypes from 'prop-types';
import { Button, Dialog, Pagination, Table, ConfigProvider } from '@alifd/next';
import { connect } from 'react-redux';
import {
  createUserAppPermission,
  deleteUserAppPermission,
  searchUserAppPermissions,
} from '../../../reducers/authority';
import RegionGroup from '../../../components/RegionGroup';
import NewUserAppPermissions from './NewUserAppPermissions';

import './UserAppPermissionsManagement.scss';

@connect(
  state => ({
    userAppPermissions: state.authority.userAppPermissions,
  }),
  { searchUserAppPermissions }
)
@ConfigProvider.config
class UserAppPermissionsManagement extends React.Component {
  static displayName = 'UserAppPermissionsManagement';

  static propTypes = {
    locale: PropTypes.object,
    userAppPermissions: PropTypes.object,
    searchUserAppPermissions: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      pageNo: 1,
      pageSize: 9,
      username: '',
      app: '',
      createPermission: false,
    };
  }

  componentDidMount() {
    this.searchUserAppPermissions();
  }

  searchUserAppPermissions() {
    this.setState({ loading: true });
    const { pageNo, pageSize, username, app } = this.state;
    this.props
      .searchUserAppPermissions({ pageNo, pageSize, username, app })
      .then(() => {
        if (this.state.loading) {
          this.setState({ loading: false });
        }
      })
      .catch(() => this.setState({ loading: false }));
  }

  closeCreatePermission() {
    this.setState({ createPermissionVisible: false });
  }

  getActionText(action) {
    const { locale } = this.props;
    return {
      r: `${locale.readOnly} (r)`,
      w: `${locale.writeOnly} (w)`,
      rw: `${locale.readWrite} (rw)`,
    }[action];
  }

  getModuleText(module) {
    const { locale } = this.props;
    if ('*' === module) {
      return `${locale.allModule} (All)`;
    }
    return {
      config: `${locale.configOnly} (Config)`,
      naming: `${locale.namingOnly} (Naming)`,
    }[module];
  }

  render() {
    const { userAppPermissions, locale } = this.props;
    const { loading, pageSize, pageNo, createPermissionVisible } = this.state;
    return (
      <>
        <RegionGroup left={locale.privilegeManagement} />
        <div className="filter-panel">
          <Button
            type="primary"
            onClick={() => this.setState({ createPermissionVisible: true })}
            style={{ marginRight: 20 }}
          >
            {locale.addAppPermission}
          </Button>
          <Button type="secondary" onClick={() => this.searchUserAppPermissions()}>
            {locale.refresh}
          </Button>
        </div>
        <Table
          dataSource={userAppPermissions.pageItems}
          loading={loading}
          maxBodyHeight={476}
          fixedHeader
        >
          <Table.Column title={locale.username} dataIndex="username" />
          <Table.Column title={locale.app} dataIndex="app" />
          <Table.Column
            title={locale.module}
            dataIndex="modules"
            cell={modules => this.getModuleText(modules)}
          />
          <Table.Column
            title={locale.action}
            dataIndex="action"
            cell={action => this.getActionText(action)}
          />
          <Table.Column
            title={locale.operation}
            cell={(value, index, record) => (
              <>
                <Button
                  type="primary"
                  warning
                  onClick={() =>
                    Dialog.confirm({
                      title: locale.deletePermission,
                      content: locale.deletePermissionTip,
                      onOk: () =>
                        deleteUserAppPermission(record).then(() => {
                          this.setState({ pageNo: 1 }, () => this.searchUserAppPermissions());
                        }),
                    })
                  }
                >
                  {locale.deletePermission}
                </Button>
              </>
            )}
          />
        </Table>
        {userAppPermissions.totalCount > pageSize && (
          <Pagination
            className="users-pagination"
            current={pageNo}
            total={userAppPermissions.totalCount}
            pageSize={pageSize}
            onChange={pageNo => this.setState({ pageNo }, () => this.searchUserAppPermissions())}
          />
        )}
        <NewUserAppPermissions
          visible={createPermissionVisible}
          onOk={permission =>
            createUserAppPermission(permission).then(res => {
              this.setState({ pageNo: 1 }, () => this.searchUserAppPermissions());
              return res;
            })
          }
          onCancel={() => this.closeCreatePermission()}
        />
      </>
    );
  }
}

export default UserAppPermissionsManagement;
