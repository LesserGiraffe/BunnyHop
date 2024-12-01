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
 * {@link BhCmd} を処理するクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface CmdProcessor {

  /**
   * コマンドを処理する.
   *
   * @param cmd 受信したコマンド
   * @param data 受信したデータ
   * @return 受信したコマンドに対する返信データ
   */
  CmdData process(BhCmd cmd, CmdData data);
}
