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
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.control.debugger.ThreadSelectorController.ThreadSelectionEvent;
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
  @FXML private ThreadSelectorController threadSelectorController;
  @FXML private ThreadStateViewController threadStateViewController;
  
  /** スレッド ID とコールスタックビューのマップ. */
  private final Map<Long, Node> threadIdToCallStackView = new HashMap<>();
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  private DebugViewFactory factory;
  
  /** 初期化する. */
  public synchronized void initialize(Debugger debugger, DebugViewFactory factory) {
    this.factory = factory;
    Debugger.CallbackRegistry registry = debugger.getCallbackRegistry();
    registry.getOnThreadContextReceived().add(event -> addThreadContext(event.context()));
    registry.getOnCleared().add(event -> clear());
    threadSelectorController.setOnThreadSelected(event -> {
      showCallStackView(event);
      showThreadState(event);
    });
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
    Node callStackView = createCallStackView(context);
    if (callStackView == null) {
      return;
    }
    threadIdToCallStackView.put(threadId, callStackView);
    threadIdToContext.put(threadId, context);
    threadSelectorController.addToSelection(threadId);
    boolean isSelectedThread = threadSelectorController.getSelected()
        .map(selected -> selected == context.threadId())
        .orElse(false);
    if (isSelectedThread) {
      callStackScrollPane.setContent(callStackView);
      threadStateViewController.showThreadState(context);
    }
  }

  /** {@code context} からコールスタックを表示するビューを作成する. */
  private Node createCallStackView(ThreadContext context) {
    try {
      return factory.createCallStackView(context.callStack());
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
    }
    return null;
  }

  /** コールスタックビューを表示する. */
  private void showCallStackView(ThreadSelectionEvent event) {
    Node callStackView = null;
    if (!event.isAllSelected() && event.newThreadId() != null) {
      callStackView = threadIdToCallStackView.get(event.newThreadId());
    }
    callStackScrollPane.setContent(callStackView);
  }

  /** スレッドの状態を表示する. */
  private void showThreadState(ThreadSelectionEvent event) {
    if (event.isAllSelected() || event.newThreadId() == null) {
      threadStateViewController.hideThreadState();
      return;
    }
    ThreadContext context = threadIdToContext.get(event.newThreadId());
    threadStateViewController.showThreadState(context);
  }

  /** デバッグ情報をクリアする. */
  private synchronized void clear() {
    ViewUtil.runSafe(() -> {
      threadIdToCallStackView.clear();
      threadIdToContext.clear();
      threadSelectorController.reset();
      threadStateViewController.reset();
    });
  }
}
