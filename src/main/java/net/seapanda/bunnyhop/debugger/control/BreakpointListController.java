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


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.debugger.model.breakpoint.BreakpointRegistry;
import net.seapanda.bunnyhop.debugger.view.BreakpointListCell;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.LookManager.EffectTarget;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.ui.model.SearchQueryResult;
import net.seapanda.bunnyhop.ui.service.search.ItemSearcher;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
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
  private final ModelAccessNotificationService notifService;
  private final SearchBox searchBox;
  private final BreakpointRegistry breakpointRegistry;
  private final Map<BhNode, Set<BreakpointListCell>> nodeToCallBpListCell = new WeakHashMap<>();
  private ImmutableCircularList<BhNode> searchResult;

  /** コンストラクタ. */
  public BreakpointListController(
      WorkspaceSet wss,
      ModelAccessNotificationService notifService,
      SearchBox searchBox,
      BreakpointRegistry breakpointRegistry) {
    this.wss = wss;
    this.notifService = notifService;
    this.searchBox = searchBox;
    this.breakpointRegistry = breakpointRegistry;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    breakpointRegistry.getCallbackRegistry()
        .getOnBreakpointAdded().add(event -> addBreakpointToList(event.breakpoint()));
    breakpointRegistry.getCallbackRegistry()
        .getOnBreakpointRemoved().add(event -> removeBreakpointFromList(event.breakpoint()));

    bpListView.setCellFactory(stack -> new BreakpointListCell(nodeToCallBpListCell));
    bpListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onBreakpointSelected(oldVal, newVal));
    bpListView.itemsProperty().addListener((obs, oldVal, newVal) -> searchResult = null);

    bpSearchButton.setOnAction(action -> {
      searchBox.setOnSearchRequested(this::selectItem);
      searchBox.enable();
    });
    bpWsSelectorController.setOnWorkspaceSelected(
        event -> showBreakpoints(event.newWs(), event.isAllSelected()));
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged()
        .add(event -> updateCellDecoration(event.node()));
  }

  /** ブレークポイント一覧のブレークポイントが選択されたときのイベントハンドラ. */
  private void onBreakpointSelected(BhNode deselected, BhNode selected) {
    Context context = notifService.beginWrite();
    try {
      var userOpe = new UserOperation();
      if (deselected != null) {
        deselected.deselect(userOpe);
      }
      if (selected != null) {
        if (bpJumpCheckBox.isSelected()) {
          jump(selected, context.userOpe());
        }
      }
    } finally {
      notifService.endWrite();
    }
  }

  /** {@code node} を選択して, これを画面中央に表示する. */
  private static void jump(BhNode node, UserOperation userOpe) {
    BhNodeView nodeView = node.getView().orElse(null);
    if (node.isDeleted() || nodeView == null) {
      return;
    }
    ViewUtil.jump(nodeView, true, EffectTarget.SELF);
    node.getWorkspace().getSelectedNodes().forEach(selectedNode -> selectedNode.deselect(userOpe));
    node.select(userOpe);
  }

  /** コールスタックから {@code query} で指定された文字列に一致する要素を探して選択する. */
  private SearchQueryResult selectItem(SearchQuery query) {
    if (StringUtils.isEmpty(query.word())) {
      return new SearchQueryResult(0, 0);
    }
    BhNode found = null;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      found = query.findNext() ? searchResult.getNext() : searchResult.getPrevious();
    } else {
      searchResult = ItemSearcher.<BhNode>search(query, bpListView.getItems(), BhNode::getAlias);
      found = searchResult.getCurrent();
    }
    if (found != null) {
      bpListView.getSelectionModel().select(found);
      bpListView.scrollTo(found);
    }
    return new SearchQueryResult(searchResult.getPointer(), searchResult.size());
  }

  /** {@code node} をブレークポイント一覧に加える. */
  private void addBreakpointToList(BhNode node) {
    Workspace ws = node.getWorkspace();
    if (bpWsSelectorController.getSelected().map(selected -> selected == ws).orElse(false)
        || bpWsSelectorController.isAllSelected()) {
      bpListView.getItems().add(node);
    }
  }

  /** {@code node} をブレークポイント一覧から削除する. */
  private void removeBreakpointFromList(BhNode node) {
    bpListView.getItems().remove(node);
    nodeToCallBpListCell.remove(node);
  }

  /** {@code ws} 上にある, ブレークポイントを指定されたノードを表示する. */
  private void showBreakpoints(Workspace ws, boolean isAllSelected) {
    bpListView.getItems().clear();
    if (ws == null && !isAllSelected) {
      return;
    }
    List<BhNode> bpNodes = breakpointRegistry.getBreakpointNodes().stream()
        .filter(node -> node.getWorkspace() == ws || isAllSelected)
        .toList();
    bpListView.getItems().addAll(bpNodes);
  }

  /** {@code node} に対応する {@link BreakpointListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (nodeToCallBpListCell.containsKey(node)) {
      nodeToCallBpListCell.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
    }
  }
}
