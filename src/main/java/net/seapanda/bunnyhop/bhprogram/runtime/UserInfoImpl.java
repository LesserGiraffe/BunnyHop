/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.bhprogram.runtime;

import com.jcraft.jsch.UserInfo;
import net.seapanda.bunnyhop.service.message.MessageService;

/**
 * SSH 接続時に必要な情報を返す機能を持つクラス.
 *
 * @author K.Koike
 */
class UserInfoImpl implements UserInfo {

  private final String uname;
  private final String host;
  private final String password;
  private final MessageService msgService;

  /**
   * コンストラクタ.
   *
   * @param host ホスト名.
   * @param uname ユーザー名.
   * @param password パスワード.
   * @param msgService メッセージ出力用オブジェクト
   */
  UserInfoImpl(String host, String uname, String password, MessageService msgService) {
    this.host = host == null ? "" : host;
    this.uname = uname == null ? "" : uname;
    this.password = password == null ? "" : password;
    this.msgService = msgService;
  }

  /** コピーコンストラクタ. */
  UserInfoImpl(UserInfoImpl original, MessageService msgService) {
    this.host = original.host;
    this.uname = original.uname;
    this.password = original.password;
    this.msgService = msgService;
  }

  @Override
  public String getPassphrase() {
    return null;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean promptPassphrase(String arg0) {
    return false;
  }

  @Override
  public boolean promptPassword(String message) {
    return true;
  }

  @Override
  public boolean promptYesNo(String message) {
    return true;
  }

  @Override
  public void showMessage(String message) {
    msgService.info(message + "\n");
  }

  public String getUname() {
    return uname;
  }

  public String getHost() {
    return host;
  }
}
