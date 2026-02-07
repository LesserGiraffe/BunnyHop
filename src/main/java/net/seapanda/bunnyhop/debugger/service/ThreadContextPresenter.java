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

import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.NEXT_STEP;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.RUNTIME_ERROR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.model.DebugUtil;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.Debugger.ThreadContextAddedEvent;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem;
import net.seapanda.bunnyhop.debugger.model.callstack.StackFrameSelection;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadSelection;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
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
  /** 実行中もしくは次に実行するノードであることを表す視覚効果が適用された {@link BhNodeView} を格納するセット. */
  private final Set<BhNodeView> nextStepView = new HashSet<>();
  /** ランタイムエラーを起こしたことを表す視覚効果が適用された {@link BhNodeView} を格納するセット. */
  private final Set<BhNodeView> runtimeErrorView = new HashSet<>();


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
    cbRegistry.getOnCleared().add(event -> reset());
    cbRegistry.getOnCurrentStackFrameChanged().add(
        event -> refreshVisualEffects(event.currentThread(), event.newVal()));
    cbRegistry.getOnThreadContextAdded().add(this::onThreadContextAdded);
  }


  /** スレッドコンテキストが追加されたときに呼び出されるイベントハンドラ. */
  private void onThreadContextAdded(ThreadContextAddedEvent event) {
    ThreadContext context = event.context();
    threadIdToContext.put(context.threadId, context);
    context.getException().ifPresent(this::outputErrMsg);
    refreshVisualEffects(debugger.getCurrentThread(), debugger.getCurrentStackFrame());
  }

  private void reset() {
    removeAllVisualEffects();
    clearEffectViewCache();
    threadIdToContext.clear();
  }

  /** 現在ノードに適用されているノードの状態を表す視覚効果をすべて消す. */
  private void removeAllVisualEffects() {
    nextStepView.forEach(view -> effectManager.setEffectEnabled(view, false, NEXT_STEP));
    runtimeErrorView.forEach(view -> effectManager.setEffectEnabled(view, false, RUNTIME_ERROR));
  }

  /** ノードの状態を表す視覚効果がついた {@link BhNodeView} を格納するセットをクリアする. */
  private void clearEffectViewCache() {
    nextStepView.clear();
    runtimeErrorView.clear();
  }

  /**
   * 選択されたスレッドとスタックフレームに応じて、ノードに適用する視覚効果を更新する.
   * 既存の視覚効果を削除した後, 選択状態に基づいて新しい視覚効果を適用する.
   *
   * @param threadSelection スレッドの選択状態
   * @param stackFrameSelection スタックフレームの選択状態
   */
  private void refreshVisualEffects(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    if (threadSelection.equals(ThreadSelection.NONE)) {
      return;
    }
    removeAllVisualEffects();
    clearEffectViewCache();
    if (threadSelection.equals(ThreadSelection.ALL)) {
      applyVisualEffectsToAllThreadTopFrames();
    } else if (threadIdToContext.containsKey(threadSelection.getThreadId())) {
      applyVisualEffectToStackFrame(threadSelection, stackFrameSelection);
    }
  }

  /**
   * {@code item} に対応するノードビューがある場合, それに {@code effectType} で指定した視覚効果を適用する.
   *
   * @return 視覚効果を適用した {@link BhNodeView}
   */
  private Optional<BhNodeView> setVisualEffect(CallStackItem item, VisualEffectType effectType) {
    Optional<BhNodeView> nodeView = item.getNode().flatMap(BhNode::getView);
    nodeView.ifPresent(view -> effectManager.setEffectEnabled(view, true, effectType));
    return nodeView;
  }

  /** すべてのスレッドのコールスタックの最上位ノードに, 実行状態に応じた視覚効果を適用する. */
  private void applyVisualEffectsToAllThreadTopFrames() {
    for (ThreadContext context : threadIdToContext.values()) {
      if (context.state == BhThreadState.SUSPENDED) {
        context.getNextStep()
            .flatMap(item -> setVisualEffect(item, NEXT_STEP))
            .ifPresent(nextStepView::add);
      } else if (context.state == BhThreadState.ERROR) {
        context.getErrorStep()
            .flatMap(item -> setVisualEffect(item, RUNTIME_ERROR))
            .ifPresent(runtimeErrorView::add);
      }
    }
  }

  /**
   * {@code threadSelection} と {@code stackFrameSelection} で指定されたスタックフレームに対応するノードに視覚効果を適用する.
   *
   * @param threadSelection スレッドの選択状態
   * @param stackFrameSelection スタックフレームの選択状態
   */
  private void applyVisualEffectToStackFrame(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    if (stackFrameSelection.equals(StackFrameSelection.NONE)) {
      return;
    }
    findCallStackItem(threadSelection, stackFrameSelection).ifPresent(
        item -> {
          if (item.isError) {
            setVisualEffect(item, RUNTIME_ERROR).ifPresent(runtimeErrorView::add);
          } else {
            setVisualEffect(item, NEXT_STEP).ifPresent(nextStepView::add);
          }
        });
  }

  /**
   * {@code threadSelection} と {@code stackFrameSelection} で指定した {@link CallStackItem} を
   * {@link #threadIdToContext} から探す.
   */
  private Optional<CallStackItem> findCallStackItem(
      ThreadSelection threadSelection, StackFrameSelection stackFrameSelection) {
    ThreadContext context = threadIdToContext.get(threadSelection.getThreadId());
    if (context == null) {
      return Optional.empty();
    }
    if (isCallStackItemTop(context, stackFrameSelection)) {
      return switch (context.state) {
        case SUSPENDED -> context.getNextStep();
        case ERROR -> context.getErrorStep();
        default -> Optional.empty();
      };
    }
    return context.getCallStackItem(stackFrameSelection.getIndex() + 1);
  }

  /** {@code stackFrameSelection} で選択したスタックフレームが {@code context} のコールスタックのトップであるか調べる. */
  private static boolean isCallStackItemTop(
      ThreadContext context, StackFrameSelection stackFrameSelection) {
    if (context.callStack.isEmpty()) {
      return false;
    }
    return context.callStack.getLast().idx == stackFrameSelection.getIndex();
  }

  /** {@code exception} が持つエラーメッセージを出力する. */
  private void outputErrMsg(BhProgramException exception) {
    String errMsg = DebugUtil.getErrMsg(exception);
    String runtimeErrOccurred = TextDefs.Debugger.runtimeErrOccurred.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccurred, errMsg));
  }
}
