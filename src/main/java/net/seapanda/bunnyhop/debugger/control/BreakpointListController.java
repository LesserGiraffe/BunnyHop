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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.debugger.model.breakpoint.BreakpointCache;
import net.seapanda.bunnyhop.debugger.view.BreakpointListCell;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
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

  private final Map<BhNode, Set<BreakpointListCell>> nodeToCells = new HashMap<>();
  private ImmutableCircularList<BhNode> searchResult;

  /** コンストラクタ. */
  public BreakpointListController(
      WorkspaceSet wss,
      BreakpointCache breakpointCache,
      SearchBox searchBox,
      VisualEffectManager visualEffectManager) {
    this.wss = wss;
    this.breakpointCache = breakpointCache;
    this.searchBox = searchBox;
    this.effectManager = visualEffectManager;
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

    bpListView.setCellFactory(stack -> new BreakpointListCell(nodeToCells));
    bpListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onBreakpointSelected(newVal));
    bpListView.itemsProperty().addListener((obs, oldVal, newVal) -> searchResult = null);
    bpListView.focusedProperty().addListener(
        (obs, oldVal, newVal) -> onFocusChanged(newVal));

    bpSearchButton.setOnAction(action -> prepareForSearch());
    bpWsSelectorController.setOnWorkspaceSelected(
        event -> showBreakpoints(event.newWs(), event.isAllSelected()));
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged()
        .add(event -> updateCellDecoration(event.node()));
  }

  /** ブレークポイント一覧のブレークポイントが選択されたときのイベントハンドラ. */
  private void onBreakpointSelected(BhNode selected) {
    if (!bpJumpCheckBox.isSelected()) {
      return;
    }
    Optional.ofNullable(selected)
        .filter(BhNode::isInWorkspace)
        .flatMap(BhNode::getView)
        .ifPresent(this::jumpTo);
  }

  /** フォーカスが変更されたときの処理. */
  private void onFocusChanged(Boolean isFocused) {
    if (isFocused) {
      updateCellValues();
    }
  }

  /** 検索の準備をする. */
  private void prepareForSearch() {
    searchBox.setOnSearchRequested(this::selectItem);
    searchBox.enable();
    updateCellValues();
  }

  /** ブレークポイント一覧から {@code query} で指定された文字列に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (StringUtils.isEmpty(query.word())) {
      return new SearchQueryResult(0, 0);
    }
    BhNode found = null;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      found = query.findNext() ? searchResult.getNext() : searchResult.getPrevious();
    } else {
      searchResult =
          ItemSearcher.<BhNode>search(query, bpListView.getItems(), BreakpointListCell::getText);
      found = searchResult.getCurrent();
    }
    if (found != null) {
      bpListView.getSelectionModel().select(found);
      bpListView.scrollTo(found);
    }
    return new SearchQueryResult(searchResult.getPointer(), searchResult.size());
  }

  /** {@code node} をブレークポイント一覧に加える. */
  private void addBreakpoint(BhNode node) {
    if (bpWsSelectorController.isAllSelected()
        || bpWsSelectorController.getSelected().orElse(new Workspace("")) == node.getWorkspace()) {
      bpListView.getItems().add(node);
    }
  }

  /** {@code node} をブレークポイント一覧から削除する. */
  private void removeBreakpoint(BhNode node) {
    // ブレークポイントを削除したときに別のブレークポイントが自動的に選択されるのを防ぐ
    if (bpListView.getSelectionModel().getSelectedItem() == node) {
      bpListView.getSelectionModel().clearSelection();
    }
    bpListView.getItems().remove(node);
    nodeToCells.remove(node);
  }

  /** {@code ws} 上にある, ブレークポイントを指定されたノードを表示する. */
  private void showBreakpoints(Workspace ws, boolean isAllSelected) {
    bpListView.getItems().clear();
    if (ws == null && !isAllSelected) {
      return;
    }
    List<BhNode> bpNodes = breakpointCache.getBreakpoints().stream()
        .filter(node -> node.getWorkspace() == ws || isAllSelected)
        .toList();
    bpListView.getItems().addAll(bpNodes);
    updateCellValues();
  }

  /** 現在表示されている {@link BreakpointListCell} の内容を更新する. */
  private void updateCellValues() {
    for (BhNode node : bpListView.getItems()) {
      Set<BreakpointListCell> cells = nodeToCells.get(node);
      if (cells != null) {
        cells.forEach(BreakpointListCell::updateValue);
      }
    }
  }

  /** {@code node} に対応する {@link BreakpointListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (nodeToCells.containsKey(node)) {
      nodeToCells.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
    }
  }

  /** {@code view} にジャンプし, ジャンプ先となった際の視覚効果をつける. */
  private void jumpTo(BhNodeView view) {
    ViewUtil.jump(view);
    effectManager.disableEffects(VisualEffectType.JUMP_TARGET);
    effectManager.setEffectEnabled(view, true, VisualEffectType.JUMP_TARGET);
  }
}
