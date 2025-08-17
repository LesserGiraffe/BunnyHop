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
import java.util.Set;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.bhprogram.debugger.DebugUtil;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
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
  private final Set<BhNodeView> nextStepView = new HashSet<>();

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
      nextStepView.forEach(view -> view.getLookManager().setNextStepMarkVisibility(false));
    });
    cbRegistry.getOnCurrentThreadChanged().add(event -> showNextStepMarks(event.newVal()));
    cbRegistry.getOnThreadContextAdded().add(event -> {
      ThreadContext context = event.context();
      threadIdToContext.put(context.threadId(), context);
      showNextStepMarks(ThreadSelection.of(context.threadId()));
      outputErrMsg(context.exception());
    });
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    if (exception == null) {
      return;
    }
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccurred = TextDefs.Debugger.runtimeErrOccurred.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccurred, errMsg));
  }

  /** {@code selection} で指定されたスレッドで次に実行されるノードに, そのことを示すマークを付ける. */
  private void showNextStepMarks(ThreadSelection selection) {
    if (selection.equals(ThreadSelection.NONE)) {
      return;
    }
    Set<ThreadContext> contexts = new HashSet<>();
    if (selection.equals(ThreadSelection.ALL)) {
      contexts.addAll(threadIdToContext.values());
    } else if (threadIdToContext.containsKey(selection.getThreadId())) {
      contexts.add(threadIdToContext.get(selection.getThreadId()));
    }
    nextStepView.forEach(view -> view.getLookManager().setNextStepMarkVisibility(false));

    for (ThreadContext context : contexts) {
      if (context.callStack().isEmpty()) {
        continue;
      }
      CallStackItem top = context.callStack().getLast();
      if (!top.isNotCalled()) {
        continue;
      }
      if (top.getNode().flatMap(BhNode::getView).orElse(null) instanceof BhNodeView view) {
        view.getLookManager().setNextStepMarkVisibility(true);
        nextStepView.add(view);
      }
    }
  }
}
