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
import java.util.SequencedCollection;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
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

  private final VariableInfo varInfo;
  private final SearchBox searchBox;
  private final Debugger debugger;
  private final WorkspaceSet wss;
  private final String viewName;
  private final TreeItem<VariableListItem> rootVarItem;
  private final Map<VariableListItem, Set<VariableListCell>> varItemToVarCells =
      new WeakHashMap<>();
  private final Map<BhNode, Set<VariableListCell>> nodeToVarCells = new WeakHashMap<>();
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
    rootVarItem.setExpanded(false);
    varInfo.getCallbackRegistry().getOnVariablesAdded().add(event -> addVarInfo(event.added()));
    addVarInfo(varInfo.getVariables());
  }

  /** 変数情報をビューに追加する. */
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
        if (item.numValues <= BhSettings.Debug.maxListTreeChildren) {
          item.variable.getNode().ifPresent(
              node -> debugger.requestLocalListVals(node, item.startIdx, item.numValues));
          return;
        }
        for (VariableListItem subItem : subItems) {
          if (subItem.numValues == 1) {
            subItem.variable.getNode().ifPresent(
                node -> debugger.requestLocalListVals(node, subItem.startIdx, subItem.numValues));
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
        items -> new VariableListCell(varItemToVarCells, nodeToVarCells));
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
    ViewUtil.runSafe(() -> {
      variableTreeView.setRoot(null);
      synchronized (varItemToVarCells) {
        varItemToVarCells.clear();
      }
      synchronized (nodeToVarCells) {
        nodeToVarCells.clear();
      }
    });
  }

  /** {@code varItem} に対応する {@link VariableListCell} の内容を変更する. */
  private void updateCellValues(VariableListItem varItem) {
    if (isDiscarded) {
      return;
    }
    synchronized (varItemToVarCells) {
      if (varItemToVarCells.containsKey(varItem)) {
        varItemToVarCells.get(varItem).forEach(VariableListCell::updateValue);
      }
    }
  }

  /** {@code node} に対応する {@link VariableListCell} の装飾を変更する. */
  private void updateCellDecoration(BhNode node) {
    if (isDiscarded) {
      return;
    }
    synchronized (nodeToVarCells) {
      if (nodeToVarCells.containsKey(node)) {
        nodeToVarCells.get(node).forEach(cell -> cell.decorateText(node.isSelected()));
      }
    }
  }
}
