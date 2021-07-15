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
import { Field, Form, Input, Dialog, ConfigProvider } from '@alifd/next';
import './UserManagement.scss';

const FormItem = Form.Item;

const formItemLayout = {
  labelCol: { fixedSpan: 4 },
  wrapperCol: { span: 19 },
};

@ConfigProvider.config
class UpdateUser extends React.Component {
  static displayName = 'UpdateUser';

  field = new Field(this);

  static propTypes = {
    locale: PropTypes.object,
    visible: PropTypes.bool,
    username: PropTypes.string,
    onCancel: PropTypes.func,
    onOk: PropTypes.func,
  };

  check() {
    const { locale } = this.props;
    const errors = {
      kps: locale.kps,
    };
    const vals = Object.keys(errors).map(key => this.field.getValue(key));
    return [this.props.username, ...vals];
  }

  render() {
    const { locale } = this.props;
    const { getError } = this.field;
    const { username, onOk, onCancel } = this.props;
    return (
      <>
        <Dialog
          title={locale.updateUser}
          visible={username}
          onOk={() => {
            const vals = this.check();
            if (vals) {
              onOk(vals).then(() => onCancel());
            }
          }}
          onClose={onCancel}
          onCancel={onCancel}
          afterClose={() => this.field.reset()}
        >
          <Form style={{ width: 400 }} {...formItemLayout} field={this.field}>
            <FormItem label={locale.username} required>
              <p>{username}</p>
            </FormItem>
            <FormItem label={locale.kps} help={getError('kps')}>
              <Input name="kps" trim placeholder={locale.kpsPlaceholder} />
            </FormItem>
          </Form>
        </Dialog>
      </>
    );
  }
}

export default UpdateUser;
