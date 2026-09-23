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
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInBreakpointList;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.JUMP_TARGET;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.debugger.model.breakpoint.BreakpointCache;
import net.seapanda.bunnyhop.debugger.view.BreakpointListCell;
import net.seapanda.bunnyhop.debugger.view.BreakpointListCell.ItemChangeEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.search.ItemSearcher;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSelectorController;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * ブレークポイントを表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class BreakpointListController {

  @FXML private WorkspaceSelectorController bpWsSelectorController;
  @FXML private ListView<BhNode> bpListView;
  @FXML private Button bpSearchButton;
  @FXML private CheckBox bpJumpCheckBox;

  private final WorkspaceSet wss;
  private final BreakpointCache breakpointCache;
  private final VisualEffectManager effectManager;
  private final SearchBox searchBox;
  private final CellRegistry cellRegistry;
  private SearchResult searchResult;

  /** コンストラクタ. */
  public BreakpointListController(
      WorkspaceSet wss,
      BreakpointCache breakpointCache,
      SearchBox searchBox,
      VisualEffectManager visualEffectManager) {
    this.wss = wss;
    this.breakpointCache = breakpointCache;
    this.searchBox = searchBox;
    effectManager = visualEffectManager;
    cellRegistry = new CellRegistry();
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    BreakpointCache.CallbackRegistry cbRegistry = breakpointCache.getCallbackRegistry();
    cbRegistry.getOnNodeAdded().add(event -> addBreakpoint(event.added()));
    cbRegistry.getOnNodeRemoved().add(event -> removeBreakpoint(event.removed()));

    bpListView.setCellFactory(view -> cellRegistry.createCell());
    bpListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onBreakpointSelected(oldVal, newVal));
    bpListView.getItems().addListener(
        (ListChangeListener<? super BhNode>) event -> clearSearchResult());

    bpSearchButton.setOnAction(action -> onSearchButtonClicked());
    bpWsSelectorController.setOnWorkspaceSelected(
        event -> refreshBreakpointList(event.newWs(), event.isAllSelected()));

    WorkspaceSet.CallbackRegistry wssCbRegistry = wss.getCallbackRegistry();
    wssCbRegistry.getOnNodeSelectionStateChanged().add(event -> updateCellDecoration(event.node()));
    wssCbRegistry.getOnNodeTextChanged().add(event -> {
      clearSearchResult();
      updateCellValues();
    });
  }

  /** ブレークポイント一覧のブレークポイントが選択されたときのイベントハンドラ. */
  private void onBreakpointSelected(BhNode deselected, BhNode selected) {
    Optional.ofNullable(deselected)
        .flatMap(BhNode::getView)
        .ifPresent(view -> effectManager.setEffectEnabled(view, false, JUMP_TARGET));

    if (!bpJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(selected)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView)
        .ifPresent(this::jumpTo);
  }

  /** 検索ボタンが押されたときの処理. */
  private void onSearchButtonClicked() {
    if (searchBox.getUser() == this) {
      searchBox.close();
      return;
    }
    bpSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), true);
    searchBox.open(new SearchBoxDelegateImpl());
  }

  /** ブレークポイント一覧から {@code query} で指定された文字列に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (query.isEmpty()) {
      return new SearchQueryResult(0, 0);
    }
    ImmutableCircularList<BhNode> matchedItems;
    BhNode found;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      matchedItems = searchResult.nodes();
      found = query.isForward() ? matchedItems.getNext() : matchedItems.getPrevious();
    } else {
      matchedItems = searchAndHighlight(query);
      found = matchedItems.getCurrent();
    }
    if (found != null) {
      bpListView.getSelectionModel().select(found);
      bpListView.scrollTo(found);
    }
    boolean truncated = matchedItems.size() == maxResultsInBreakpointList;
    return new SearchQueryResult(matchedItems.getPointer(), matchedItems.size(), truncated);
  }

  /**
   * {@code query} で変数一覧全体を検索し, 一致した要素を強調表示した上で, それらを巡回可能なリストとして返す.
   *
   * @param query 検索条件
   * @return {@code query} に一致した {@link BhNode} を格納する巡回リスト
   */
  private ImmutableCircularList<BhNode> searchAndHighlight(SearchQuery query) {
    ImmutableCircularList<BhNode> matchedItems = ItemSearcher.search(
        query,
        bpListView.getItems(),
        BreakpointListCell::getText,
        maxResultsInBreakpointList);
    searchResult = new SearchResult(matchedItems, query);
    highlightSearchResult(searchResult);
    return matchedItems;
  }

  private void highlightSearchResult(SearchResult result) {
    Pattern pattern = result.query().getPattern();
    for (BhNode node : result.nodeSet()) {
      cellRegistry
          .getCells(node)
          .forEach(cell -> cell.enableHighlighting(pattern, DEFAULT_TEXT_HIGHLIGHT));
    }
  }

  /** {@code nodes} をブレークポイント一覧に加える. */
  private void addBreakpoint(BhNode node) {
    if (bpWsSelectorController.matchesSelection(node.getWorkspace())) {
      bpListView.getItems().add(node);
    }
  }

  /** {@code nodes} をブレークポイント一覧から削除する. */
  private void removeBreakpoint(BhNode node) {
    // ブレークポイントを削除したときに別のブレークポイントが自動的に選択されるのを防ぐ
    if (bpListView.getSelectionModel().getSelectedItem() == node) {
      bpListView.getSelectionModel().clearSelection();
    }
    bpListView.getItems().remove(node);
    cellRegistry.removeMapping(node);
  }

  /** {@code ws} 上にある, ブレークポイントを指定されたノードを表示する. */
  private void refreshBreakpointList(Workspace ws, boolean isAllSelected) {
    if (ws == null && !isAllSelected) {
      bpListView.getItems().clear();
      return;
    }
    List<BhNode> bpNodes = breakpointCache.getBreakpoints().stream()
        .filter(node -> node.getWorkspace() == ws || isAllSelected)
        .toList();
    bpListView.getItems().setAll(bpNodes);
    updateCellValues();
  }

  /** 現在表示されている {@link BreakpointListCell} の内容を更新する. */
  private void updateCellValues() {
    for (BhNode node : bpListView.getItems()) {
      cellRegistry.getCells(node).forEach(BreakpointListCell::updateValue);
    }
  }

  /** {@code nodes} に対応する {@link BreakpointListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    cellRegistry.getCells(node).forEach(cell -> cell.decorateText(node.isSelected()));
  }

  /**
   * {@link BreakpointListCell} に新しく割り当てられたアイテムの {@link BhNode} の選択状態に応じて,
   * セルの装飾を更新する.
   */
  private static void updateCellDecoration(ItemChangeEvent event) {
    boolean shouldDecorate =
        !event.empty()
        && Optional.ofNullable(event.newVal())
            .map(BhNode::isSelected)
            .orElse(false);
    event.cell().decorateText(shouldDecorate);
  }

  /** {@link BreakpointListCell} に新しく割り当てられたアイテムに応じて, セルの強調表示を更新する. */
  private void updateSearchResultHighlight(ItemChangeEvent event) {
    boolean shouldHighlight =
        !event.empty()
        && event.newVal() != null
        && searchResult != null
        && searchResult.nodeSet().contains(event.newVal());
    if (shouldHighlight) {
      event.cell().enableHighlighting(searchResult.query().getPattern(), DEFAULT_TEXT_HIGHLIGHT);
    } else {
      event.cell().disableHighlighting();
    }
  }

  /** {@link BreakpointListCell} に割り当てられるアイテムが変わったときの処理. */
  private void onCellItemChanged(ItemChangeEvent event) {
    cellRegistry.updateNodeToCellsMap(event);
    updateCellDecoration(event);
    updateSearchResultHighlight(event);
  }

  /** {@code view} にジャンプし, ジャンプ先となった際の視覚効果をつける. */
  private void jumpTo(BhNodeView view) {
    ViewUtil.jump(view);
    effectManager.disableEffects(VisualEffectType.JUMP_TARGET);
    effectManager.setEffectEnabled(view, true, VisualEffectType.JUMP_TARGET);
  }

  /** 現在の検索結果を破棄し, それに伴う強調表示を全て解除する. */
  private void clearSearchResult() {
    searchResult = null;
    cellRegistry.getCells().forEach(BreakpointListCell::disableHighlighting);
  }

  /** {@link SearchBox} を使ったブレークポイント一覧の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {

    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return selectItem(query);
    }

    @Override
    public void onClosed() {
      bpSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), false);
      clearSearchResult();
    }

    @Override
    public void onCleared() {}

    @Override
    public Object getUser() {
      return BreakpointListController.this;
    }
  }

  /**
   * {@link BreakpointListController} が生成した全ての {@link BreakpointListCell} を管理し,
   * 各セルに現在割り当てられている {@link BhNode} との対応関係を追跡するクラス.
   */
  private class CellRegistry {
    private final Map<BhNode, Set<BreakpointListCell>> nodeToCells = new HashMap<>();
    private final Set<BreakpointListCell> cells = new HashSet<>();

    /** {@link BhNode} と {@link BreakpointListCell} の対応関係を更新する. */
    void updateNodeToCellsMap(ItemChangeEvent event) {
      Optional.ofNullable(event.oldVal())
          .filter(oldVal -> event.empty() || oldVal != event.newVal())
          .filter(nodeToCells::containsKey)
          .ifPresent(oldVal -> nodeToCells.get(oldVal).remove(event.cell()));

      Optional.ofNullable(event.newVal())
          .filter(newVal -> !event.empty())
          .filter(newVal -> event.oldVal() != newVal)
          .ifPresent(newVal -> nodeToCells
              .computeIfAbsent(newVal, key -> Collections.newSetFromMap(new WeakHashMap<>()))
              .add(event.cell()));
    }

    /** 引数で指定した {@link BhNode} に対応する {@link BreakpointListCell} のセットを取得する. */
    Set<BreakpointListCell> getCells(BhNode node) {
      return nodeToCells.getOrDefault(node, new HashSet<>());
    }

    /** このオブジェクトが作成した全ての {@link BreakpointListCell} を取得する. */
    Set<BreakpointListCell> getCells() {
      return cells;
    }

    /**
     * {@link BreakpointListCell} を生成し, このオブジェクトの管理下に加える.
     *
     * @return 生成した {@link BreakpointListCell}
     */
    BreakpointListCell createCell() {
      var cell = new BreakpointListCell();
      cell.setOnItemChanged(BreakpointListController.this::onCellItemChanged);
      cells.add(cell);
      return cell;
    }

    /** 引数で指定した {@link BhNode} と {@link BreakpointListCell} の対応関係を取り除く. */
    void removeMapping(BhNode node) {
      nodeToCells.remove(node);
    }
  }

  /** 検索結果を格納するレコード. */
  record SearchResult(
      ImmutableCircularList<BhNode> nodes,
      Set<BhNode> nodeSet,
      SearchQuery query) {

    SearchResult(ImmutableCircularList<BhNode> items, SearchQuery query) {
      this(
          items,
          new HashSet<>(items.getItems()),
          query);
    }
  }
}
