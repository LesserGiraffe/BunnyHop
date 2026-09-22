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

package net.seapanda.bunnyhop.linter.control;

import static javafx.css.PseudoClass.getPseudoClass;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.DEFAULT_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInErrorNodeList;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInVariableInspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.debugger.view.VariableListCell;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.linter.model.ErrorNodeListItem;
import net.seapanda.bunnyhop.linter.view.ErrorNodeListCell;
import net.seapanda.bunnyhop.linter.view.ErrorNodeListCell.ItemChangeEvent;
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
 * エラーノードを表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ErrorNodeListController {

  @FXML private WorkspaceSelectorController enWsSelectorController;
  @FXML private TreeView<ErrorNodeListItem> enTreeView;
  @FXML private Button enSearchButton;
  @FXML private CheckBox enJumpCheckBox;

  private final WorkspaceSet wss;
  private final CompileErrorNodeCache compileErrorNodeCache;
  private final VisualEffectManager effectManager;
  private final SearchBox searchBox;
  private final ErrorNodeTreeItem rootErrorNodeItem;
  private final CellRegistry cellRegistry;
  private final TreeItemRegistry treeItemRegistry;
  private SearchResult searchResult;

  /** コンストラクタ. */
  public ErrorNodeListController(
      WorkspaceSet wss,
      CompileErrorNodeCache compileErrorNodeCache,
      SearchBox searchBox,
      VisualEffectManager visualEffectManager) {
    this.wss = wss;
    this.compileErrorNodeCache = compileErrorNodeCache;
    this.searchBox = searchBox;
    effectManager = visualEffectManager;
    rootErrorNodeItem = new ErrorNodeTreeItem();
    rootErrorNodeItem.setExpanded(false);
    cellRegistry = new CellRegistry();
    treeItemRegistry = new TreeItemRegistry();
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    enTreeView.setShowRoot(false);
    enTreeView.setRoot(rootErrorNodeItem);
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    enTreeView.setCellFactory(view -> cellRegistry.createCell());
    enTreeView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> onItemSelected(newVal));
    enTreeView.focusedProperty().addListener(
        (obs, oldVal, newVal) -> onFocusChanged(newVal));
    rootErrorNodeItem.getChildren().addListener(
        (ListChangeListener<? super TreeItem<ErrorNodeListItem>>) change -> clearSearchResult());
    enSearchButton.setOnAction(action -> onSearchButtonClicked());
    enWsSelectorController.setOnWorkspaceSelected(
        event -> showErrorNodes(event.newWs(), event.isAllSelected()));

    WorkspaceSet.CallbackRegistry wssCbRegistry = wss.getCallbackRegistry();
    wssCbRegistry.getOnNodeSelectionStateChanged().add(event -> updateCellDecoration(event.node()));
    wssCbRegistry.getOnNodeTextChanged().add(event -> clearSearchResult());
    CompileErrorNodeCache.CallbackRegistry cbRegistry = compileErrorNodeCache.getCallbackRegistry();
    cbRegistry.getOnCompileErrorStateUpdated().add(event -> addErrorNode(event.updated()));
    cbRegistry.getOnNodeAdded().add(event -> addErrorNode(event.added()));
    cbRegistry.getOnNodeRemoved().add(event -> removeErrorNode(event.removed()));
  }

  /** エラーノード情報を一覧に追加する. */
  private void addErrorNode(BhNode node) {
    var messages = node.getCompileErrorMessages().stream()
        .map(msg -> new ErrorNodeTreeItem(new ErrorNodeListItem(node, msg)))
        .toList();
    ErrorNodeTreeItem treeItem = treeItemRegistry.getTreeItem(node);
    treeItem.getChildren().clear();
    treeItem.getChildren().addAll(messages);
    treeItem.setExpanded(true);
    if (!enWsSelectorController.matchesSelection(node.getWorkspace())) {
      return;
    }
    if (treeItem.getParent() == null) {
      rootErrorNodeItem.getChildren().add(treeItem);
    }
  }

  /** エラーノード情報を一覧から削除する. */
  private void removeErrorNode(BhNode node) {
    // エラーノードを削除したときに別のエラーノードが自動的に選択されるのを防ぐ
    Optional.ofNullable(enTreeView.getSelectionModel().getSelectedItem())
        .map(TreeItem::getValue)
        .map(ErrorNodeListItem::node)
        .filter(selected -> selected == node)
        .ifPresent(selected -> enTreeView.getSelectionModel().clearSelection());

    cellRegistry.removeMapping(node);
    ErrorNodeTreeItem treeItem = treeItemRegistry.removeMapping(node);
    if (treeItem != null) {
      rootErrorNodeItem.getChildren().remove(treeItem);
    }
  }

  /** {@code ws} 上にあるエラーノードを表示する. */
  private void showErrorNodes(Workspace ws, boolean isAllSelected) {
    rootErrorNodeItem.getChildren().clear();
    if (ws == null && !isAllSelected) {
      return;
    }
    List<ErrorNodeTreeItem> treeItems = treeItemRegistry.getTreeItems().stream()
        .filter(treeItem -> treeItem.getValue().node().getWorkspace() == ws || isAllSelected)
        .toList();
    rootErrorNodeItem.getChildren().addAll(treeItems);
    updateCellValues();
  }

  /** エラーの項目が選択された時の処理. */
  private void onItemSelected(TreeItem<ErrorNodeListItem> item) {
    if (!enJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(item)
        .map(TreeItem::getValue)
        .map(ErrorNodeListItem::node)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView)
        .ifPresent(this::jumpTo);
  }

  /** フォーカスが変更されたときの処理. */
  private void onFocusChanged(Boolean isFocused) {
    if (!isFocused) {
      enTreeView.getSelectionModel().clearSelection();
    } else {
      updateCellValues();
    }
  }

  /** 検索ボタンが押されたときの処理. */
  private void onSearchButtonClicked() {
    if (searchBox.getUser() == this) {
      searchBox.close();
      return;
    }
    enSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), true);
    searchBox.open(new SearchBoxDelegateImpl());
    updateCellValues();
  }

  /** 変数一覧から {@code query} に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (query.isEmpty()) {
      return new SearchQueryResult(0, 0);
    }
    ImmutableCircularList<ErrorNodeTreeItem> matchedItems;
    ErrorNodeTreeItem found;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      matchedItems = searchResult.treeItems();
      found = query.isForward() ? matchedItems.getNext() : matchedItems.getPrevious();
    } else {
      matchedItems = searchAndHighlight(query);
      found = matchedItems.getCurrent();
    }
    if (found != null) {
      expandAncestorsOf(found);
      enTreeView.getSelectionModel().select(found);
      int index = enTreeView.getRow(found);
      enTreeView.scrollTo(index);
    }
    boolean truncated = matchedItems.size() == maxResultsInVariableInspection;
    return new SearchQueryResult(matchedItems.getPointer(), matchedItems.size(), truncated);
  }

  /**
   * {@code query} で変数一覧全体を検索し, 一致した要素を強調表示した上で, それらを巡回可能なリストとして返す.
   *
   * @param query 検索条件
   * @return {@code query} に一致した {@link ErrorNodeTreeItem} を格納する巡回リスト
   */
  private ImmutableCircularList<ErrorNodeTreeItem> searchAndHighlight(SearchQuery query) {
    ImmutableCircularList<ErrorNodeTreeItem> matchedItems = ItemSearcher.search(
        query,
        rootErrorNodeItem.collectDescendants(),
        treeItem -> ErrorNodeListCell.getText(treeItem.getValue()),
        maxResultsInErrorNodeList);
    searchResult = new SearchResult(matchedItems, query);
    highlightSearchResult(searchResult);
    return matchedItems;
  }

  private void highlightSearchResult(SearchResult result) {
    Pattern pattern = result.query().getPattern();
    for (ErrorNodeListItem listItem : result.listItems()) {
      cellRegistry
          .getCells(listItem.node())
          .forEach(cell -> cell.enableHighlighting(pattern, DEFAULT_TEXT_HIGHLIGHT));
    }
  }

  /** {@code item} の先祖要素を全て展開する. */
  private static void expandAncestorsOf(TreeItem<?> item) {
    var parent = item.getParent();
    while (parent != null) {
      parent.setExpanded(true);
      parent = parent.getParent();
    }
  }

  /** 現在表示されている {@link ErrorNodeListCell} の内容を更新する. */
  private void updateCellValues() {
    for (TreeItem<ErrorNodeListItem> item : rootErrorNodeItem.getChildren()) {
      BhNode node = item.getValue().node();
      cellRegistry.getCells(node).forEach(ErrorNodeListCell::updateValue);
    }
  }

  /** {@code node} に対応する {@link ErrorNodeListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    cellRegistry.getCells(node).forEach(cell -> cell.decorateText(node.isSelected()));
  }

  /**
   * {@link ErrorNodeListCell} に新しく割り当てられたアイテムの {@link BhNode} の選択状態に応じて,
   * セルの装飾を更新する.
   */
  private static void updateCellDecoration(ItemChangeEvent event) {
    boolean shouldDecorate =
        !event.empty()
        && Optional.ofNullable(event.newVal())
            .map(ErrorNodeListItem::node)
            .map(BhNode::isSelected)
            .orElse(false);
    event.cell().decorateText(shouldDecorate);
  }

  /** {@link ErrorNodeListCell} に新しく割り当てられたアイテムに応じて, セルの強調表示を更新する. */
  private void updateSearchResultHighlight(ItemChangeEvent event) {
    boolean shouldHighlight =
        !event.empty()
        && event.newVal() != null
        && searchResult != null
        && searchResult.listItems().contains(event.newVal());
    if (shouldHighlight) {
      event.cell().enableHighlighting(searchResult.query().getPattern(), DEFAULT_TEXT_HIGHLIGHT);
    } else {
      event.cell().disableHighlighting();
    }
  }

  /** {@link ErrorNodeListCell} に割り当てられるアイテムが変わったときの処理. */
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
    cellRegistry.getCells().forEach(ErrorNodeListCell::disableHighlighting);
  }

  /** 変数情報を表示する {@link TreeView} がの各要素のモデル. */
  private static class ErrorNodeTreeItem extends TreeItem<ErrorNodeListItem> {

    ErrorNodeTreeItem() {
      super(null);
    }

    ErrorNodeTreeItem(ErrorNodeListItem item) {
      super(item);
    }

    /**
     * このオブジェクトの子孫要素を深さ優先探査で取得して返す.
     *
     * @return このオブジェクトの子孫のコレクション.
     */
    public SequencedCollection<ErrorNodeTreeItem> collectDescendants() {
      SequencedCollection<ErrorNodeTreeItem> descendants = new ArrayList<>();
      getCurrentChildren().forEach(item -> item.collectSubTree(descendants));
      return descendants;
    }

    private void collectSubTree(SequencedCollection<ErrorNodeTreeItem> descendants) {
      descendants.addLast(this);
      getCurrentChildren().forEach(item -> item.collectSubTree(descendants));
    }

    /**
     * このオブジェクトが現在保持している子要素を取得する.
     *
     * <p>{@link #getChildren} は新しく子要素を作成して返すので, このメソッドを用意する.
     */
    private List<ErrorNodeTreeItem> getCurrentChildren() {
      return super.getChildren().stream()
          .map(item -> (ErrorNodeTreeItem) item)
          .collect(Collectors.toCollection(ArrayList::new));
    }
  }

  /** {@link SearchBox} を使ったエラーノード一覧の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {

    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return selectItem(query);
    }

    @Override
    public void onClosed() {
      clearSearchResult();
      enSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), false);
    }

    @Override
    public void onCleared() {}

    @Override
    public Object getUser() {
      return ErrorNodeListController.this;
    }
  }

  /**
   * {@link ErrorNodeListController} が生成した全ての {@link ErrorNodeListCell} を管理し,
   * 各セルに現在割り当てられている {@link BhNode} との対応関係を追跡するクラス.
   */
  private class CellRegistry {

    private final Map<BhNode, Set<ErrorNodeListCell>> nodeToCells = new HashMap<>();
    private final Set<ErrorNodeListCell> cells = new HashSet<>();

    /** {@link BhNode} と {@link ErrorNodeListCell} の対応関係を更新する. */
    void updateNodeToCellsMap(ItemChangeEvent event) {
      Optional.ofNullable(event.oldVal())
          .filter(oldVal -> event.empty() || oldVal != event.newVal())
          .map(ErrorNodeListItem::node)
          .filter(nodeToCells::containsKey)
          .ifPresent(node -> nodeToCells.get(node).remove(event.cell()));

      Optional.ofNullable(event.newVal())
          .filter(newVal -> !event.empty())
          .filter(newVal -> event.oldVal() != newVal)
          .map(ErrorNodeListItem::node)
          .ifPresent(node -> nodeToCells
              .computeIfAbsent(node, key -> Collections.newSetFromMap(new WeakHashMap<>()))
              .add(event.cell()));
    }

    /** 引数で指定した {@link BhNode} に対応する {@link ErrorNodeListCell} のセットを取得する. */
    Set<ErrorNodeListCell> getCells(BhNode node) {
      return nodeToCells.getOrDefault(node, new HashSet<>());
    }

    /** このオブジェクトが作成した全ての {@link ErrorNodeListCell} を取得する. */
    Set<ErrorNodeListCell> getCells() {
      return cells;
    }

    /** 引数で指定した {@link BhNode} と {@link ErrorNodeListCell} の対応関係を取り除く. */
    void removeMapping(BhNode node) {
      nodeToCells.remove(node);
    }

    /**
     * {@link VariableListCell} を生成し, このオブジェクトの管理下に加える.
     *
     * @return 生成した {@link VariableListCell}
     */
    ErrorNodeListCell createCell() {
      var cell = new ErrorNodeListCell();
      cell.setOnItemChanged(ErrorNodeListController.this::onCellItemChanged);
      cells.add(cell);
      return cell;
    }
  }

  /**
   * {@link ErrorNodeListController} が生成した全ての {@link ErrorNodeTreeItem} を管理し,
   * 各アイテムに割り当てられた {@link BhNode} との対応関係を追跡するクラス.
   */
  private static class TreeItemRegistry {

    private final Map<BhNode, ErrorNodeTreeItem> nodeToTreeItem = new HashMap<>();
    private final Set<ErrorNodeTreeItem> treeItems = new HashSet<>();

    /**
     * 引数で指定した {@link BhNode} に対応する {@link ErrorNodeTreeItem} を取得する.
     *
     * <p>対応する {@link BhNode} が存在しない場合, 新たに作成して返す.

     * @return {@code node} に対応する {@link ErrorNodeTreeItem}
     */
    ErrorNodeTreeItem getTreeItem(BhNode node) {
      return nodeToTreeItem.computeIfAbsent(
          node,
          bhNode -> {
            var item = new ErrorNodeTreeItem(new ErrorNodeListItem(bhNode));
            treeItems.add(item);
            return item;
          });
    }

    /** このオブジェクトが作成した全ての {@link ErrorNodeTreeItem} を取得する. */
    Set<ErrorNodeTreeItem> getTreeItems() {
      return treeItems;
    }

    /**
     * 引数で指定した {@link BhNode} と {@link ErrorNodeTreeItem} の対応関係を取り除く.
     *
     * @return {@code node} と対応関係にあった {@link ErrorNodeTreeItem}
     */
    ErrorNodeTreeItem removeMapping(BhNode node) {
      return nodeToTreeItem.remove(node);
    }
  }

  /** 検索結果を格納するレコード. */
  record SearchResult(
      ImmutableCircularList<ErrorNodeTreeItem> treeItems,
      Set<ErrorNodeListItem> listItems,
      SearchQuery query) {

    SearchResult(ImmutableCircularList<ErrorNodeTreeItem> treeItems, SearchQuery query) {
      this(
          treeItems,
          treeItems.getItems().stream()
              .map(TreeItem::getValue)
              .collect(Collectors.toCollection(HashSet::new)),
          query);
    }
  }
}
