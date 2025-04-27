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

package net.seapanda.bunnyhop.bhprogram.debugger;

import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;

/**
 * デバッグ情報を受け取る機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface DebugInfoReceiver {

  /**
   * デバッガにスレッドの例外情報を追加する.
   *
   * @param exception 追加する例外情報
   */
  void receive(BhProgramException exception);
}
