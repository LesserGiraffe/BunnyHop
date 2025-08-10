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

import java.util.Objects;
import net.seapanda.bunnyhop.bhprogram.BhProgramMessenger;
import net.seapanda.bunnyhop.bhprogram.ThreadSelection;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.ResumeThreadCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepIntoCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOutCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOverCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.SuspendThreadCmd;
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
  private final BhProgramMessenger messenger;
  private ThreadSelection threadSelection = ThreadSelection.NONE;

  /** コンストラクタ. */
  public BhDebugger(BhProgramMessenger messenger, MessageService msgService) {
    this.messenger = messenger;
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

  @Override
  public void suspend(long threadId) {
    messenger.send(new SuspendThreadCmd(threadId));
  }

  @Override
  public void suspendAll() {
    messenger.send(new SuspendThreadCmd(SuspendThreadCmd.ALL_THREADS));
  }

  @Override
  public void resume(long threadId) {
    messenger.send(new ResumeThreadCmd(threadId));
  }

  @Override
  public void resumeAll() {
    messenger.send(new ResumeThreadCmd(ResumeThreadCmd.ALL_THREADS));
  }

  @Override
  public void stepOver(long threadId) {
    messenger.send(new StepOverCmd(threadId));
  }

  @Override
  public void stepInto(long threadId) {
    messenger.send(new StepIntoCmd(threadId));
  }

  @Override
  public void stepOut(long threadId) {
    messenger.send(new StepOutCmd(threadId));
  }

  @Override
  public void setThreadSelection(ThreadSelection selection) {
    Objects.nonNull(selection);
    if (!threadSelection.equals(selection)) {
      var event = new ThreadSelectionEvent(this, threadSelection, selection);
      threadSelection = selection;
      cbRegistry.onThreadSelectionChanged.invoke(event);
    }
  }

  @Override
  public ThreadSelection getThreadSelection() {
    return threadSelection;
  }

  /** イベントハンドラの管理を行うクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** {@link ThreadContext} を取得したときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ThreadContextReceivedEvent> onThreadContextReceivedInvoker =
        new ConsumerInvoker<>();

    /** デバッグ情報をクリアしたときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ClearEvent> onClearedInvoker = new ConsumerInvoker<>();

    /** スレッドの選択状態が変わったときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ThreadSelectionEvent> onThreadSelectionChanged =
        new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<ThreadContextReceivedEvent>.Registry getOnThreadContextReceived() {
      return onThreadContextReceivedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ClearEvent>.Registry getOnCleared() {
      return onClearedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ThreadSelectionEvent>.Registry getOnThreadSelectionChanged() {
      return onThreadSelectionChanged.getRegistry();
    }
  }
}
