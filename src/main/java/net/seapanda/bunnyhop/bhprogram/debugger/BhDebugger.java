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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.ThreadSelection;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.AddBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetThreadContextsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.RemoveBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.ResumeThreadCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.SetBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepIntoCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOutCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOverCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.SuspendThreadCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.runtime.LocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.runtime.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * BhProgram のデバッガクラス.
 *
 * @author K.Koike
 */
public class BhDebugger implements Debugger {
  
  private final MessageService msgService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  /** このデバッガに対し指定されたスレッドの選択状態. */
  private volatile ThreadSelection threadSelection = ThreadSelection.NONE;
  /** 次に実行するノードのビューのセット. */
  private final Set<BhNodeView> nextStepView = new HashSet<>();
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  private final LocalBhRuntimeController localBhRuntimeCtrl;
  private final RemoteBhRuntimeController remoteBhRuntimeCtrl;
  private final BreakpointRegistry breakpointRegistry = new BreakpointRegistry();

  /** コンストラクタ. */
  public BhDebugger(
      MessageService msgService,
      LocalBhRuntimeController localBhRuntimeCtrl,
      RemoteBhRuntimeController remoteBhRuntimeCtrl,
      WorkspaceSet wss) {
    this.msgService = msgService;
    this.localBhRuntimeCtrl = localBhRuntimeCtrl;
    this.remoteBhRuntimeCtrl = remoteBhRuntimeCtrl;
    setEventHandlers(wss);
  }

  private void setEventHandlers(WorkspaceSet wss) {
    wss.getCallbackRegistry().getOnNodeAdded()
        .add(event -> {
          if (event.node().isBreakpointSet()) {
            breakpointRegistry.addBreakpointNode(event.node(), event.userOpe());
          }
        });
    wss.getCallbackRegistry().getOnNodeRemoved()
        .add(event -> breakpointRegistry.removeBreakpointNode(event.node(), event.userOpe()));
    wss.getCallbackRegistry().getOnNodeBreakpointSetEvent()
        .add(event -> {
          if (event.node().isBreakpointSet()) {
            breakpointRegistry.addBreakpointNode(event.node(), event.userOpe());
          } else {
            breakpointRegistry.removeBreakpointNode(event.node(), event.userOpe());
          }
        });

    localBhRuntimeCtrl.getCallbackRegistry().getOnConnectionConditionChanged()
        .add(event -> {
          if (event.isConnected()) {
            setBreakpoints(breakpointRegistry.getBreakpointNodes().toArray(new BhNode[0]));
          }
          clear();
        });
    remoteBhRuntimeCtrl.getCallbackRegistry().getOnConnectionConditionChanged()
        .add(event -> {
          if (event.isConnected()) {
            setBreakpoints(breakpointRegistry.getBreakpointNodes().toArray(new BhNode[0]));
          }
          clear();
        });
    breakpointRegistry.getCallbackRegistry().getOnBreakpointAdded()
        .add(event -> addBreakpoints(event.breakpoint()));
    breakpointRegistry.getCallbackRegistry().getOnBreakpointRemoved()
        .add(event -> removeBreakpoints(event.breakpoint()));
  }

  @Override
  public void clear() {
    cbRegistry.onClearedInvoker.invoke(new ClearEvent(this));
    threadIdToContext.clear();
    nextStepView.forEach(view -> view.getLookManager().setNextStepMarkVisibility(false));
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public synchronized void output(ThreadContext context) {
    threadIdToContext.put(context.threadId(), context);
    showNextStepMarks();
    if (context.exception() != null) {
      outputErrMsg(context.exception());
    }
    var event = new ThreadContextReceivedEvent(this, context);
    cbRegistry.onThreadContextReceivedInvoker.invoke(event);
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccurred = TextDefs.Debugger.runtimeErrOccurred.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccurred, errMsg));
  }

  @Override
  public synchronized void suspend(long threadId) {
    getBhRuntimeCtrl().send(new SuspendThreadCmd(threadId));
  }

  @Override
  public synchronized void suspendAll() {
    getBhRuntimeCtrl().send(new SuspendThreadCmd(SuspendThreadCmd.ALL_THREADS));
  }

  @Override
  public synchronized void resume(long threadId) {
    getBhRuntimeCtrl().send(new ResumeThreadCmd(threadId));
  }

  @Override
  public synchronized void resumeAll() {
    getBhRuntimeCtrl().send(new ResumeThreadCmd(ResumeThreadCmd.ALL_THREADS));
  }

  @Override
  public synchronized void stepOver(long threadId) {
    getBhRuntimeCtrl().send(new StepOverCmd(threadId));
  }

  @Override
  public synchronized void stepInto(long threadId) {
    getBhRuntimeCtrl().send(new StepIntoCmd(threadId));
  }

  @Override
  public synchronized void stepOut(long threadId) {
    getBhRuntimeCtrl().send(new StepOutCmd(threadId));
  }

  @Override
  public synchronized void requireThreadContexts() {
    getBhRuntimeCtrl().send(new GetThreadContextsCmd());
  }

  @Override
  public synchronized void selectThread(ThreadSelection selection) {
    if (!threadSelection.equals(selection)) {
      var event = new ThreadSelectionEvent(this, threadSelection, selection);
      threadSelection = selection;
      showNextStepMarks();
      cbRegistry.onThreadSelectionChanged.invoke(event);
    }
  }

  @Override
  public synchronized ThreadSelection getSelectedThread() {
    return threadSelection;
  }

  @Override
  public BreakpointRegistry getBreakpointRegistry() {
    return breakpointRegistry;
  }

  /** 現在操作対象になっている BhRuntime のコントローラオブジェクトを返す. */
  private BhRuntimeController getBhRuntimeCtrl() {
    return switch (BhSettings.BhRuntime.currentBhRuntimeType) {
      case LOCAL ->  localBhRuntimeCtrl;
      case REMOTE ->  remoteBhRuntimeCtrl;
    };
  }

  /** {@link #threadSelection} で指定されたスレッドで次に実行されるノードに, そのことを示すマークを付ける. */
  private void showNextStepMarks() {
    if (threadSelection.equals(ThreadSelection.NONE)) {
      return;
    }
    Set<ThreadContext> contexts = new HashSet<>();
    if (threadSelection.equals(ThreadSelection.ALL)) {
      contexts.addAll(threadIdToContext.values());
    } else if (threadIdToContext.containsKey(threadSelection.getThreadId())) {
      contexts.add(threadIdToContext.get(threadSelection.getThreadId()));
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

  /**
   * BhRuntime にブレークポイントを設定する.
   *
   * <p>追加済みのブレークポイントは削除される.
   */
  private void setBreakpoints(BhNode... nodes) {
    Collection<BhSymbolId> breakpoints = Arrays.stream(nodes)
        .map(node -> BhSymbolId.of(node.getInstanceId().toString()))
        .collect(Collectors.toCollection(ArrayList::new));
    getBhRuntimeCtrl().send(new SetBreakpointsCmd(breakpoints));
  }

  /** BhRuntime にブレークポイントを追加する. */
  private void addBreakpoints(BhNode... nodes) {
    Collection<BhSymbolId> breakpoints = Arrays.stream(nodes)
        .map(node -> BhSymbolId.of(node.getInstanceId().toString()))
        .collect(Collectors.toCollection(ArrayList::new));
    getBhRuntimeCtrl().send(new AddBreakpointsCmd(breakpoints));
  }

  /** BhRuntime からブレークポイントを削除する. */
  private void removeBreakpoints(BhNode... nodes) {
    Collection<BhSymbolId> breakpoints = Arrays.stream(nodes)
        .map(node -> BhSymbolId.of(node.getInstanceId().toString()))
        .collect(Collectors.toCollection(ArrayList::new));
    getBhRuntimeCtrl().send(new RemoveBreakpointsCmd(breakpoints));
  }

  /** イベントハンドラの管理を行うクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** {@link ThreadContext} を取得したときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ThreadContextReceivedEvent> onThreadContextReceivedInvoker =
        new ConsumerInvoker<>();

    /** デバッグ情報をクリアしたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ClearEvent> onClearedInvoker = new ConsumerInvoker<>();

    /** スレッドの選択状態が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ThreadSelectionEvent> onThreadSelectionChanged =
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
