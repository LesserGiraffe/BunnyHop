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

package net.seapanda.bunnyhop.debugger.service;

import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.EXEC_STEP;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.model.DebugUtil;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem;
import net.seapanda.bunnyhop.debugger.model.callstack.StackFrameSelection;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadSelection;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.service.message.MessageService;

/**
 * {@link ThreadContext} の情報をアプリケーションユーザに提示する機能を持ったクラス.
 *
 * @author  K.Koike
 */
public class ThreadContextPresenter {

  private final Debugger debugger;
  private final MessageService msgService;
  private final VisualEffectManager effectManager;
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  /**
   * key : 止まっているスレッドの ID. <br>
   * value : key のスレッドが次に実行するノードのビュー.
   */
  private final Map<Long, BhNodeView> threadIdToExecStepView = new HashMap<>();

  /** コンストラクタ. */
  public ThreadContextPresenter(
      Debugger debugger,
      MessageService msgService,
      VisualEffectManager visualEffectManager) {
    this.debugger = debugger;
    this.msgService = msgService;
    this.effectManager = visualEffectManager;
    setEventHandlers();
  }

  private void setEventHandlers() {
    Debugger.CallbackRegistry cbRegistry = debugger.getCallbackRegistry();
    
    cbRegistry.getOnCleared().add(event -> clearThreadContexts());
    cbRegistry.getOnCurrentStackFrameChanged()
        .add(event -> showExecStepMarks(event.currentThread(), event.newVal()));
    cbRegistry.getOnThreadContextAdded().add(event -> {
      ThreadContext context = event.context();
      threadIdToContext.put(context.threadId, context);
      context.getException().ifPresent(this::outputErrMsg);
      updateExecStepMark(context);
    });
  }

  /** スレッドコンテキストと実行ステップマークをすべてクリアする. */
  private void clearThreadContexts() {
    threadIdToContext.clear();
    threadIdToExecStepView.values().forEach(
        view -> effectManager.setEffectEnabled(view, false, EXEC_STEP));
    threadIdToExecStepView.clear();
  }

  /** {@code context} のスレッドの状態とデバッガの現在のスレッドに基づいて次に実行するノードのマークを変更する. */
  private void updateExecStepMark(ThreadContext context) {
    if (context.state != BhThreadState.SUSPENDED) {
      BhNodeView view = threadIdToExecStepView.remove(context.threadId);
      if (view != null && !threadIdToExecStepView.containsValue(view)) {
        effectManager.setEffectEnabled(view, false, EXEC_STEP);
      }
      return;
    }

    var threadId = ThreadSelection.of(context.threadId);
    ThreadSelection currentThread = debugger.getCurrentThread();
    if (!currentThread.equals(threadId) && !currentThread.equals(ThreadSelection.ALL)) {
      return;
    }
    context.getNextStep()
        .flatMap(CallStackItem::getNode)
        .flatMap(BhNode::getView)
        .ifPresent(
            view -> {
              effectManager.setEffectEnabled(view, true, EXEC_STEP);
              threadIdToExecStepView.put(context.threadId, view);
            });
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccurred = TextDefs.Debugger.runtimeErrOccurred.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccurred, errMsg));
  }

  /** {@code threadSelection} で指定されたスレッドで次に実行されるノードに, そのことを示すマークを付ける. */
  private void showExecStepMarks(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    if (threadSelection.equals(ThreadSelection.NONE)) {
      return;
    }
    threadIdToExecStepView.values().forEach(
        view -> effectManager.setEffectEnabled(view, false, EXEC_STEP));
    if (threadSelection.equals(ThreadSelection.ALL)) {
      setExecStepMarksOnCallStackTopNodes();
    } else if (threadIdToContext.containsKey(threadSelection.getThreadId())) {
      setExecStepMarkOnCurrentNodeInStackFrame(threadSelection, stackFrameSelection);
    }
  }

  /** 全てのスレッドに対して, 次に実行するノードにそのことを示すマークをつける. */
  private void setExecStepMarksOnCallStackTopNodes() {
    for (ThreadContext context : threadIdToContext.values()) {
      if (context.state != BhThreadState.SUSPENDED) {
        continue;
      }
      context.getNextStep()
          .flatMap(CallStackItem::getNode)
          .flatMap(BhNode::getView)
          .ifPresent(
            view -> {
              effectManager.setEffectEnabled(view, true, EXEC_STEP);
              threadIdToExecStepView.put(context.threadId, view);
            });
    }
  }

  /**
   * {@code threadSelection} と {@code stackFrameSelection} で指定されたスタックフレームの現在実行中のノードに
   * 実行中であることを表すマークを付ける.
   */
  private void setExecStepMarkOnCurrentNodeInStackFrame(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    if (stackFrameSelection.equals(StackFrameSelection.NONE)) {
      return;
    }

    Optional.ofNullable(threadIdToContext.get(threadSelection.getThreadId()))
        .flatMap(context -> {
          if (context.state != BhThreadState.SUSPENDED) {
            return Optional.empty();
          }
          if (isCallStackItemTop(context, stackFrameSelection)) {
            return context.getNextStep();
          }
          return context.getCallStackItem(stackFrameSelection.getIndex() + 1);
        })
        .flatMap(callStackItem -> callStackItem.getNode().flatMap(BhNode::getView))
        .ifPresent(view -> {
          effectManager.setEffectEnabled(view, true, EXEC_STEP);
          threadIdToExecStepView.put(threadSelection.getThreadId(), view);
        });
  }

  /** {@code stackFrameSel} で選択したスタックフレームが {@code context} のコールスタックのトップであるか調べる. */
  private static boolean isCallStackItemTop(
      ThreadContext context, StackFrameSelection stackFrameSelection) {
    if (context.callStack.isEmpty()) {
      return false;
    }
    return context.callStack.getLast().idx == stackFrameSelection.getIndex();
  }
}
