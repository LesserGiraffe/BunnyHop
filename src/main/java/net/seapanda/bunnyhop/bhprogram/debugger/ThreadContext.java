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

import java.util.ArrayList;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;

/**
 * BhProgram のスレッドに関連する情報を格納するクラス.
 *
 * @param threadId スレッド ID
 * @param state スレッドの状態
 * @param callStack コールスタック
 * @param exception スレッドで発生した例外
 */
public record ThreadContext(
    long threadId,
    BhThreadState state,
    SequencedCollection<CallStackItem> callStack,
    BhProgramException exception) {
  
  /** コンストラクタ. */
  public ThreadContext(
      long threadId,
      BhThreadState state,
      SequencedCollection<CallStackItem> callStack,
      BhProgramException exception) {
    this.threadId = threadId;
    this.state = state;
    this.callStack = new ArrayList<>(callStack);
    this.exception = exception;
  }  
}