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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedCollection;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger.CurrentStackFrameChangedEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger.CurrentThreadChangedEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.factory.DebugViewFactory;

/**
 * デバッグ情報を表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class DebugViewController {
  
  @FXML private ScrollPane callStackScrollPane;
  @FXML private ScrollPane localVarScrollPane;
  @FXML private ScrollPane globalVarScrollPane;
  
  /** スレッド ID とコールスタックビューのマップ. */
  private final Map<Long, Node> threadIdToCallStackView = new HashMap<>();
  /** スタックフレームと変数情報のマップ. */
  private final Map<StackFrameId, VarInfoModelView> stackFrameToVarInfo = new HashMap<>();
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  private DebugViewFactory factory;
  private Debugger debugger;
  
  /** 初期化する. */
  public synchronized void initialize(Debugger debugger, DebugViewFactory factory) {
    this.factory = factory;
    this.debugger = debugger;
    Debugger.CallbackRegistry registry = debugger.getCallbackRegistry();
    registry.getOnThreadContextAdded().add(event -> addThreadContext(event.context()));
    registry.getOnCleared().add(event -> clear());
    registry.getOnCurrentThreadChanged().add(this::showCallStackView);
    registry.getOnCurrentStackFrameChanged().add(event -> event.debugger().requestLocalVars());
  }

  /**
   * スレッドの情報を追加する.
   *
   * @param context 追加するスレッドの情報
   */
  private synchronized void addThreadContext(ThreadContext context) {
    long threadId = context.threadId();
    if (threadId < 1) {
      return;
    }
    if (context.state() == BhThreadState.FINISHED
        && !threadIdToContext.containsKey(context.threadId())) {
      return;
    }
    Node callStackView = createCallStackView(context.callStack());
    if (callStackView == null) {
      return;
    }
    threadIdToCallStackView.put(threadId, callStackView);
    threadIdToContext.put(threadId, context);
    boolean isSelectedThread =
        debugger.getCurrentThread().equals(ThreadSelection.of(context.threadId()));
    if (isSelectedThread) {
      ViewUtil.runSafe(() -> callStackScrollPane.setContent(callStackView));
    }
  }

  /** {@code items} からコールスタックを表示するビューを作成する. */
  private Node createCallStackView(SequencedCollection<CallStackItem> items) {
    try {
      return factory.createCallStackView(items);
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
    }
    return null;
  }

  /** コールスタックビューを表示する. */
  private void showCallStackView(CurrentThreadChangedEvent event) {
    Node callStackView = createCallStackView(new ArrayList<>());;
    if (!event.newVal().equals(ThreadSelection.ALL)
        && !event.newVal().equals(ThreadSelection.NONE)) {
      callStackView = threadIdToCallStackView.get(event.newVal().getThreadId());
    }
    callStackScrollPane.setContent(callStackView);
  }

  /** {@code varInfo} から変数検査ビューを作成する. */
  private Node createVarInspectionView(VariableInfo varInfo, boolean isLocal) {
    try {
      String viewName = isLocal
          ? TextDefs.Debugger.VarInspection.localVars.get()
          : TextDefs.Debugger.VarInspection.globalVars.get();
      return factory.createVariableInspectionView(varInfo, viewName);
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
    }
    return null;
  }

  /** 変数検査ビューを表示する. */
  private void showVaraInspectionView(CurrentStackFrameChangedEvent event) {

  }

  /** デバッグ情報をクリアする. */
  private synchronized void clear() {
    threadIdToCallStackView.clear();
    threadIdToContext.clear();
    stackFrameToVarInfo.clear();
  }

  /**
   * 変数情報とそれを表示するビューのセット
   *
   * @param model 変数情報
   * @param view {@code model} を表示するビュー
   */
  private record VarInfoModelView(VariableInfo model, Node view) {}

  /**
   * スタックフレームを一意に識別するための ID.
   * スレッド ID とコールスタック内のスタックフレームのインデックスで特定する.
   */
  private record StackFrameId(long threadId, long frameIdx) {};
}
