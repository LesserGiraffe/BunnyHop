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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.StackFrameSelection;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.control.SearchBox.Query;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.debugger.CallStackCell;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.LookManager.EffectTarget;
import org.apache.commons.lang3.StringUtils;

/**
 * コールスタックを表示するビューのコントローラ.
 *
 * @author K.Koike
 */
public class CallStackController {

  @FXML private VBox callStackViewBase;
  @FXML private CheckBox csShowAllCheckBox;
  @FXML private ListView<CallStackItem> callStackListView;
  @FXML private Button csSearchButton;
  @FXML private CheckBox csJumpCheckBox;

  private final ThreadContext threadContext;
  private final SearchBox searchBox;
  private final Debugger debugger;
  private final WorkspaceSet wss;
  private final ReentrantLock debugLock;
  private final Map<BhNode, Set<CallStackCell>> nodeToCallStackCell = new HashMap<>();
  private final Consumer<Debugger.CurrentThreadChangedEvent> onCurrentThreadChanged =
      event -> onCurrentDebugThreadChanged();
  private final Consumer<WorkspaceSet.NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());
  private final Consumer<Query> onSearchRequested = this::selectItem;

  /**
   * コンストラクタ.
   *
   * @param threadContext このコントローラが管理するコールスタックに関連するスレッドの情報を格納したオブジェクト
   * @param searchBox 検索クエリを受け取る UI コンポーネントのインタフェース
   * @param debugLock デバッガを使用する一連の処理に適用するロック
   */
  public CallStackController(
      ThreadContext threadContext,
      SearchBox searchBox,
      Debugger debugger,
      WorkspaceSet wss,
      ReentrantLock debugLock) {
    this.threadContext = threadContext;
    this.searchBox = searchBox;
    this.debugger = debugger;
    this.wss = wss;
    this.debugLock = debugLock;
  }

  /**
   * このコントローラの UI 要素を初期化する.
   *
   * <p>GUI コンポーネントのインジェクション後に FXMLLoader から呼ばれることを期待する.
   */
  public void initialize() {
    callStackListView.setCellFactory(stack -> new CallStackCell(nodeToCallStackCell));
    callStackListView.setItems(createCallStackItem());
    callStackListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onCallStackCellSelected(oldVal, newVal));
    csShowAllCheckBox.selectedProperty().addListener(
        (observable, oldVal, newVal) -> callStackListView.setItems(createCallStackItem()));
    csSearchButton.setOnAction(action -> {
      searchBox.setOnSearchRequested(onSearchRequested);
      searchBox.enable();
    });
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().add(onCurrentThreadChanged);
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
    Consumer<CallStackItem.SelectionEvent> selectItem = this::onCallStackItemSelected;
    threadContext.callStack.forEach(
        item -> item.getCallbackRegistry().getOnSelectionStateChanged().add(selectItem));
  }

  /** このコントローラが管理するビューのルート要素を返す. */
  public Node getView() {
    return callStackViewBase;
  }

  /** このコントローラが管理するコールスタックに関連するスレッドの情報を返す. */
  public ThreadContext getThreadContext() {
    return threadContext;
  }

  /** このコントローラを破棄するときに呼ぶこと. */
  public void discard() {
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().remove(onCurrentThreadChanged);
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().remove(onNodeSelStateChanged);
    searchBox.unsetOnSearchRequested(onSearchRequested);
  }

  /** {@link #callStackListView} に設定するアイテムを作成する. */
  private ObservableList<CallStackItem> createCallStackItem() {
    var callStack = new ArrayList<>(threadContext.callStack);
    if (!csShowAllCheckBox.isSelected() && callStack.size() > BhSettings.Debug.maxCallStackItems) {
      var items = new ArrayList<CallStackItem>();
      int len = BhSettings.Debug.maxCallStackItems / 2;
      for (int i = 0; i < len; ++i) {
        items.add(callStack.get(callStack.size() - 1 - i));
      }
      items.add(new CallStackItem(-1, -1, TextDefs.Debugger.CallStack.ellipsis.get()));

      len = BhSettings.Debug.maxCallStackItems - len;
      for (int i = len - 1; i >= 0; --i) {
        items.add(callStack.get(i));
      }
      return FXCollections.observableArrayList(items);
    }
    return FXCollections.observableArrayList(callStack.reversed());
  }

  /** コールスタックの UI 要素が選択されたときのイベントハンドラ. */
  private void onCallStackCellSelected(CallStackItem deselected, CallStackItem selected) {
    debugLock.lock();
    try {
      var tmpUserOpe = new UserOperation();
      if (deselected != null) {
        deselected.deselect(tmpUserOpe);
      }
      if (selected != null) {
        selected.select(tmpUserOpe);
      }
    } finally {
      debugLock.unlock();
    }
  }

  /** {@link CallStackItem} が選択されたときのイベントハンドラ. */
  private void onCallStackItemSelected(CallStackItem.SelectionEvent event) {
    if (event.isSelected()) {
      callStackListView.getSelectionModel().select(event.item());
      if (isThisThreadSameAsDebugThread()) {
        if (csJumpCheckBox.isSelected()) {
          getNextItemOf(event.item())
              .flatMap(CallStackItem::getNode)
              .ifPresent(CallStackController::jump);
        }
        debugger.selectCurrentStackFrame(StackFrameSelection.of(event.item().getIdx()));
      }
    } else {
      callStackListView.getSelectionModel().clearSelection();
      if (isThisThreadSameAsDebugThread()) {
        debugger.selectCurrentStackFrame(StackFrameSelection.NONE);
      }
    }
  }

  /** {@code node} を選択して, これを画面中央に表示する. */
  private static void jump(BhNode node) {
    BhNodeView nodeView = node.getView().orElse(null);
    if (node.isDeleted() || nodeView == null) {
      return;
    }
    ViewUtil.jump(nodeView, true, EffectTarget.SELF);
  }

  /**
   * {@code item} の次の {@link CallStackItem} を {@link #threadContext} のコールスタックから探す.
   */
  private Optional<CallStackItem> getNextItemOf(CallStackItem item) {
    if (threadContext.callStack.isEmpty()) {
      return Optional.empty();
    }
    if (threadContext.callStack.getLast().getIdx() == item.getIdx()) {
      return threadContext.getNextStep();
    }
    return threadContext.getCallStackItem(item.getIdx() + 1);
  }

  /** デバッガの現在のスレッドが変わったときの処理. */
  private void onCurrentDebugThreadChanged() {
    if (!isThisThreadSameAsDebugThread()) {
      return;
    }
    CallStackItem selected = callStackListView.getSelectionModel().getSelectedItem();
    StackFrameSelection stackFrameSel = (selected == null)
        ? StackFrameSelection.NONE
        : StackFrameSelection.of(selected.getIdx());
    debugger.selectCurrentStackFrame(stackFrameSel);
  }

  /** コールスタックから {@code query} に一致する要素を探して選択する. */
  private void selectItem(Query query) {
    CallStackItem item = null;
    if (StringUtils.isEmpty(query.word())) {
      return;
    }
    if (query.isRegex()) {
      try {
        int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(query.word(), regexFlag);
        item = searchCallStackFor(pattern, query.findNext());
      } catch (PatternSyntaxException e) { /* do nothing. */ }
    } else {
      item = searchCallStackFor(query.word(), query.findNext(), query.isCaseSensitive());
    }
    if (item != null) {
      callStackListView.getSelectionModel().select(item);
      callStackListView.scrollTo(item);  
    }
  }

  /** コールスタックから {@code word} に一致する {@link CallStackItem} を探す. */
  private CallStackItem searchCallStackFor(String word, boolean findNext, boolean caseSensitive) {
    if (!caseSensitive) {
      word = word.toLowerCase();
    }
    int size = callStackListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = callStackListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<CallStackItem> items =
        findNext ? callStackListView.getItems() : callStackListView.getItems().reversed();
    for (int i = 0; i < items.size(); ++i) {
      CallStackItem item = items.get((i + startIdx) % items.size());
      String itemName = caseSensitive ? item.getName() : item.getName().toLowerCase();
      if (itemName.contains(word)) {
        return item;
      }
    }
    return null;
  }

  /** コールスタックから {@code pattern} に一致する {@link CallStackItem} を探す. */
  private CallStackItem searchCallStackFor(Pattern pattern, boolean findNext) {
    int size = callStackListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = callStackListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<CallStackItem> items =
        findNext ? callStackListView.getItems() : callStackListView.getItems().reversed();
    for (int i = 0; i < items.size(); ++i) {
      CallStackItem item = items.get((i + startIdx) % items.size());
      if (pattern.matcher(item.getName()).find()) {
        return item;
      }
    }
    return null;
  }

  /** デバッガの現在のスレッド ID が, このコントローラが保持するスレッドコンテキストのスレッド ID と同じか調べる. */
  private boolean isThisThreadSameAsDebugThread() {
    ThreadSelection thisThread = ThreadSelection.of(threadContext.threadId);
    ThreadSelection debugThread = debugger.getCurrentThread();
    return !debugThread.equals(ThreadSelection.NONE)
          && !debugThread.equals(ThreadSelection.ALL)
          && debugThread.equals(thisThread);
  }

  /** {@code node} に対応する {@link CallStackCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    synchronized (nodeToCallStackCell) {
      if (nodeToCallStackCell.containsKey(node)) {
        nodeToCallStackCell.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
      }
    }
  }
}
