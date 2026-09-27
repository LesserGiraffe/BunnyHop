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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.DEFAULT_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInVariableInspection;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.JUMP_TARGET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.debugger.view.VariableListCell;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.linter.model.ErrorNodeListItem;
import net.seapanda.bunnyhop.linter.view.ErrorNodeListCell;
import net.seapanda.bunnyhop.linter.view.ErrorNodeListCell.ItemChangeEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.search.CyclicSublistFinder;
import net.seapanda.bunnyhop.search.ItemSearcher;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
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
  @FXML private CheckBox enJumpCheckBox;
  @FXML private SearchBox searchBoxController;

  private final WorkspaceSet wss;
  private final CompileErrorNodeCache compileErrorNodeCache;
  private final VisualEffectManager effectManager;
  private final ErrorNodeTreeItem rootErrorNodeItem;
  private final CellRegistry cellRegistry;
  private final TreeItemRegistry treeItemRegistry;
  private SearchResult searchResult;

  /** コンストラクタ. */
  public ErrorNodeListController(
      WorkspaceSet wss,
      CompileErrorNodeCache compileErrorNodeCache,
      VisualEffectManager visualEffectManager) {
    this.wss = wss;
    this.compileErrorNodeCache = compileErrorNodeCache;
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
    searchBoxController.setSearchBoxDelegate(new SearchBoxDelegateImpl());
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    enTreeView.setCellFactory(view -> cellRegistry.createCell());
    enTreeView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> onItemSelected(oldVal, newVal));
    enWsSelectorController.setOnWorkspaceSelected(
        event -> refreshErrorNodeList(event.newWs(), event.isAllSelected()));

    WorkspaceSet.CallbackRegistry wssCbRegistry = wss.getCallbackRegistry();
    wssCbRegistry.getOnNodeSelectionStateChanged().add(event -> updateCellDecoration(event.node()));
    wssCbRegistry.getOnNodeTextChanged().add(event -> {
      clearSearchResult();
      updateCellValues();
    });
    CompileErrorNodeCache.CallbackRegistry cbRegistry = compileErrorNodeCache.getCallbackRegistry();
    cbRegistry.getOnCompileErrorStateUpdated().add(event -> addErrorNode(event.updated()));
    cbRegistry.getOnNodeAdded().add(event -> addErrorNode(event.added()));
    cbRegistry.getOnNodeRemoved().add(event -> removeErrorNode(event.removed()));
  }

  /** エラーノード情報を一覧に追加する. */
  private void addErrorNode(BhNode node) {
    ErrorNodeTreeItem treeItem = treeItemRegistry.getOrCreateTreeItem(node);
    if (!enWsSelectorController.matchesSelection(node.getWorkspace())) {
      return;
    }
    if (treeItem.getParent() == null) {
      rootErrorNodeItem.getChildren().add(treeItem);
    }
    // エラーメッセージだけ変更された場合にも検索結果が無効になるので, 検索結果をクリアする必要がある.
    clearSearchResult();
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
    clearSearchResult();
  }

  /**
   * 引数で指定したワークスペース上にあるエラーノードを表示する.
   *
   * <p>{@code isAllSelected} が true なら, 全てのワークペース上にあるエラーノードを表示する.
   */
  private void refreshErrorNodeList(Workspace ws, boolean isAllSelected) {
    if (ws == null && !isAllSelected) {
      rootErrorNodeItem.getChildren().clear();
      return;
    }
    List<ErrorNodeTreeItem> treeItems = treeItemRegistry.getTreeItems().stream()
        .filter(treeItem -> treeItem.getValue().node().getWorkspace() == ws || isAllSelected)
        .toList();
    rootErrorNodeItem.getChildren().setAll(treeItems);
    updateCellValues();
  }

  /** エラーの項目が選択された時の処理. */
  private void onItemSelected(
      TreeItem<ErrorNodeListItem> deselected, TreeItem<ErrorNodeListItem> selected) {
    Optional.ofNullable(deselected)
        .map(TreeItem::getValue)
        .map(ErrorNodeListItem::node)
        .flatMap(BhNode::getView)
        .ifPresent(view -> effectManager.setEffectEnabled(view, false, JUMP_TARGET));

    if (!enJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(selected)
        .map(TreeItem::getValue)
        .map(ErrorNodeListItem::node)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView)
        .ifPresent(this::jumpTo);
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

  /** エラーノード情報を表示する {@link TreeView} の各要素のモデル. */
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
    public List<ErrorNodeTreeItem> collectDescendants() {
      List<ErrorNodeTreeItem> descendants = new ArrayList<>();
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
      if (query.isEmpty()) {
        clearSearchResult();
        return new SearchQueryResult(0, 0);
      }
      try {
        if (searchBoxController.getNumConsecutiveSameRequests() <= 1 || searchResult == null) {
          searchResult = search(query);
          highlightSearchResult(searchResult);
        }

        ErrorNodeTreeItem selectedItem =
            (ErrorNodeTreeItem) enTreeView.getSelectionModel().getSelectedItem();
        Optional<CyclicSublistFinder.Found<ErrorNodeTreeItem>> foundOpt = query.isForward()
            ? searchResult.finder.getItemAfter(selectedItem)
            : searchResult.finder.getItemBefore(selectedItem);

        foundOpt.ifPresent(found -> {
          expandAncestorsOf(found.getItem());
          int idx = enTreeView.getRow(found.getItem());
          enTreeView.getSelectionModel().select(idx);
          enTreeView.scrollTo(Math.max(idx - 1, 0));
        });

        int numResults = searchResult.treeItems.size();
        boolean truncated = numResults == maxResultsInVariableInspection;
        int idxInResults = foundOpt.map(CyclicSublistFinder.Found::getIdxInSublist).orElse(-1);
        return new SearchQueryResult(idxInResults, numResults, truncated);
      } catch (PatternSyntaxException e) {
        clearSearchResult();
        return new SearchQueryResult(true);
      }
    }

    /** {@code query} でエラーノード一覧を検索する. */
    private SearchResult search(SearchQuery query) {
      List<ErrorNodeTreeItem> allVarItems = rootErrorNodeItem.collectDescendants();
      List<ErrorNodeTreeItem> results = ItemSearcher.search(
          query,
          allVarItems,
          treeItem -> ErrorNodeListCell.getText(treeItem.getValue()),
          maxResultsInVariableInspection);
      return new SearchResult(results, allVarItems, query);
    }

    private void highlightSearchResult(SearchResult result) throws PatternSyntaxException {
      Pattern pattern = result.query.getPattern();
      cellRegistry.getCells()
          .forEach(cell -> cell.enableHighlighting(pattern, DEFAULT_TEXT_HIGHLIGHT));
    }

    /** {@code item} の先祖要素を全て展開する. */
    private static void expandAncestorsOf(TreeItem<?> item) {
      var parent = item.getParent();
      while (parent != null) {
        parent.setExpanded(true);
        parent = parent.getParent();
      }
    }

    @Override
    public void onSearchResultCleared() {
      clearSearchResult();
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
      cell.setOnItemChanged(this::onCellItemChanged);
      cells.add(cell);
      return cell;
    }

    /** {@link ErrorNodeListCell} に割り当てられるアイテムが変わったときの処理. */
    private void onCellItemChanged(ItemChangeEvent event) {
      cellRegistry.updateNodeToCellsMap(event);
      updateCellDecoration(event);
      updateSearchResultHighlight(event);
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
          && searchResult.listItems.contains(event.newVal());
      if (shouldHighlight) {
        event.cell().enableHighlighting(searchResult.query.getPattern(), DEFAULT_TEXT_HIGHLIGHT);
      } else {
        event.cell().disableHighlighting();
      }
    }
  }

  /**
   * {@link ErrorNodeListController} が生成した全ての {@link ErrorNodeTreeItem} を管理し,
   * 各アイテムに割り当てられた {@link BhNode} との対応関係を追跡するクラス.
   */
  private static class TreeItemRegistry {

    private final Map<BhNode, ErrorNodeTreeItem> nodeToTreeItem = new LinkedHashMap<>();

    /**
     * 引数で指定した {@link BhNode} に対応する {@link ErrorNodeTreeItem} を取得する.
     *
     * <p>対応する {@link BhNode} が存在しない場合, 新たに作成して返す.

     * @return {@code node} に対応する {@link ErrorNodeTreeItem}
     */
    ErrorNodeTreeItem getOrCreateTreeItem(BhNode node) {
      ErrorNodeTreeItem treeItem = nodeToTreeItem.computeIfAbsent(
          node,
          bhNode -> new ErrorNodeTreeItem(new ErrorNodeListItem(bhNode)));
      treeItem.getChildren().setAll(createErrorMessageItems(node));
      treeItem.setExpanded(true);
      return treeItem;
    }

    private static List<ErrorNodeTreeItem> createErrorMessageItems(BhNode node) {
      return node.getCompileErrorMessages().stream()
          .map(msg -> new ErrorNodeTreeItem(new ErrorNodeListItem(node, msg)))
          .toList();
    }

    /** このオブジェクトが作成した全ての {@link ErrorNodeTreeItem} を取得する. */
    Set<ErrorNodeTreeItem> getTreeItems() {
      return new HashSet<>(nodeToTreeItem.values());
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

  /** エラーノード一覧に対する検索結果を保持するクラス. */
  private static class SearchResult {

    private final CyclicSublistFinder<ErrorNodeTreeItem> finder;
    private final SequencedSet<ErrorNodeTreeItem> treeItems;
    private final SequencedSet<ErrorNodeListItem> listItems;
    private final SearchQuery query;

    SearchResult(
        List<ErrorNodeTreeItem> matchedItems, List<ErrorNodeTreeItem> allItems, SearchQuery query) {
      finder = new CyclicSublistFinder<>(matchedItems, allItems);
      treeItems = new LinkedHashSet<>(matchedItems);
      listItems = matchedItems.stream()
          .map(TreeItem::getValue)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      this.query = query;
    }
  }
}
