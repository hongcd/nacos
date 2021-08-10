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
import { Field, Form, Input, Select, Dialog, ConfigProvider } from '@alifd/next';
import { connect } from 'react-redux';
import { getApps, searchUsers } from '../../../reducers/authority';

const FormItem = Form.Item;
const { Option } = Select;

const formItemLayout = {
  labelCol: { fixedSpan: 4 },
  wrapperCol: { span: 19 },
};

@connect(
  state => ({
    apps: state.authority.apps,
    users: state.authority.users,
  }),
  { getApps, searchUsers }
)
@ConfigProvider.config
class NewUserAppPermissions extends React.Component {
  static displayName = 'NewUserAppPermissions';

  field = new Field(this);

  static propTypes = {
    locale: PropTypes.object,
    visible: PropTypes.bool,
    onOk: PropTypes.func,
    onCancel: PropTypes.func,
    getApps: PropTypes.func,
    searchUsers: PropTypes.func,
    apps: PropTypes.array,
  };

  state = {
    dataSource: [],
  };
  componentDidMount() {
    this.props.getApps();
  }

  check() {
    const { locale } = this.props;
    const errors = {
      username: locale.usernameError,
      app: locale.appError,
      module: locale.moduleError,
      action: locale.actionError,
    };
    const vals = Object.keys(errors).map(key => {
      const val = this.field.getValue(key);
      if (!val) {
        this.field.setError(key, errors[key]);
      }
      return val;
    });
    if (vals.filter(v => v).length === 4) {
      return vals;
    }
    return null;
  }

  handleChange = value => {
    if (value.length > 0) {
      searchUsers(value).then(val => {
        this.setState({ dataSource: val });
      });
    }
  };

  render() {
    const { getError } = this.field;
    const { visible, onOk, onCancel, locale, apps } = this.props;
    return (
      <>
        <Dialog
          title={locale.addAppPermission}
          visible={visible}
          onOk={() => {
            const validatedObj = this.check();
            if (validatedObj) {
              onOk(validatedObj).then(() => onCancel());
            }
          }}
          onClose={onCancel}
          onCancel={onCancel}
          afterClose={() => this.field.reset()}
        >
          <Form style={{ width: 400 }} {...formItemLayout} field={this.field}>
            <FormItem label={locale.username} required help={getError('username')}>
              <Select.AutoComplete
                name="username"
                style={{ width: 316 }}
                filterLocal={false}
                placeholder={locale.usernamePlaceholder}
                onChange={this.handleChange}
                dataSource={this.state.dataSource}
              />
            </FormItem>
            <FormItem label={locale.app} required help={getError('app')}>
              <Select name="app" placeholder={locale.appPlaceholder} style={{ width: '100%' }}>
                {apps.map(app => (
                  <Option value={`${app}`}>{app}</Option>
                ))}
              </Select>
            </FormItem>
            <FormItem label={locale.module} required help={getError('module')}>
              <Select
                name="module"
                placeholder={locale.modulePlaceholder}
                style={{ width: '100%' }}
              >
                <Option value="config">{locale.configOnly}(Config)</Option>
                <Option value="naming">{locale.namingOnly}(Naming)</Option>
                <Option value="*">{locale.allModule}(All)</Option>
              </Select>
            </FormItem>
            <FormItem label={locale.action} required help={getError('action')}>
              <Select
                name="action"
                placeholder={locale.actionPlaceholder}
                style={{ width: '100%' }}
              >
                <Option value="r">{locale.readOnly}(r)</Option>
                <Option value="w">{locale.writeOnly}(w)</Option>
                <Option value="rw">{locale.readWrite}(rw)</Option>
              </Select>
            </FormItem>
          </Form>
        </Dialog>
      </>
    );
  }
}

export default NewUserAppPermissions;
