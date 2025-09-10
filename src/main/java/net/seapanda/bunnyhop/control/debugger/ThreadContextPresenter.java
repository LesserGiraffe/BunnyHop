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

package net.seapanda.bunnyhop.control.debugger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.bhprogram.debugger.DebugUtil;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.StackFrameSelection;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@link ThreadContext} の情報をアプリケーションユーザに提示する機能を持ったクラス.
 *
 * @author  K.Koike
 */
public class ThreadContextPresenter {

  private final Debugger debugger;
  private final MessageService msgService;
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  /** 次に実行するノードのビューのセット. */
  private final Set<BhNodeView> execStepView = new HashSet<>();

  /** コンストラクタ. */
  public ThreadContextPresenter(Debugger debugger, MessageService msgService) {
    this.debugger = debugger;
    this.msgService = msgService;
    setEventHandlers();
  }

  private void setEventHandlers() {
    Debugger.CallbackRegistry cbRegistry = debugger.getCallbackRegistry();
    cbRegistry.getOnCleared().add(event -> {
      threadIdToContext.clear();
      execStepView.forEach(view -> view.getLookManager().setExecStepMarkVisibility(false));
      execStepView.clear();
    });
    cbRegistry.getOnCurrentStackFrameChanged()
        .add(event -> showExecStepMarks(event.currentThread(), event.newVal()));
    cbRegistry.getOnThreadContextAdded().add(event -> {
      ThreadContext context = event.context();
      threadIdToContext.put(context.threadId, context);
      context.getException().ifPresent(this::outputErrMsg);
    });
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccurred = TextDefs.Debugger.runtimeErrOccurred.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccurred, errMsg));
  }

  /** {@code selection} で指定されたスレッドで次に実行されるノードに, そのことを示すマークを付ける. */
  private void showExecStepMarks(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    if (threadSelection.equals(ThreadSelection.NONE)) {
      return;
    }
    execStepView.forEach(view -> view.getLookManager().setExecStepMarkVisibility(false));
    if (threadSelection.equals(ThreadSelection.ALL)) {
      setExecStepMarksOnCallStackTopNodes();
    } else if (threadIdToContext.containsKey(threadSelection.getThreadId())) {
      setExecStepMarkOnCurrentNodeInStackFrame(threadSelection, stackFrameSelection);
    }
  }

  /** 全てのスレッドに対して, 次に実行するノードにそのことを示すマークをつける. */
  private void setExecStepMarksOnCallStackTopNodes() {
    for (ThreadContext context : threadIdToContext.values()) {
      context.getNextStep()
          .flatMap(CallStackItem::getNode)
          .flatMap(BhNode::getView)
          .ifPresent(
            view -> {
              view.getLookManager().setExecStepMarkVisibility(true);
              execStepView.add(view);
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
          if (isCallStackItemTop(context, stackFrameSelection)) {
            return context.getNextStep();
          }
          return context.getCallStackItem(stackFrameSelection.getIndex() + 1);
        })
        .flatMap(callStackItem -> callStackItem.getNode().flatMap(BhNode::getView))
        .ifPresent(view -> {
          view.getLookManager().setExecStepMarkVisibility(true);
          execStepView.add(view);
        });
  }

  /** {@code stackFrameSel} で選択したスタックフレームが {@code context} のコールスタックのトップであるか調べる. */
  private boolean isCallStackItemTop(
      ThreadContext context, StackFrameSelection stackFrameSelection) {
    if (context.callStack.isEmpty()) {
      return false;
    }
    return context.callStack.getLast().getIdx() == stackFrameSelection.getIndex();
  }
}
