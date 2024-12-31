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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;

/**
 * BhProgram の特定のスレッドの特定の時点における情報を格納するレコード.
 *
 * @param callStack コールスタック. 呼び出した順番が古いメソッドを前に格納すること.
 * @param msg {@code callStack} このコンテキストに付随するメッセージ (例外発生時のエラーメッセージなど)
 * @param threadId {@code callStack} のメソッドを実行したスレッドの ID
 * @param errorOccured スレッドが実行した処理でエラーが発生したことを示すフラグ.
 */
public record ThreadContext(
    long threadId,
    SequencedCollection<InstanceId> callStack,
    String msg,
    boolean errorOccured) {
  
  /** コンストラクタ. */
  public ThreadContext(
      long threadId,
      SequencedCollection<InstanceId> callStack,
      String msg,
      boolean errorOccured) {
    this.callStack = (callStack == null) ? new LinkedList<>() : new ArrayList<>(callStack);
    this.msg = (msg == null) ? "" : msg;
    this.threadId = threadId;
    this.errorOccured = errorOccured;
  }
}
