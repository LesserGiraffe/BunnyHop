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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.variable.ListVariable;
import net.seapanda.bunnyhop.debugger.model.variable.ScalarVariable;
import net.seapanda.bunnyhop.debugger.model.variable.Variable;
import net.seapanda.bunnyhop.debugger.model.variable.VariableInfo;
import net.seapanda.bunnyhop.debugger.model.variable.VariableListItem;
import net.seapanda.bunnyhop.debugger.view.VariableListCell;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.ui.model.SearchQueryResult;
import net.seapanda.bunnyhop.ui.service.search.ItemSearcher;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import org.apache.commons.lang3.StringUtils;

/**
 * 変数情報を表示するビューのコントローラ.
 *
 * @author K.Koike
 */
public class VariableInspectionController {

  @FXML private VBox variableInspectionViewBase;
  @FXML private Label viViewName;
  @FXML private TreeView<VariableListItem> variableTreeView;
  @FXML private Button viSearchButton;
  @FXML private CheckBox viJumpCheckBox;
  @FXML private Button viReloadBtn;

  private final VariableInfo varInfo;
  private final SearchBox searchBox;
  private final Debugger debugger;
  private final WorkspaceSet wss;
  private final String viewName;
  private final VariableTreeItem rootVarItem;
  private final DataStore dataStore;
  private boolean isDiscarded = false;
  private final Consumer<WorkspaceSet.NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());
  private final Function<SearchQuery, SearchQueryResult> onSearchRequested = this::selectItem;
  private ImmutableCircularList<VariableTreeItem> searchResult;

  /**
   * コンストラクタ.
   *
   * @param varInfo このコントローラが管理するビューに表示する変数情報を格納したオブジェクト
   * @param viewName ビューの名前
   * @param searchBox 検索クエリを受け取る UI コンポーネントのインタフェース
   */
  public VariableInspectionController(
      VariableInfo varInfo,
      String viewName,
      SearchBox searchBox,
      Debugger debugger,
      WorkspaceSet wss) {
    this.viewName = (viewName == null) ? "" : viewName;
    this.varInfo = varInfo;
    this.searchBox = searchBox;
    this.debugger = debugger;
    this.wss = wss;
    this.rootVarItem = new VariableTreeItem();
    this.dataStore = new DataStore();
    rootVarItem.setExpanded(false);
    addVarInfo(varInfo.getVariables());
  }

  /** ビューに変数情報を追加する. */
  private void addVarInfo(SequencedCollection<Variable> variables) {
    for (Variable variable : variables) {
      if (variable instanceof ScalarVariable scalar) {
        rootVarItem.getChildren().add(createTreeItem(scalar));
      } else if (variable instanceof ListVariable list) {
        rootVarItem.getChildren().add(createTreeItem(list));
        if (list.length == 1) {
          requestListVals(list, 0, 1);
        }
      }
    }
    searchResult = null;
  }

  /** デバッガにリスト変数の値を取得するリクエストを出す. */
  private void requestListVals(ListVariable list, long startIdx, long length) {
    if (varInfo.getStackFrameId().isPresent()) {
      list.getNode().ifPresent(node -> debugger.requestLocalListVals(node, startIdx, length));
    } else {
      list.getNode().ifPresent(node -> debugger.requestGlobalListVals(node, startIdx, length));
    }
  }

  /** ビューから変数情報を削除する. */
  private void removeVarInfo(Collection<Variable> variables) {
    Map<Variable, TreeItem<VariableListItem>> varItemToTreeItem = rootVarItem.getChildren().stream()
        .collect(Collectors.toMap(item -> item.getValue().variable, UnaryOperator.identity()));
    for (Variable variable : variables) {
      TreeItem<VariableListItem> treeItem = varItemToTreeItem.get(variable);
      treeItem.getParent().getChildren().remove(treeItem);
    }
    searchResult = null;
  }

  private VariableTreeItem createTreeItem(ScalarVariable scalar) {
    var item = new VariableListItem(scalar);
    item.getCallbackRegistry().getOnValueChanged().add(event -> updateCellValues(event.item()));
    return new VariableTreeItem(item);
  }

  private VariableTreeItem createTreeItem(ListVariable list) {
    var item = new VariableListItem(list, 0, list.length - 1);
    item.getCallbackRegistry().getOnValueChanged().add(event -> updateCellValues(event.item()));
    return new VariableTreeItem(item);
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    viViewName.setText(viewName);
    variableTreeView.setShowRoot(false);
    variableTreeView.setRoot(rootVarItem);
    variableTreeView.setCellFactory(
        items -> new VariableListCell(dataStore.varItemToVarCells, dataStore.nodeToVarCells));
    variableTreeView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> onVariableSelected(newVal));
    variableTreeView.focusedProperty().addListener(
        (obs, oldVal, newVal) -> onFocusChanged(newVal));

    viReloadBtn.setOnAction(event -> reloadVarInfo());
    viSearchButton.setOnAction(action -> prepareSearchUi());
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
    VariableInfo.CallbackRegistry registry = varInfo.getCallbackRegistry();
    registry.getOnVariablesAdded().add(event -> addVarInfo(event.added()));
    registry.getOnVariablesRemoved().add(event -> removeVarInfo(event.removed()));
    registry.getOnValueChanged().add(event -> searchResult = null);
  }


  /** 変数が選択された時の処理. */
  private void onVariableSelected(TreeItem<VariableListItem> item) {
    if (!viJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(item)
        .map(TreeItem::getValue)
        .map(varListItem -> varListItem.variable)
        .flatMap(Variable::getNode)
        .flatMap(BhNode::getView)
        .ifPresent(view -> ViewUtil.jump(view, true, BhNodeView.LookManager.EffectTarget.SELF));
  }

  /** フォーカスが変更されたときの処理. */
  private void onFocusChanged(Boolean isFocused) {
    if (!isFocused) {
      variableTreeView.getSelectionModel().clearSelection();
    }
  }

  /** このコントローラが管理するビューのルート要素を返す. */
  public Node getView() {
    return variableInspectionViewBase;
  }

  /** このコントローラが管理するモデルを返す. */
  public VariableInfo getModel() {
    return varInfo;
  }

  /** このコントローラを破棄するときに呼ぶこと. */
  public void discard() {
    if (isDiscarded) {
      return;
    }
    isDiscarded = true;
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().remove(onNodeSelStateChanged);
    if (searchBox.unsetOnSearchRequested(onSearchRequested)) {
      searchBox.disable();
    }
    dataStore.clear();
    variableTreeView.setRoot(null);
  }

  /** {@code varItem} に対応する {@link VariableListCell} の内容を変更する. */
  private void updateCellValues(VariableListItem varItem) {
    if (isDiscarded) {
      return;
    }
    if (dataStore.varItemToVarCells.containsKey(varItem)) {
      dataStore.varItemToVarCells.get(varItem).forEach(VariableListCell::updateValue);
    }
  }

  /** {@code node} に対応する {@link VariableListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (isDiscarded) {
      return;
    }
    if (dataStore.nodeToVarCells.containsKey(node)) {
      dataStore.nodeToVarCells.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
    }
  }

  /** 変数情報を再取得する. */
  private void reloadVarInfo() {
    varInfo.clearVariables();
    if (varInfo.getStackFrameId().isPresent()) {
      debugger.requestLocalVars();
    } else {
      debugger.requestGlobalVars();
    }
  }

  /** {@link VariableInspectionController} が高速にデータにアクセスするための Map を集めたレコード. */
  private record DataStore(
      Map<VariableListItem, Set<VariableListCell>> varItemToVarCells,
      Map<BhNode, Set<VariableListCell>> nodeToVarCells) {

    public DataStore() {
      this(new HashMap<>(), new HashMap<>());
    }

    /** このオブジェクトが持つデータをクリアする. */
    public void clear() {
      varItemToVarCells.clear();
      nodeToVarCells.clear();
    }
  }

  /** 検索 UI の準備をする. */
  private void prepareSearchUi() {
    if (isDiscarded) {
      return;
    }
    searchBox.setOnSearchRequested(onSearchRequested);
    searchBox.enable();
  }

  /** 変数一覧から {@code query} に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (isDiscarded || StringUtils.isEmpty(query.word())) {
      return new SearchQueryResult(0, 0);
    }
    VariableTreeItem found = null;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      found = query.findNext() ? searchResult.getNext() : searchResult.getPrevious();
    } else {
      searchResult = ItemSearcher.<VariableTreeItem>search(
          query, rootVarItem.collectDescendants(), VariableTreeItem::toString);
      found = searchResult.getCurrent();
    }
    if (found != null) {
      expandAncestorsOf(found);
      variableTreeView.getSelectionModel().select(found);
      int index = variableTreeView.getRow(found);
      variableTreeView.scrollTo(index);
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

  /** 変数情報を表示する {@link TreeView} がの各要素のモデル. */
  private class VariableTreeItem extends TreeItem<VariableListItem> {

    private final VariableListItem item;
    private final boolean isLeaf;
    private boolean isFirstTimeChildren = true;

    VariableTreeItem() {
      super(null);
      this.item = null;
      isLeaf = false;
    }

    VariableTreeItem(VariableListItem item) {
      super(item);
      this.item = item;
      isLeaf = item.numValues <= 1;
    }

    @Override
    public ObservableList<TreeItem<VariableListItem>> getChildren() {
      if (shouldCreateChildren()) {
        isFirstTimeChildren = false;
        List<VariableListItem> subItems = item.createSubItems();
        setEventHandlers(subItems);
        requestListValues(subItems);
        var children = subItems.stream().map(VariableTreeItem::new).toList();
        super.getChildren().setAll(children);
        if (!children.isEmpty()) {
          searchResult = null;
        }
      }
      return super.getChildren();
    }

    /** このノードの子要素を作るべきか調べる. */
    private boolean shouldCreateChildren() {
      return !isDiscarded
          && isFirstTimeChildren
          && item != null
          && item.variable instanceof ListVariable;
    }

    private void setEventHandlers(List<VariableListItem> items) {
      for (VariableListItem item : items) {
        item.getCallbackRegistry().getOnValueChanged()
            .add(event -> updateCellValues(event.item()));
      }
    }

    /** リスト変数の値の取得をデバッガに命令する. */
    private void requestListValues(List<VariableListItem> subItems) {
      if (item.numValues <= BhSettings.Debug.maxListTreeChildren) {
        requestListVals((ListVariable) item.variable, item.startIdx, item.numValues);
        return;
      }
      for (VariableListItem subItem : subItems) {
        if (subItem.numValues == 1) {
          requestListVals((ListVariable) subItem.variable, subItem.startIdx, subItem.numValues);
        }
      }
    }

    /**
     * このオブジェクトが現在保持している子要素を取得する.
     *
     * <p>{@link #getChildren} は新しく子要素を作成して返すので, このメソッドを用意する.
     */
    public List<VariableTreeItem> getCurrentChildren() {
      return super.getChildren().stream()
          .map(item -> (VariableTreeItem) item)
          .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * このオブジェクトの子孫要素を深さ優先探査で取得して返す.
     *
     * @return このオブジェクトの子孫のコレクション.  先頭の要素はこのオブジェクト.
     */
    public SequencedCollection<VariableTreeItem> collectDescendants() {
      SequencedCollection<VariableTreeItem> descendants = new ArrayList<>();
      collectDescendants(descendants);
      return descendants;
    }

    private void collectDescendants(SequencedCollection<VariableTreeItem> descendants) {
      descendants.addLast(this);
      getCurrentChildren().forEach(item -> item.collectDescendants(descendants));
    }

    @Override
    public boolean isLeaf() {
      return isLeaf;
    }
  }
}
