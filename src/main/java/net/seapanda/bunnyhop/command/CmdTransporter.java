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

package net.seapanda.bunnyhop.command;

/**
 * コマンドを送信するクラス.
 *
 * @author K.Koike
 */
public class CmdTransporter {

  /** コンストラクタ. */
  public CmdTransporter() {}

  /**
   * dispatchers に順番にコマンドを渡す.
   * 2 つめ以降の {@link CmdDispatcher} オブジェクトには,
   * 1 つ前の {@link CmdDispatcher} オブジェクトの処理結果である {@link CmdData} が渡される.
   *
   * @param cmd 送信コマンド
   * @param data 1 つ目の dispatchers に渡されるコマンド
   * @param dispatchers コマンド送信先
   * @return 最後のコマンド送信先から返されるデータ
   */
  public CmdData sendCmd(BhCmd cmd, CmdData data, CmdDispatcher... dispatchers) {
    for (CmdDispatcher dispatcher : dispatchers) {
      data = dispatcher.dispatch(cmd, data);
    }
    return data;
  }

  /**
   * dispatchers に順番にコマンドを渡す.
   * 2 つめ以降の {@link CmdDispatcher} オブジェクトには, 
   * 1つ前の {@link CmdDispatcher} オブジェクトの処理結果である {@link CmdData} が渡される.
   *
   * @param cmd 送信コマンド
   * @param dispatchers コマンド送信先
   * @return 最後のコマンド送信先から返されるデータ
   */
  public CmdData sendCmd(BhCmd cmd, CmdDispatcher... dispatchers) {
    CmdData data = null;
    for (CmdDispatcher dispatcher : dispatchers) {
      data = dispatcher.dispatch(cmd, data);
    }
    return data;
  }
}
