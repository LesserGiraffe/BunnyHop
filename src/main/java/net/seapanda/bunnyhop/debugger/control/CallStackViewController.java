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

package net.seapanda.bunnyhop.debugger.control;

import static javafx.css.PseudoClass.getPseudoClass;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.DEFAULT_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInCallStack;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.JUMP_TARGET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem.SelectionEvent;
import net.seapanda.bunnyhop.debugger.model.callstack.StackFrameSelection;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadSelection;
import net.seapanda.bunnyhop.debugger.view.CallStackCell;
import net.seapanda.bunnyhop.debugger.view.CallStackCell.ItemChangeEvent;
import net.seapanda.bunnyhop.debugger.view.VariableListCell;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.search.ItemSearcher;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * コールスタックを表示するビューのコントローラ.
 *
 * @author K.Koike
 */
public class CallStackViewController {

  @FXML private VBox callStackViewBase;
  @FXML private CheckBox csShowAllCheckBox;
  @FXML private ListView<CallStackItem> callStackListView;
  @FXML private Button csSearchButton;
  @FXML private CheckBox csJumpCheckBox;

  private final ThreadContext threadContext;
  private final SearchBox searchBox;
  private final Debugger debugger;
  private final WorkspaceSet wss;
  private final BooleanProperty sharedJumpFlag;
  private final VisualEffectManager effectManager;
  private final CellRegistry cellRegistry;
  private boolean isDiscarded = false;
  private BhNodeView lastJumpTarget;
  private final Consumer<Debugger.CurrentThreadChangedEvent> onCurrentThreadChanged =
      event -> onCurrentDebugThreadChanged();
  private final Consumer<WorkspaceSet.NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());
  private SearchResult searchResult;

  /**
   * コンストラクタ.
   *
   * @param threadContext このコントローラが管理するコールスタックに関連するスレッドの情報を格納したオブジェクト
   * @param searchBox 検索クエリを受け取る UI コンポーネントのインタフェース
   * @param sharedJumpFlag スタックフレームが選択されたときに対応するノードへジャンプするかどうかのフラグ.
   *                       同じ {@link BooleanProperty} オブジェクトが指定された
   *                       {@link CallStackViewController} は全て同じフラグ値を共有する.
   */
  public CallStackViewController(
      ThreadContext threadContext,
      SearchBox searchBox,
      Debugger debugger,
      WorkspaceSet wss,
      BooleanProperty sharedJumpFlag,
      VisualEffectManager visualEffectManager) {
    this.threadContext = threadContext;
    this.searchBox = searchBox;
    this.debugger = debugger;
    this.wss = wss;
    this.sharedJumpFlag = sharedJumpFlag;
    effectManager = visualEffectManager;
    cellRegistry = new CellRegistry();
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    callStackListView.getItems().setAll(createCallStackItems());
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    callStackViewBase.parentProperty().addListener(
        (obs, oldVal, newVal) -> onViewParentChanged(newVal));
    callStackListView.setCellFactory(stack -> cellRegistry.createCell());
    callStackListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onCallStackCellSelected(oldVal, newVal));
    callStackListView.getItems().addListener(
        (ListChangeListener<? super CallStackItem>) event -> clearSearchResult());
    csShowAllCheckBox.selectedProperty().addListener(
        (observable, oldVal, newVal) -> updateCallStackItems());
    csSearchButton.setOnAction(action -> onSearchButtonClicked());
    csJumpCheckBox.selectedProperty().bindBidirectional(sharedJumpFlag);
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().add(onCurrentThreadChanged);
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
    Consumer<SelectionEvent> selectItem = this::onCallStackItemSelected;
    threadContext.callStack.forEach(
        item -> item.getCallbackRegistry().getOnSelectionStateChanged().add(selectItem));
    threadContext.getNextStep().ifPresent(
        item -> item.getCallbackRegistry().getOnSelectionStateChanged().add(selectItem));
    threadContext.getErrorStep().ifPresent(
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
    if (isDiscarded) {
      return;
    }
    isDiscarded = true;
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().remove(onCurrentThreadChanged);
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().remove(onNodeSelStateChanged);
    if (searchBox.getUser() == this) {
      searchBox.close();
    }
    if (lastJumpTarget != null) {
      effectManager.setEffectEnabled(lastJumpTarget, false, JUMP_TARGET);
    }
    callStackListView.getItems().clear();
    cellRegistry.clear();
    clearSearchResult();
    csJumpCheckBox.selectedProperty().unbindBidirectional(sharedJumpFlag);
  }

  /** {@link #callStackListView} に設定するアイテムを作成する. */
  private ObservableList<CallStackItem> createCallStackItems() {
    var callStack = new ArrayList<>(threadContext.callStack);
    if (!csShowAllCheckBox.isSelected() && callStack.size() > BhSettings.Debug.maxCallStackItems) {
      return FXCollections.observableArrayList(createTruncatedCallStack(callStack));
    }
    return FXCollections.observableArrayList(callStack.reversed());
  }

  /** {@code callStack} の一部を省略したコールスタックを作成する. */
  private static List<CallStackItem> createTruncatedCallStack(ArrayList<CallStackItem> callStack) {
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
    return items;
  }

  /** コールスタックの UI 要素が選択されたときのイベントハンドラ. */
  private void onCallStackCellSelected(CallStackItem deselected, CallStackItem selected) {
    if (isDiscarded) {
      return;
    }
    if (deselected != null) {
      deselected.deselect();
    }
    if (selected != null) {
      selected.select();
    }
  }

  /** {@link CallStackItem} が選択されたときのイベントハンドラ. */
  private void onCallStackItemSelected(SelectionEvent event) {
    if (isDiscarded) {
      return;
    }
    CallStackItem item = event.item();
    if (event.isSelected()) {
      callStackListView.getSelectionModel().select(item);
    }
    if (!isThisThreadSameAsDebugThread()) {
      return;
    }
    if (event.isSelected()) {
      if (csJumpCheckBox.isSelected()) {
        getJumpTarget(item).ifPresent(this::jumpTo);
      }
      int frameIdx = (item.isNext || item.isError) ? Math.max(item.idx - 1, 0) : item.idx;
      debugger.selectCurrentStackFrame(StackFrameSelection.of(frameIdx));
    } else {
      getJumpTarget(item)
          .ifPresent(view -> effectManager.setEffectEnabled(view, false, JUMP_TARGET));
      debugger.selectCurrentStackFrame(StackFrameSelection.NONE);
    }
  }

  /** {@code item} が選択されたときのジャンプ先のノードを探す. */
  private Optional<BhNodeView> getJumpTarget(CallStackItem item) {
    boolean itemIsErrorOrNextStep =
        threadContext.getNextStep().map(next -> next == item).orElse(false)
        || threadContext.getErrorStep().map(next -> next == item).orElse(false);

    return (itemIsErrorOrNextStep ? Optional.of(item) : getNextItemOf(item))
        .flatMap(CallStackItem::getNode)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView);
  }

  /**
   * {@code item} の次の {@link CallStackItem} を {@link #threadContext} のコールスタックから探す.
   */
  private Optional<CallStackItem> getNextItemOf(CallStackItem item) {
    if (threadContext.callStack.isEmpty()) {
      return Optional.empty();
    }
    if (threadContext.callStack.getLast().idx == item.idx) {
      return threadContext.getNextStep().isPresent()
          ? threadContext.getNextStep() : threadContext.getErrorStep();
    }
    return threadContext.getCallStackItem(item.idx + 1);
  }

  /** {@code view} にジャンプして視覚効果を適用する. */
  private void jumpTo(BhNodeView view) {
    ViewUtil.jump(view);
    effectManager.disableEffects(JUMP_TARGET);
    effectManager.setEffectEnabled(view, true, JUMP_TARGET);
    lastJumpTarget = view;
  }

  /** デバッガの現在のスレッドが変わったときの処理. */
  private void onCurrentDebugThreadChanged() {
    if (isDiscarded || !isThisThreadSameAsDebugThread()) {
      Optional.ofNullable(lastJumpTarget).ifPresent(
          view -> effectManager.setEffectEnabled(view, false, JUMP_TARGET));
      return;
    }
    CallStackItem selected = callStackListView.getSelectionModel().getSelectedItem();
    StackFrameSelection stackFrameSel = Optional.ofNullable(selected)
        .map(item -> (item.isNext || item.isError) ? Math.max(item.idx - 1, 0) : item.idx)
        .map(StackFrameSelection::of)
        .orElse(StackFrameSelection.NONE);
    debugger.selectCurrentStackFrame(stackFrameSel);
  }

  /** コールスタックから {@code query} に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (isDiscarded || query.isEmpty()) {
      return new SearchQueryResult(0, 0);
    }
    ImmutableCircularList<CallStackItem> matchedItems;
    CallStackItem found;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      matchedItems = searchResult.items();
      found = query.isForward() ? matchedItems.getNext() : matchedItems.getPrevious();
    } else {
      matchedItems = searchAndHighlight(query);
      found = matchedItems.getCurrent();
    }
    if (found != null) {
      callStackListView.getSelectionModel().select(found);
      callStackListView.scrollTo(found);
    }
    boolean truncated = matchedItems.size() == maxResultsInCallStack;
    return new SearchQueryResult(matchedItems.getPointer(), matchedItems.size(), truncated);
  }

  /**
   * {@code query} で変数一覧全体を検索し, 一致した要素を強調表示した上で, それらを巡回可能なリストとして返す.
   *
   * @param query 検索条件
   * @return {@code query} に一致した {@link CallStackItem} を格納する巡回リスト
   */
  private ImmutableCircularList<CallStackItem> searchAndHighlight(SearchQuery query) {
    ImmutableCircularList<CallStackItem> matchedItems = ItemSearcher.search(
        query,
        callStackListView.getItems(),
        CallStackCell::getText,
        maxResultsInCallStack);
    searchResult = new SearchResult(matchedItems, query);
    highlightSearchResult(searchResult);
    return matchedItems;
  }

  private void highlightSearchResult(SearchResult result) {
    Pattern pattern = result.query().getPattern();
    for (CallStackItem item : result.itemSet) {
      cellRegistry
          .getCells(item)
          .forEach(cell -> cell.enableHighlighting(pattern, DEFAULT_TEXT_HIGHLIGHT));
    }
  }

  /** デバッガの現在のスレッド ID が, このコントローラが保持するスレッドコンテキストのスレッド ID と同じか調べる. */
  private boolean isThisThreadSameAsDebugThread() {
    ThreadSelection thisThread = ThreadSelection.of(threadContext.threadId);
    ThreadSelection debugThread = debugger.getCurrentThread();
    return !debugThread.equals(ThreadSelection.NONE)
          && !debugThread.equals(ThreadSelection.ALL)
          && debugThread.equals(thisThread);
  }

  /** {@code nodes} に対応する {@link CallStackCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (isDiscarded) {
      return;
    }
    cellRegistry.getCells(node).forEach(cell -> cell.decorateText(node.isSelected()));
  }

  /**
   * {@link CallStackCell} に新しく割り当てられたアイテムの {@link BhNode} の選択状態に応じて,
   * セルの装飾を更新する.
   */
  private static void updateCellDecoration(ItemChangeEvent event) {
    boolean shouldDecorate =
        !event.empty()
        && Optional.ofNullable(event.newVal())
            .flatMap(CallStackItem::getNode)
            .map(BhNode::isSelected)
            .orElse(false);
    event.cell().decorateText(shouldDecorate);
  }

  /** {@link CallStackCell} に新しく割り当てられたアイテムに応じて, セルの強調表示を更新する. */
  private void updateSearchResultHighlight(ItemChangeEvent event) {
    boolean shouldHighlight =
        !event.empty()
        && event.newVal() != null
        && searchResult != null
        && searchResult.itemSet().contains(event.newVal());
    if (shouldHighlight) {
      event.cell().enableHighlighting(searchResult.query().getPattern(), DEFAULT_TEXT_HIGHLIGHT);
    } else {
      event.cell().disableHighlighting();
    }
  }

  /** {@link CallStackCell} に割り当てられるアイテムが変わったときの処理. */
  private void onCellItemChanged(ItemChangeEvent event) {
    cellRegistry.updateItemToCellsMap(event);
    cellRegistry.updateNodeToCellsMap(event);
    updateCellDecoration(event);
    updateSearchResultHighlight(event);
  }

  /** 検索ボタンが押されたときの処理. */
  private void onSearchButtonClicked() {
    if (isDiscarded) {
      return;
    }
    if (searchBox.getUser() == this) {
      searchBox.close();
      return;
    }
    csSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), true);
    searchBox.open(new SearchBoxDelegateImpl());
  }

  /** コールスタックビューの親要素が変わったときのイベントハンドラ. */
  private void onViewParentChanged(Parent newParent) {
    if (newParent == null && searchBox.getUser() == this) {
      searchBox.close();
      // 検索ボタンに適用した CSS が処理されない問題を回避するために必要.
      csSearchButton.applyCss();
    }
  }

  /** {@link #callStackListView} の表示項目を更新する. */
  private void updateCallStackItems() {
    if (!isDiscarded) {
      // 意図したコールバック関数が呼ばれないので ListView::setItems を使わないこと.
      callStackListView.getItems().setAll(createCallStackItems());
    }
  }

  /** 現在の検索結果を破棄し, それに伴う強調表示を全て解除する. */
  private void clearSearchResult() {
    searchResult = null;
    cellRegistry.getCells().forEach(CallStackCell::disableHighlighting);
  }

  /** {@link SearchBox} を使ったコールスタック一覧の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {

    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return selectItem(query);
    }

    @Override
    public void onClosed() {
      clearSearchResult();
      csSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), false);
    }

    @Override
    public void onCleared() {}

    @Override
    public Object getUser() {
      return CallStackViewController.this;
    }
  }

  /**
   * {@link CallStackViewController} が生成した全ての {@link CallStackCell} を管理し,
   * 各セルに現在割り当てられている {@link CallStackItem} および {@link BhNode} との対応関係を追跡するクラス.
   */
  private class CellRegistry {

    private final Map<CallStackItem, Set<CallStackCell>> itemToCells = new HashMap<>();
    private final Map<BhNode, Set<CallStackCell>> nodeToCells = new HashMap<>();
    private final Set<CallStackCell> cells = new HashSet<>();

    /** このオブジェクトが持つデータをクリアする. */
    void clear() {
      itemToCells.clear();
      nodeToCells.clear();
      cells.clear();
    }

    /** {@link CallStackItem} と {@link CallStackCell} の対応関係を更新する. */
    void updateItemToCellsMap(ItemChangeEvent event) {
      Optional.ofNullable(event.oldVal())
          .filter(oldVal -> event.empty() || oldVal != event.newVal())
          .filter(itemToCells::containsKey)
          .ifPresent(oldVal -> itemToCells.get(oldVal).remove(event.cell()));

      Optional.ofNullable(event.newVal())
          .filter(newVal -> !event.empty())
          .filter(newVal -> event.oldVal() != newVal)
          .ifPresent(newVal -> itemToCells
              .computeIfAbsent(newVal, key -> Collections.newSetFromMap(new WeakHashMap<>()))
              .add(event.cell()));
    }

    /** {@link BhNode} と {@link CallStackCell} の対応関係を更新する. */
    void updateNodeToCellsMap(ItemChangeEvent event) {
      Optional.ofNullable(event.oldVal())
          .filter(oldVal -> event.empty() || oldVal != event.newVal())
          .flatMap(CallStackItem::getNode)
          .filter(nodeToCells::containsKey)
          .ifPresent(node -> nodeToCells.get(node).remove(event.cell()));

      Optional.ofNullable(event.newVal())
          .filter(newVal -> !event.empty())
          .filter(newVal -> event.oldVal() != newVal)
          .flatMap(CallStackItem::getNode)
          .ifPresent(node -> nodeToCells
              .computeIfAbsent(node, key -> Collections.newSetFromMap(new WeakHashMap<>()))
              .add(event.cell()));
    }

    /** 引数で指定した {@link CallStackItem} に対応する {@link VariableListCell} のセットを取得する. */
    Set<CallStackCell> getCells(CallStackItem item) {
      return itemToCells.getOrDefault(item, new HashSet<>());
    }

    /** 引数で指定した {@link BhNode} に対応する {@link CallStackCell} のセットを取得する. */
    Set<CallStackCell> getCells(BhNode node) {
      return nodeToCells.getOrDefault(node, new HashSet<>());
    }

    /** このオブジェクトが作成した全ての {@link CallStackCell} を取得する. */
    Set<CallStackCell> getCells() {
      return cells;
    }

    /**
     * {@link VariableListCell} を生成し, このオブジェクトの管理下に加える.
     *
     * @return 生成した {@link VariableListCell}
     */
    CallStackCell createCell() {
      var cell = new CallStackCell();
      cell.setOnItemChanged(CallStackViewController.this::onCellItemChanged);
      cells.add(cell);
      return cell;
    }
  }

  /** 検索結果を格納するレコード. */
  record SearchResult(
      ImmutableCircularList<CallStackItem> items,
      Set<CallStackItem> itemSet,
      SearchQuery query) {

    SearchResult(ImmutableCircularList<CallStackItem> items, SearchQuery query) {
      this(
          items,
          new HashSet<>(items.getItems()),
          query);
    }
  }
}
