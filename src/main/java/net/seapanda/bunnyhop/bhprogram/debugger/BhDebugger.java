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
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.AddBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalListValsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalVarsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalListValsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalVarsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetThreadContextsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.RemoveBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.ResumeThreadCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.SetBreakpointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepIntoCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOutCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.StepOverCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.SuspendThreadCmd;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.runtime.LocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.runtime.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガクラス.
 *
 * @author K.Koike
 */
public class BhDebugger implements Debugger {
  
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  /** 「現在のスレッド」として選択されたスレッド. */
  private volatile ThreadSelection currentThread = ThreadSelection.NONE;
  /** 「現在のスタックフレーム」として選択されたスタックフレーム. */
  private volatile StackFrameSelection currentStackFrame = StackFrameSelection.NONE;
  private final LocalBhRuntimeController localBhRuntimeCtrl;
  private final RemoteBhRuntimeController remoteBhRuntimeCtrl;
  private final BreakpointRegistry breakpointRegistry = new BreakpointRegistry();

  /** コンストラクタ. */
  public BhDebugger(
      LocalBhRuntimeController localBhRuntimeCtrl,
      RemoteBhRuntimeController remoteBhRuntimeCtrl,
      WorkspaceSet wss) {
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
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public synchronized void add(ThreadContext context) {
    cbRegistry.onThreadContextAddedInvoker.invoke(new ThreadContextAddedEvent(this, context));
  }

  @Override
  public synchronized void add(VariableInfo variableInfo) {
    var event = new VariableInfoAddedEvent(this, variableInfo);
    cbRegistry.onVariableInfoAddedInvoker.invoke(event);
  }

  @Override
  public synchronized boolean suspend() {
    BhRuntimeStatus status = null;
    if (currentThread.equals(ThreadSelection.ALL)) {
      status = getBhRuntimeCtrl().send(new SuspendThreadCmd(SuspendThreadCmd.ALL_THREADS));
    } else if (!currentThread.equals(ThreadSelection.NONE)) {
      status = getBhRuntimeCtrl().send(new SuspendThreadCmd(currentThread.getThreadId()));
    }
    return status == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean resume() {
    BhRuntimeStatus status = null;
    if (currentThread.equals(ThreadSelection.ALL)) {
      status = getBhRuntimeCtrl().send(new ResumeThreadCmd(ResumeThreadCmd.ALL_THREADS));
    } else if (!currentThread.equals(ThreadSelection.NONE)) {
      status = getBhRuntimeCtrl().send(new ResumeThreadCmd(currentThread.getThreadId()));
    }
    return status == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean stepOver() {
    BhRuntimeStatus status = null;
    if (isParticularThreadSelected()) {
      status = getBhRuntimeCtrl().send(new StepOverCmd(currentThread.getThreadId()));
    }
    return status == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean stepInto() {
    BhRuntimeStatus status = null;
    if (isParticularThreadSelected()) {
      status = getBhRuntimeCtrl().send(new StepIntoCmd(currentThread.getThreadId()));
    }
    return status == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean stepOut() {
    BhRuntimeStatus status = null;
    if (isParticularThreadSelected()) {
      status = getBhRuntimeCtrl().send(new StepOutCmd(currentThread.getThreadId()));
    }
    return status == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean requestThreadContexts() {
    return getBhRuntimeCtrl().send(new GetThreadContextsCmd()) == BhRuntimeStatus.SUCCESS;
  }

  @Override
  public synchronized boolean requestLocalVars() {
    if (isParticularThreadSelected() && !currentStackFrame.equals(StackFrameSelection.NONE)) {
      var cmd = new GetLocalVarsCmd(currentThread.getThreadId(), currentStackFrame.getIndex());
      BhRuntimeStatus status = getBhRuntimeCtrl().send(cmd);
      return status == BhRuntimeStatus.SUCCESS;
    }
    return false;
  }

  @Override
  public synchronized boolean requestGlobalVars() {
    if (isParticularThreadSelected() && !currentStackFrame.equals(StackFrameSelection.NONE)) {
      BhRuntimeStatus status = getBhRuntimeCtrl().send(new GetGlobalVarsCmd());
      return status == BhRuntimeStatus.SUCCESS;
    }
    return false;
  }

  @Override
  public synchronized boolean requestLocalListVals(BhNode node, long startIdx, long length) {
    if (isParticularThreadSelected() && !currentStackFrame.equals(StackFrameSelection.NONE)) {
      var cmd = new GetLocalListValsCmd(
          currentThread.getThreadId(),
          currentStackFrame.getIndex(),
          BhSymbolId.of(node.getInstanceId().toString()),
          startIdx,
          length);
      BhRuntimeStatus status = getBhRuntimeCtrl().send(cmd);
      return status == BhRuntimeStatus.SUCCESS;
    }
    return false;
  }

  @Override
  public synchronized boolean requestGlobalListVals(BhNode node, long startIdx, long length) {
    if (isParticularThreadSelected() && !currentStackFrame.equals(StackFrameSelection.NONE)) {
      var symbolId = BhSymbolId.of(node.getInstanceId().toString());
      var cmd = new GetGlobalListValsCmd(symbolId, startIdx, length);
      BhRuntimeStatus status = getBhRuntimeCtrl().send(cmd);
      return status == BhRuntimeStatus.SUCCESS;
    }
    return false;
  }

  @Override
  public synchronized void selectCurrentThread(ThreadSelection selection) {
    if (!currentThread.equals(selection)) {
      final var event = new CurrentThreadChangedEvent(this, currentThread, selection);
      currentThread = selection;
      deselectCurrentStackFrame();
      cbRegistry.onCurrentThreadChanged.invoke(event);
    }
  }

  @Override
  public synchronized ThreadSelection getCurrentThread() {
    return currentThread;
  }

  @Override
  public synchronized void selectCurrentStackFrame(StackFrameSelection selection) {
    if (currentThread.equals(ThreadSelection.NONE) || currentThread.equals(ThreadSelection.ALL)) {
      return;
    }
    if (!currentStackFrame.equals(selection)) {
      final var event =
          new CurrentStackFrameChangedEvent(this, currentThread, currentStackFrame, selection);
      currentStackFrame = selection;
      cbRegistry.onCurrentStackFrameChanged.invoke(event);
    }
  }

  /** 「現在のスタックフレーム」を未選択にする. */
  private void deselectCurrentStackFrame() {
    var selection = StackFrameSelection.NONE;
    if (!currentStackFrame.equals(selection)) {
      final var event =
          new CurrentStackFrameChangedEvent(this, currentThread, currentStackFrame, selection);
      currentStackFrame = selection;
      cbRegistry.onCurrentStackFrameChanged.invoke(event);
    }
  }

  @Override
  public synchronized StackFrameSelection getCurrentStackFrame() {
    return currentStackFrame;
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

  /** 特定のスレッドが選択されているかどうかを調べる. */
  private boolean isParticularThreadSelected() {
    return !currentThread.equals(ThreadSelection.ALL)
        && !currentThread.equals(ThreadSelection.NONE);
  }


  /** イベントハンドラの管理を行うクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** {@link ThreadContext} を取得したときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ThreadContextAddedEvent> onThreadContextAddedInvoker =
        new ConsumerInvoker<>();

    /** {@link VariableInfoAddedEvent} を取得したときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<VariableInfoAddedEvent> onVariableInfoAddedInvoker =
        new ConsumerInvoker<>();

    /** デバッグ情報をクリアしたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ClearEvent> onClearedInvoker = new ConsumerInvoker<>();

    /** 「現在のスレッド」が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CurrentThreadChangedEvent> onCurrentThreadChanged =
        new ConsumerInvoker<>();

    /** 「現在のスタックフレーム」が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CurrentStackFrameChangedEvent> onCurrentStackFrameChanged =
        new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<ThreadContextAddedEvent>.Registry getOnThreadContextAdded() {
      return onThreadContextAddedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<VariableInfoAddedEvent>.Registry getOnVariableInfoAdded() {
      return onVariableInfoAddedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ClearEvent>.Registry getOnCleared() {
      return onClearedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<CurrentThreadChangedEvent>.Registry getOnCurrentThreadChanged() {
      return onCurrentThreadChanged.getRegistry();
    }

    @Override
    public ConsumerInvoker<CurrentStackFrameChangedEvent>.Registry getOnCurrentStackFrameChanged() {
      return onCurrentStackFrameChanged.getRegistry();
    }
  }
}
