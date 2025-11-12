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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.linter.model.ErrorNodeListItem;
import net.seapanda.bunnyhop.linter.view.ErrorNodeListCell;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.ui.model.SearchQueryResult;
import net.seapanda.bunnyhop.ui.service.search.ItemSearcher;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSelectorController;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import org.apache.commons.lang3.StringUtils;

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
  private final SearchBox searchBox;
  private final ErrorNodeTreeItem rootErrorNodeItem;
  private final DataStore dataStore;
  private final Consumer<WorkspaceSet.NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());
  private final Function<SearchQuery, SearchQueryResult> onSearchRequested = this::selectItem;
  private ImmutableCircularList<ErrorNodeTreeItem> searchResult;


  /** コンストラクタ. */
  public ErrorNodeListController(
      WorkspaceSet wss, CompileErrorNodeCache compileErrorNodeCache, SearchBox searchBox) {
    this.wss = wss;
    this.compileErrorNodeCache = compileErrorNodeCache;
    this.searchBox = searchBox;
    this.rootErrorNodeItem = new ErrorNodeTreeItem();
    this.dataStore = new DataStore();
    rootErrorNodeItem.setExpanded(false);
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    enTreeView.setShowRoot(false);
    enTreeView.setRoot(rootErrorNodeItem);
    enTreeView.setCellFactory(
        view -> new ErrorNodeListCell(dataStore.nodeToCells));
    enTreeView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> onItemSelected(newVal));
    enTreeView.focusedProperty().addListener(
        (obs, oldVal, newVal) -> onFocusChanged(newVal));
    rootErrorNodeItem.getChildren().addListener(
        (ListChangeListener<? super TreeItem<ErrorNodeListItem>>) change -> searchResult = null);
    enSearchButton.setOnAction(action -> prepareForSearch());
    enWsSelectorController.setOnWorkspaceSelected(
        event -> showErrorNodes(event.newWs(), event.isAllSelected()));

    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
    CompileErrorNodeCache.CallbackRegistry registry = compileErrorNodeCache.getCallbackRegistry();
    registry.getOnCompileErrorStateUpdated().add(event -> addErrorNode(event.updated()));
    registry.getOnNodeAdded().add(event -> addErrorNode(event.added()));
    registry.getOnNodeRemoved().add(event -> removeErrorNode(event.removed()));
  }

  /** エラーノード情報を一覧に追加する. */
  private void addErrorNode(BhNode node) {
    ErrorNodeTreeItem nodeTreeItem = dataStore.nodeToTreeItem.computeIfAbsent(
        node, bhNode -> new ErrorNodeTreeItem(new ErrorNodeListItem(bhNode)));
    var messages = node.getCompileErrorMessages().stream()
        .map(msg -> new ErrorNodeTreeItem(new ErrorNodeListItem(node, msg)))
        .toList();
    nodeTreeItem.getChildren().clear();
    nodeTreeItem.getChildren().addAll(messages);
    nodeTreeItem.setExpanded(true);
    if (!enWsSelectorController.isAllSelected()
        && enWsSelectorController.getSelected().orElse(new Workspace("")) != node.getWorkspace()) {
      return;
    }
    if (nodeTreeItem.getParent() == null) {
      rootErrorNodeItem.getChildren().add(nodeTreeItem);
    }
  }

  /** エラーノード情報を一覧から削除する. */
  private void removeErrorNode(BhNode node) {
    dataStore.nodeToCells.remove(node);
    ErrorNodeTreeItem nodeTreeItem = dataStore.nodeToTreeItem.remove(node);
    if (nodeTreeItem.getParent() != null) {
      rootErrorNodeItem.getChildren().remove(nodeTreeItem);
    }
  }

  /** {@code ws} 上にあるエラーノードを表示する. */
  private void showErrorNodes(Workspace ws, boolean isAllSelected) {
    rootErrorNodeItem.getChildren().clear();
    if (ws == null && !isAllSelected) {
      return;
    }
    List<ErrorNodeTreeItem> items = dataStore.nodeToTreeItem.entrySet().stream()
        .filter(entry -> entry.getKey().getWorkspace() == ws || isAllSelected)
        .map(Map.Entry::getValue)
        .toList();

    rootErrorNodeItem.getChildren().addAll(items);
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
        .flatMap(BhNode::getView)
        .ifPresent(view -> ViewUtil.jump(view, true, BhNodeView.LookManager.EffectTarget.SELF));
  }

  /** フォーカスが変更されたときの処理. */
  private void onFocusChanged(Boolean isFocused) {
    if (!isFocused) {
      enTreeView.getSelectionModel().clearSelection();
    } else {
      updateCellValues();
    }
  }

  /** 検索の準備をする. */
  private void prepareForSearch() {
    searchBox.setOnSearchRequested(onSearchRequested);
    searchBox.enable();
    updateCellValues();
  }

  /** 変数一覧から {@code query} に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (StringUtils.isEmpty(query.word())) {
      return new SearchQueryResult(0, 0);
    }
    ErrorNodeTreeItem found = null;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      found = query.findNext() ? searchResult.getNext() : searchResult.getPrevious();
    } else {
      searchResult = ItemSearcher.<ErrorNodeTreeItem>search(
          query,
          rootErrorNodeItem.collectDescendants(),
          treeItem -> ErrorNodeListCell.getText(treeItem.getValue()));
      found = searchResult.getCurrent();
    }
    if (found != null) {
      expandAncestorsOf(found);
      enTreeView.getSelectionModel().select(found);
      int index = enTreeView.getRow(found);
      enTreeView.scrollTo(index);
    }
    return new SearchQueryResult(searchResult.getPointer(), searchResult.size());
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
      Set<ErrorNodeListCell> cells = dataStore.nodeToCells.get(item.getValue().node());
      if (cells != null) {
        cells.forEach(ErrorNodeListCell::updateValue);
      }
    }
  }

  /** {@code node} に対応する {@link ErrorNodeListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (dataStore.nodeToCells.containsKey(node)) {
      dataStore.nodeToCells.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
    }
  }

  /** {@link ErrorNodeListController} が高速にデータにアクセスするための Map を集めたレコード. */
  private record DataStore(
      Map<BhNode, ErrorNodeTreeItem> nodeToTreeItem,
      Map<BhNode, Set<ErrorNodeListCell>> nodeToCells) {

    public DataStore() {
      this(new HashMap<>(), new HashMap<>());
    }
  }

  /** 変数情報を表示する {@link TreeView} がの各要素のモデル. */
  private class ErrorNodeTreeItem extends TreeItem<ErrorNodeListItem> {

    private final ErrorNodeListItem item;

    ErrorNodeTreeItem() {
      super(null);
      this.item = null;
    }

    ErrorNodeTreeItem(ErrorNodeListItem item) {
      super(item);
      this.item = item;
    }

    /**
     * このオブジェクトの子孫要素を深さ優先探査で取得して返す.
     *
     * @return このオブジェクトの子孫のコレクション.  先頭の要素はこのオブジェクト.
     */
    public SequencedCollection<ErrorNodeTreeItem> collectDescendants() {
      SequencedCollection<ErrorNodeTreeItem> descendants = new ArrayList<>();
      collectDescendants(descendants);
      return descendants;
    }

    private void collectDescendants(SequencedCollection<ErrorNodeTreeItem> descendants) {
      descendants.addLast(this);
      getCurrentChildren().forEach(item -> item.collectDescendants(descendants));
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
}
