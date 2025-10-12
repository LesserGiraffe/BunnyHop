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


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.bhprogram.debugger.BreakpointRegistry;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.control.SearchBox.Query;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.debugger.BreakpointListCell;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.LookManager.EffectTarget;
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
      var userOpe = new UserOperation(); // コールスタックの選択は undo / redo の対象にしない
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
  private void selectItem(Query query) {
    BhNode node = null;
    if (StringUtils.isEmpty(query.word())) {
      return;
    }
    if (query.isRegex()) {
      try {
        int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(query.word(), regexFlag);
        node = searchCallStackFor(pattern, query.findNext());
      } catch (PatternSyntaxException e) { /* do nothing. */ }
    } else {
      node = searchCallStackFor(query.word(), query.findNext(), query.isCaseSensitive());
    }
    if (node != null) {
      bpListView.getSelectionModel().select(node);
      bpListView.scrollTo(node);  
    }
  }

  /** コールスタックから {@code query} に一致する {@link BhNode} を探して選択する. */
  private BhNode searchCallStackFor(String word, boolean findNext, boolean caseSensitive) {
    if (!caseSensitive) {
      word = word.toLowerCase();
    }
    int size = bpListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = bpListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<BhNode> nodes = findNext ? bpListView.getItems() : bpListView.getItems().reversed();
    for (int i = 0; i < nodes.size(); ++i) {
      BhNode node = nodes.get((i + startIdx) % nodes.size());
      String itemName = caseSensitive ? node.getAlias() : node.getAlias().toLowerCase();
      if (itemName.contains(word)) {
        return node;
      }
    }
    return null;
  }

  /** コールスタックから {@code pattern} に一致する {@link BhNode} を探す. */
  private BhNode searchCallStackFor(Pattern pattern, boolean findNext) {
    int size = bpListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = bpListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<BhNode> nodes = findNext ? bpListView.getItems() : bpListView.getItems().reversed();
    for (int i = 0; i < nodes.size(); ++i) {
      BhNode node = nodes.get((i + startIdx) % nodes.size());
      if (pattern.matcher(node.getAlias()).find()) {
        return node;
      }
    }
    return null;
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
