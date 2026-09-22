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
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInVariableInspection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.variable.ListVariable;
import net.seapanda.bunnyhop.debugger.model.variable.ScalarVariable;
import net.seapanda.bunnyhop.debugger.model.variable.Variable;
import net.seapanda.bunnyhop.debugger.model.variable.VariableInfo;
import net.seapanda.bunnyhop.debugger.model.variable.VariableListItem;
import net.seapanda.bunnyhop.debugger.view.VariableListCell;
import net.seapanda.bunnyhop.debugger.view.VariableListCell.ItemChangeEvent;
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
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet.NodeSelectionEvent;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet.NodeTextChangedEvent;

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
  private final VisualEffectManager effectManager;
  private final String viewName;
  private final VariableTreeItem rootVarItem;
  private final CellRegistry cellRegistry;
  private boolean isDiscarded = false;
  private BhNodeView lastJumpTarget;
  private final Consumer<NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());
  private final Consumer<NodeTextChangedEvent> onNodeTextChanged = event -> clearSearchResult();
  private SearchResult searchResult;
  /**
   * 変数情報を選択したときに対応するノードにジャンプするかどうかのフラグを
   * 複数の {@link VariableInspectionController} で共有するためのオブジェクト.
   */
  private final BooleanProperty sharedJumpFlag;
  private final SearchBoxDelegateImpl searchBoxDelegate = new SearchBoxDelegateImpl();

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
      WorkspaceSet wss,
      VisualEffectManager visualEffectManager,
      BooleanProperty sharedJumpFlag) {
    this.viewName = (viewName == null) ? "" : viewName;
    this.varInfo = varInfo;
    this.searchBox = searchBox;
    this.debugger = debugger;
    this.wss = wss;
    effectManager = visualEffectManager;
    this.sharedJumpFlag = sharedJumpFlag;
    cellRegistry = new CellRegistry();
    rootVarItem = new VariableTreeItem();
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
    clearSearchResult();
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
    // 変数情報を削除したときに別の変数情報が自動的に選択されるのを防ぐ
    Optional.ofNullable(variableTreeView.getSelectionModel().getSelectedItem())
        .map(TreeItem::getValue)
        .map(varListItem -> varListItem.variable)
        .filter(variables::contains)
        .ifPresent(variable -> variableTreeView.getSelectionModel().clearSelection());

    Map<Variable, TreeItem<VariableListItem>> varItemToTreeItem = rootVarItem.getChildren().stream()
        .collect(Collectors.toMap(item -> item.getValue().variable, UnaryOperator.identity()));
    for (Variable variable : variables) {
      TreeItem<VariableListItem> treeItem = varItemToTreeItem.get(variable);
      treeItem.getParent().getChildren().remove(treeItem);
    }
    clearSearchResult();
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
    viViewName.setText(viewName);
    variableTreeView.setShowRoot(false);
    variableTreeView.setRoot(rootVarItem);
    viJumpCheckBox.selectedProperty().bindBidirectional(sharedJumpFlag);
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    variableInspectionViewBase.parentProperty().addListener(
        (obs, oldVal, newVal) -> onViewParentChanged(newVal));
    variableTreeView.setCellFactory(view -> cellRegistry.createCell());
    variableTreeView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> onVariableSelected(newVal));
    variableTreeView.focusedProperty().addListener(
        (obs, oldVal, newVal) -> onFocusChanged(newVal));

    viReloadBtn.setOnAction(event -> reloadVarInfo());
    viSearchButton.setOnAction(action -> onSearchButtonClicked());
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
    wss.getCallbackRegistry().getOnNodeTextChanged().add(onNodeTextChanged);
    VariableInfo.CallbackRegistry registry = varInfo.getCallbackRegistry();
    registry.getOnVariablesAdded().add(event -> addVarInfo(event.added()));
    registry.getOnVariablesRemoved().add(event -> removeVarInfo(event.removed()));
    registry.getOnValueChanged().add(event -> clearSearchResult());
  }

  /** {@link VariableListCell} に割り当てられるアイテムが変わったときの処理. */
  private void onCellItemChanged(ItemChangeEvent event) {
    cellRegistry.updateItemToCellsMap(event);
    cellRegistry.updateNodeToCellsMap(event);
    updateCellDecoration(event);
    updateSearchResultHighlight(event);
  }

  /** 変数が選択されたときの処理. */
  private void onVariableSelected(TreeItem<VariableListItem> item) {
    if (!viJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(item)
        .map(TreeItem::getValue)
        .map(varListItem -> varListItem.variable)
        .flatMap(Variable::getNode)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView)
        .ifPresent(this::jumpTo);
  }

  /** フォーカスが変更されたときの処理. */
  private void onFocusChanged(Boolean isFocused) {
    if (!isFocused) {
      variableTreeView.getSelectionModel().clearSelection();
    }
  }

  /** 変数リストビューの親要素が変わったときのイベントハンドラ. */
  private void onViewParentChanged(Parent newParent) {
    if (newParent == null && searchBox.getUser() == this) {
      searchBox.close();
      viSearchButton.applyCss();
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
    wss.getCallbackRegistry().getOnNodeTextChanged().remove(onNodeTextChanged);
    if (searchBox.getUser() == this) {
      searchBox.close();
    }
    if (lastJumpTarget != null) {
      effectManager.setEffectEnabled(lastJumpTarget, false, VisualEffectType.JUMP_TARGET);
    }
    cellRegistry.clear();
    clearSearchResult();
    variableTreeView.setRoot(null);
    viJumpCheckBox.selectedProperty().unbindBidirectional(sharedJumpFlag);
  }

  /** {@code varItem} に対応する {@link VariableListCell} の内容を更新する. */
  private void updateCellValues(VariableListItem varItem) {
    if (isDiscarded) {
      return;
    }
    cellRegistry.getCells(varItem).forEach(VariableListCell::updateValue);
  }

  /** {@code nodes} に対応する {@link VariableListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (isDiscarded) {
      return;
    }
    cellRegistry.getCells(node).forEach(cell -> cell.decorateText(node.isSelected()));
  }

  /**
   * {@link VariableListCell} に新しく割り当てられたアイテムの {@link BhNode} の選択状態に応じて,
   * セルの装飾を更新する.
   */
  private static void updateCellDecoration(ItemChangeEvent event) {
    boolean shouldDecorate =
        !event.empty()
        && event.newVal() != null
        && event.newVal().variable.getNode()
          .map(BhNode::isSelected)
          .orElse(false);
    event.cell().decorateText(shouldDecorate);
  }

  /** {@link VariableListCell} に新しく割り当てられたアイテムに応じて, セルの強調表示を更新する. */
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

  /** 変数情報を再取得する. */
  private void reloadVarInfo() {
    varInfo.clearVariables();
    if (varInfo.getStackFrameId().isPresent()) {
      debugger.requestLocalVars();
    } else {
      debugger.requestGlobalVars();
    }
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
    viSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), true);
    searchBox.open(searchBoxDelegate);
  }

  /** 変数一覧から {@code query} に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (isDiscarded || query.isEmpty()) {
      return new SearchQueryResult(0, 0);
    }
    ImmutableCircularList<VariableTreeItem> matchedItems;
    VariableTreeItem found;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      matchedItems = searchResult.treeItems();
      found = query.isForward() ? matchedItems.getNext() : matchedItems.getPrevious();
    } else {
      matchedItems = searchAndHighlight(query);
      found = matchedItems.getCurrent();
    }
    if (found != null) {
      expandAncestorsOf(found);
      variableTreeView.getSelectionModel().select(found);
      int index = variableTreeView.getRow(found);
      variableTreeView.scrollTo(index);
    }
    boolean truncated = matchedItems.size() == maxResultsInVariableInspection;
    return new SearchQueryResult(matchedItems.getPointer(), matchedItems.size(), truncated);
  }

  /**
   * {@code query} で変数一覧全体を検索し, 一致した要素を強調表示した上で, それらを巡回可能なリストとして返す.
   *
   * @param query 検索条件
   * @return {@code query} に一致した {@link VariableTreeItem} を格納する巡回リスト
   */
  private ImmutableCircularList<VariableTreeItem> searchAndHighlight(SearchQuery query) {
    ImmutableCircularList<VariableTreeItem> matchedItems = ItemSearcher.search(
        query,
        rootVarItem.collectDescendants(),
        treeItem -> VariableListCell.getText(treeItem.getValue()),
        maxResultsInVariableInspection);
    searchResult = new SearchResult(matchedItems, query);
    highlightSearchResult(searchResult);
    return matchedItems;
  }

  private void highlightSearchResult(SearchResult result) {
    Pattern pattern = result.query().getPattern();
    for (VariableListItem listItem : result.listItems()) {
      cellRegistry
          .getCells(listItem)
          .forEach(cell -> cell.enableHighlighting(pattern, DEFAULT_TEXT_HIGHLIGHT));
    }
  }

  /** 現在の検索結果を破棄し, それに伴う強調表示を全て解除する. */
  private void clearSearchResult() {
    searchResult = null;
    cellRegistry.getCells().forEach(VariableListCell::disableHighlighting);
  }

  /** {@code item} の先祖要素を全て展開する. */
  private static void expandAncestorsOf(TreeItem<?> item) {
    var parent = item.getParent();
    while (parent != null) {
      parent.setExpanded(true);
      parent = parent.getParent();
    }
  }

  /** {@code view} にジャンプし, ジャンプ先となった際の視覚効果をつける. */
  private void jumpTo(BhNodeView view) {
    ViewUtil.jump(view);
    effectManager.disableEffects(VisualEffectType.JUMP_TARGET);
    effectManager.setEffectEnabled(view, true, VisualEffectType.JUMP_TARGET);
    lastJumpTarget = view;
  }

  /** 変数情報を表示する {@link TreeView} の各要素のモデル. */
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
          clearSearchResult();
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
     * @return このオブジェクトの子孫のコレクション.
     */
    public SequencedCollection<VariableTreeItem> collectDescendants() {
      SequencedCollection<VariableTreeItem> descendants = new ArrayList<>();
      getCurrentChildren().forEach(item -> item.collectSubTree(descendants));
      return descendants;
    }

    private void collectSubTree(SequencedCollection<VariableTreeItem> descendants) {
      descendants.addLast(this);
      getCurrentChildren().forEach(item -> item.collectSubTree(descendants));
    }

    @Override
    public boolean isLeaf() {
      return isLeaf;
    }
  }

  /** {@link SearchBox} を使った変数一覧の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {

    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return selectItem(query);
    }

    @Override
    public void onClosed() {
      clearSearchResult();
      viSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), false);
    }

    @Override
    public void onCleared() {}

    @Override
    public Object getUser() {
      return VariableInspectionController.this;
    }
  }

  /**
   * {@link VariableInspectionController} が生成した全ての {@link VariableListCell} を管理し,
   * 各セルに現在割り当てられている {@link VariableListItem} および {@link BhNode} との対応関係を追跡するクラス.
   */
  private class CellRegistry {

    private final Map<VariableListItem, Set<VariableListCell>> itemToCells = new HashMap<>();
    private final Map<BhNode, Set<VariableListCell>> nodeToCells = new HashMap<>();
    private final Set<VariableListCell> cells = new HashSet<>();

    /** このオブジェクトが持つデータをクリアする. */
    void clear() {
      itemToCells.clear();
      nodeToCells.clear();
      cells.clear();
    }

    /** {@link VariableListItem} と {@link VariableListCell} の対応関係を更新する. */
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

    /** {@link BhNode} と {@link VariableListCell} の対応関係を更新する. */
    void updateNodeToCellsMap(ItemChangeEvent event) {
      Optional.ofNullable(event.oldVal())
          .filter(oldVal -> event.empty() || oldVal != event.newVal())
          .flatMap(oldVal -> oldVal.variable.getNode())
          .filter(nodeToCells::containsKey)
          .ifPresent(node -> nodeToCells.get(node).remove(event.cell()));

      Optional.ofNullable(event.newVal())
          .filter(newVal -> !event.empty())
          .filter(newVal -> event.oldVal() != newVal)
          .flatMap(newVal -> newVal.variable.getNode())
          .ifPresent(node -> nodeToCells
              .computeIfAbsent(node, key -> Collections.newSetFromMap(new WeakHashMap<>()))
              .add(event.cell()));
    }

    /** 引数で指定した {@link VariableListItem} に対応する {@link VariableListCell} のセットを取得する. */
    Set<VariableListCell> getCells(VariableListItem item) {
      return itemToCells.getOrDefault(item, new HashSet<>());
    }

    /** 引数で指定した {@link BhNode} に対応する {@link VariableListCell} のセットを取得する. */
    Set<VariableListCell> getCells(BhNode node) {
      return nodeToCells.getOrDefault(node, new HashSet<>());
    }

    /** このオブジェクトが作成した全ての {@link VariableListCell} を取得する. */
    Set<VariableListCell> getCells() {
      return cells;
    }

    /**
     * {@link VariableListCell} を生成し, このオブジェクトの管理下に加える.
     *
     * @return 生成した {@link VariableListCell}
     */
    VariableListCell createCell() {
      var cell = new VariableListCell();
      cell.setOnItemChanged(VariableInspectionController.this::onCellItemChanged);
      cells.add(cell);
      return cell;
    }
  }

  /** 検索結果を格納するレコード. */
  record SearchResult(
      ImmutableCircularList<VariableTreeItem> treeItems,
      Set<VariableListItem> listItems,
      SearchQuery query) {

    SearchResult(ImmutableCircularList<VariableTreeItem> treeItems, SearchQuery query) {
      this(
          treeItems,
          treeItems.getItems().stream()
              .map(treeItem -> treeItem.item)
              .collect(Collectors.toCollection(HashSet::new)),
          query);
    }
  }
}
