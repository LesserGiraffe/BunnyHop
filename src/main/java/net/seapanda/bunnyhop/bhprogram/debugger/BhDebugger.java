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

import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガクラス.
 *
 * @author K.Koike
 */
public class BhDebugger implements Debugger {
  
  private final MessageService msgService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();

  /** コンストラクタ. */
  public BhDebugger(MessageService msgService) {
    this.msgService = msgService;
  }

  @Override
  public void clear() {
    cbRegistry.onClearedInvoker.invoke(new ClearEvent(this));
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public void output(ThreadContext context) {
    if (context.exception() != null) {
      outputErrMsg(context.exception());
    }
    cbRegistry.onThreadContextReceivedInvoker.invoke(new ThreadContextReceivedEvent(this, context));
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccured = TextDefs.Debugger.runtimErrOccured.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccured, errMsg));
  }

  /** イベントハンドラの管理を行うクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** {@link ThreadContext} を取得したときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ThreadContextReceivedEvent> onThreadContextReceivedInvoker =
        new ConsumerInvoker<>();

    /** デバッグ情報をクリアしたときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ClearEvent> onClearedInvoker = new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<ThreadContextReceivedEvent>.Registry getOnThreadContextReceived() {
      return onThreadContextReceivedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ClearEvent>.Registry getOnCleared() {
      return onClearedInvoker.getRegistry();
    }
  }
}
