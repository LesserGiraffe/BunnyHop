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
 * {@link BhCmd} を {@link CmdProcessor} に渡す機能を定義したインタフェース.
 *
 * @author K.Koike
 */
public interface CmdDispatcher {

  /**
   * {@link CmdProcessor} を登録する.
   *
   * @param processor コマンドを処理するオブジェクト
   */
  public void setMsgProcessor(CmdProcessor processor);

  /**
   * 引数で指定したメコマンドを {@link CmdProcessor} に渡して処理結果を返す.
   *
   * @param cmd 処理するコマンド
   * @param data 処理するデータ
   * @return 処理結果
   */
  public CmdData dispatch(BhCmd cmd, CmdData data);
}
