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

package net.seapanda.bunnyhop.bhprogram;

import java.nio.file.Path;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;

/**
 * BhRuntime (BhProgram の実行環境) の操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhRuntimeController {
  
  /**
   * BhRuntime を起動して BhProgram を実行する.
   *
   * @param filePath 実行するファイルのパス
   * @param ipAddr BhProgram を実行するマシンの IP アドレス.
   * @param uname BhProgram を実行するマシンにログインする際のユーザ名
   * @param password BhProgram を実行するマシンにログインする際のパスワード
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean start(Path filePath, String ipAddr, String uname, String password)
      throws UnsupportedOperationException;

  /**
   * ローカルマシン上で BhRuntime を起動して BhProgram を実行する.
   *
   * @param filePath 実行するファイルのパス
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean start(Path filePath) throws UnsupportedOperationException;

  /**
   * 現在動作中の BhRuntime を終了する.
   *
   * @return 成功した場合 true
   */
  boolean terminate();

  /**
   * BhRuntime との通信を有効化する.
   *
   * @return 成功した場合 true
   */
  boolean connect();

  /**
   * BhProgram との通信を無効化する.
   *
   * @return 成功した場合 true
   */
  boolean disconnect();

  /**
   * 引数で指定した {@link BhProgramMessage} を BhProgram の実行環境に送る.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus send(BhProgramMessage msg);
}
