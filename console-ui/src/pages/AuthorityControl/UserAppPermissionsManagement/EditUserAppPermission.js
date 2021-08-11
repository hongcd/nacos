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

const FormItem = Form.Item;
const { Option } = Select;

const formItemLayout = {
  labelCol: { fixedSpan: 4 },
  wrapperCol: { span: 19 },
};

@ConfigProvider.config
class EditUserAppPermission extends React.Component {
  static displayName = 'EditUserAppPermission';

  field = new Field(this);

  static propTypes = {
    locale: PropTypes.object,
    visible: PropTypes.bool,
    onOk: PropTypes.func,
    onCancel: PropTypes.func,
    username: PropTypes.string,
    app: PropTypes.string,
    modules: PropTypes.string,
    action: PropTypes.string,
  };

  constructor(props) {
    super(props);
    this.state = {
      editUserAppPermission: {},
      editUserAppPermissionDialogVisible: false,
    };
    this.show = this.show.bind(this);
  }

  show(editUserAppPermission = {}) {
    this.setState({ editUserAppPermission });
  }

  handleModuleChange = selectedModule => {
    const { editUserAppPermission } = this.state;
    this.setState({ editUserAppPermission: { ...editUserAppPermission, modules: selectedModule } });
  };

  handleActionChange = selectedAction => {
    const { editUserAppPermission } = this.state;
    this.setState({ editUserAppPermission: { ...editUserAppPermission, action: selectedAction } });
  };

  check() {
    const { locale } = this.props;
    const errors = {
      username: locale.moduleError,
      app: locale.moduleError,
      modules: locale.moduleError,
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

  render() {
    const { getError } = this.field;
    const { onOk, onCancel, locale, visible } = this.props;
    const { editUserAppPermission } = this.state;

    return (
      <>
        <Dialog
          title={locale.editAppPermission}
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
            <FormItem label={locale.username} help={getError('username')}>
              <Input name="username" value={editUserAppPermission.username} disabled trim />
            </FormItem>
            <FormItem label={locale.app} help={getError('app')}>
              <Input name="app" value={editUserAppPermission.app} disabled trim />
            </FormItem>
            <FormItem label={locale.module} required help={getError('module')}>
              <Select
                name="modules"
                value={editUserAppPermission.modules}
                placeholder={locale.modulePlaceholder}
                style={{ width: '100%' }}
                onChange={this.handleModuleChange}
              >
                <Option value="config">{locale.configOnly}(Config)</Option>
                <Option value="naming">{locale.namingOnly}(Naming)</Option>
                <Option value="*">{locale.allModule}(All)</Option>
              </Select>
            </FormItem>
            <FormItem label={locale.action} required help={getError('action')}>
              <Select
                name="action"
                value={editUserAppPermission.action}
                placeholder={locale.actionPlaceholder}
                style={{ width: '100%' }}
                onChange={this.handleActionChange}
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

export default EditUserAppPermission;
