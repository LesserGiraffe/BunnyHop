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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Consumer;
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
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.VariableListItem;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.ListVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.ScalarVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.Variable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.debugger.VariableListCell;
import org.apache.commons.lang3.function.TriConsumer;

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
  private final TreeItem<VariableListItem> rootVarItem;
  private final DataStore dataStore;
  private boolean isDiscarded = false;
  private final Consumer<WorkspaceSet.NodeSelectionEvent> onNodeSelStateChanged =
      event -> updateCellDecoration(event.node());

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
    this.rootVarItem = new TreeItem<>();
    this.dataStore = new DataStore();
    rootVarItem.setExpanded(false);
    varInfo.getCallbackRegistry().getOnVariablesAdded().add(event -> addVarInfo(event.added()));
    varInfo.getCallbackRegistry().getOnVariablesRemoved()
        .add(event -> removeVarInfo(event.removed()));
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
          list.getNode().ifPresent(node -> debugger.requestLocalListVals(node, 0, 1));
        }
      }
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
  }

  private TreeItem<VariableListItem> createTreeItem(ScalarVariable scalar) {
    var item = new VariableListItem(scalar);
    item.getCallbackRegistry().getOnValueChanged().add(event -> updateCellValues(event.item()));
    return createTreeItem(item);
  }

  private TreeItem<VariableListItem> createTreeItem(ListVariable list) {
    var item = new VariableListItem(list, 0, list.length - 1);
    item.getCallbackRegistry().getOnValueChanged().add(event -> updateCellValues(event.item()));
    return createTreeItem(item);
  }

  private TreeItem<VariableListItem> createTreeItem(VariableListItem item) {
    return new TreeItem<>(item) {

      private final boolean isLeaf;
      private boolean isFirstTimeChildren = true;

      {
        isLeaf = item.numValues <= 1;
      }

      @Override
      public ObservableList<TreeItem<VariableListItem>> getChildren() {
        if (!isDiscarded && isFirstTimeChildren) {
          isFirstTimeChildren = false;
          List<VariableListItem> subItems = item.createSubItems();
          setEventHandlers(subItems);
          requestListValues(subItems);
          var children = subItems.stream().map(subItem -> createTreeItem(subItem)).toList();
          super.getChildren().setAll(children);
        }
        return super.getChildren();
      }

      private void setEventHandlers(List<VariableListItem> items) {
        for (VariableListItem item : items) {
          item.getCallbackRegistry().getOnValueChanged()
              .add(event -> updateCellValues(event.item()));
        }
      }

      /** リスト変数の値の取得をデバッガに命令する. */
      private void requestListValues(List<VariableListItem> subItems) {
        TriConsumer<BhNode, Long, Long> requestListVals = varInfo.getStackFrameId().isPresent()
            ? debugger::requestLocalListVals : debugger::requestGlobalListVals;

        if (item.numValues <= BhSettings.Debug.maxListTreeChildren) {
          item.variable.getNode().ifPresent(
              node -> requestListVals.accept(node, item.startIdx, item.numValues));
          return;
        }
        for (VariableListItem subItem : subItems) {
          if (subItem.numValues == 1) {
            subItem.variable.getNode().ifPresent(
                node -> requestListVals.accept(node, subItem.startIdx, subItem.numValues));
          }
        }
      }

      @Override
      public boolean isLeaf() {
        return isLeaf;
      }
    };
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    viViewName.setText(viewName);
    variableTreeView.setShowRoot(false);
    variableTreeView.setRoot(rootVarItem);
    variableTreeView.setCellFactory(
        items -> new VariableListCell(dataStore.varItemToVarCells, dataStore.nodeToVarCells));
    viReloadBtn.setOnAction(event -> reloadVarInfo());
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged().add(onNodeSelStateChanged);
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
    synchronized (dataStore) {
      dataStore.clear();
    }
    ViewUtil.runSafe(() -> variableTreeView.setRoot(null));
  }

  /** {@code varItem} に対応する {@link VariableListCell} の内容を変更する. */
  private void updateCellValues(VariableListItem varItem) {
    if (isDiscarded) {
      return;
    }
    synchronized (dataStore) {
      if (dataStore.varItemToVarCells.containsKey(varItem)) {
        dataStore.varItemToVarCells.get(varItem).forEach(VariableListCell::updateValue);
      }
    }
  }

  /** {@code node} に対応する {@link VariableListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (isDiscarded) {
      return;
    }
    synchronized (dataStore) {
      if (dataStore.nodeToVarCells.containsKey(node)) {
        dataStore.nodeToVarCells.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
      }
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
}
