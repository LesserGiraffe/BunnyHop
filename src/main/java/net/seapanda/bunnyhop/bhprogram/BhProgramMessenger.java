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

import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;

/**
 * 現在通信が有効になっている BhProgram に {@link BhProgramMessage} を送る機能を規定したクラス.
 *
 * @author K.Koike
 */
@FunctionalInterface
public interface BhProgramMessenger {

  /**
   * 現在通信が有効になっている BhProgram に {@code message} を送る.
   *
   * @param message 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus send(BhProgramMessage message);
}
