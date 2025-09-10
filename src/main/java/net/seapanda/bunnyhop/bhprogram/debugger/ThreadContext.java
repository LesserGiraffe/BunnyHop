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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;

/** BhProgram のスレッドに関連する情報を格納するクラス. */
public class ThreadContext {

  /** スレッド ID. */
  public final long threadId;
  /** スレッドの状態. */
  public final BhThreadState state;
  /** コールスタック. */
  public final SequencedCollection<CallStackItem> callStack;
  /** 次に実行するステップの情報を格納した {@link CallStackItem} オブジェクト. */
  private final CallStackItem nextStep;
  /** スレッドで発生した例外. */
  private final BhProgramException exception;
  /** スタックフレームのインデックスとそのインデックスを持つ {@link CallStackItem} のマップ. */
  private final Map<Integer, CallStackItem> frameIdxToCallStackItem = new HashMap<>();
  
  /** コンストラクタ. */
  public ThreadContext(
      long threadId,
      BhThreadState state,
      SequencedCollection<CallStackItem> callStack,
      CallStackItem nextStep,
      BhProgramException exception) {
    this.threadId = threadId;
    this.state = state;
    this.callStack = Collections.unmodifiableSequencedCollection(new ArrayList<>(callStack));
    this.nextStep = nextStep;
    this.exception = exception;
    callStack.forEach(item -> frameIdxToCallStackItem.put(item.getIdx(), item));
  }

  /** コンストラクタ. */
  public ThreadContext(long threadId) {
    this(threadId, BhThreadState.FINISHED, new ArrayList<>(), null, null);
  }

  /**
   * {@link #callStack} からスタックフレームのインデックスが {@code frameIdx} である {@link CallStackItem} を返す.
   */
  public Optional<CallStackItem> getCallStackItem(int frameIdx) {
    return Optional.ofNullable(frameIdxToCallStackItem.get(frameIdx));
  }

  /** スレッドで発生した例外を返す. */
  public Optional<BhProgramException> getException() {
    return Optional.ofNullable(exception);
  }

  /** 次に実行するステップの情報を格納した {@link CallStackItem} オブジェクトを返す. */
  public Optional<CallStackItem> getNextStep() {
    return Optional.ofNullable(nextStep);
  }
}
