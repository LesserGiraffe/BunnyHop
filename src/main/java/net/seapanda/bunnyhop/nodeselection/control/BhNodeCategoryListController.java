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

package net.seapanda.bunnyhop.nodeselection.control;

import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.factory.MvcType;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.nodeselection.model.BhNodeCategory;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeCategoryView;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewFactory;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.Rem;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.reference.ReadOnlyInteger;

/**
 * BhNode のカテゴリ選択画面のコントローラ.
 *
 * @author K.Koike
 */
public class BhNodeCategoryListController {

  @FXML private ScrollPane nodeCategoryListViewBase;
  @FXML private TreeView<BhNodeCategory> categoryTree;

  /** カテゴリ名と対応する {@link TreeItem} のマップ. */
  private final Map<String, TreeItem<BhNodeCategory>> nameToCategory = new HashMap<>();
  private final TreeItem<BhNodeCategory> root;
  private final BhNodeSelectionViewManager nodeSelViewManager;
  private final BhNodeSelectionViewFactory nodeSelViewFactory;
  private final BhNodeFactory nodeFactory;
  private final ReadOnlyInteger currentNodeSelectionViewZoomLevel;

  /**
   * コンストラクタ.
   *
   * @param root BhNode の一覧を格納した木のルート要素
   * @param nodeSelViewManager ノード選択ビューを操作するためのオブジェクト
   * @param nodeSelViewFactory ノード選択ビューを作成するためのファクトリ
   * @param nodeFactory ノード選択ビューに表示するテンプレートノードを作成するためのファクトリ
   * @param currentNodeSelectionViewZoomLevel ノード選択ビューの現在のズームレベル
   */
  public BhNodeCategoryListController(
      TreeItem<BhNodeCategory> root,
      BhNodeSelectionViewManager nodeSelViewManager,
      BhNodeSelectionViewFactory nodeSelViewFactory,
      BhNodeFactory nodeFactory,
      ReadOnlyInteger currentNodeSelectionViewZoomLevel) {
    this.root = root;
    this.nodeSelViewManager = nodeSelViewManager;
    this.nodeSelViewFactory = nodeSelViewFactory;
    this.nodeFactory = nodeFactory;
    this.currentNodeSelectionViewZoomLevel = currentNodeSelectionViewZoomLevel;
    collectCategories(root);
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() throws ViewConstructionException {
    categoryTree.setRoot(root);
    categoryTree.setShowRoot(false);
    categoryTree.setCellFactory(templates -> new BhNodeCategoryView(nodeSelViewManager));
    nodeCategoryListViewBase.setMinWidth(Region.USE_PREF_SIZE);
    setEventHandlers();
    buildNodeSelViews();
    nodeSelViewManager.zoom(currentNodeSelectionViewZoomLevel.get());
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    nodeSelViewManager.getCallbackRegistry().getOnCurrentCategoryChanged()
        .add(event -> selectViewItem(event.newVal()));

    categoryTree.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> {
            String categoryName = (newVal == null || newVal.getValue() == null)
                ? null : newVal.getValue().name;
            nodeSelViewManager.show(categoryName);
        });

    nodeCategoryListViewBase.widthProperty().addListener(
        (obs, oldVal, newVal) -> nodeCategoryListViewBase.setMinWidth(Rem.VAL * 3));

    root.addEventHandler(
        TreeItem.branchCollapsedEvent(),
        event -> event.getTreeItem().setExpanded(true));
  }

  /** {@code categoryName} に対応する {@link #categoryTree} のアイテムを選択する. */
  private void selectViewItem(String categoryName) {
    if (categoryName == null) {
      categoryTree.getSelectionModel().clearSelection();
      return;
    }
    TreeItem<BhNodeCategory> category = nameToCategory.get(categoryName);
    if (category == null) {
      categoryTree.getSelectionModel().clearSelection();
      return;
    }
    categoryTree.getSelectionModel().select(category);
  }

  /** {@code item} 以下のカテゴリを {@link #nameToCategory} に格納する. */
  private void collectCategories(TreeItem<BhNodeCategory> item) {
    item.setExpanded(true);
    nameToCategory.put(item.getValue().name, item);
    item.getChildren().forEach(this::collectCategories);
  }

  /** ノード選択ビューを作成する. */
  private void buildNodeSelViews() throws ViewConstructionException {
    for (TreeItem<BhNodeCategory> category : nameToCategory.values()) {
      BhNodeSelectionView view = nodeSelViewFactory.create(
          category.getValue().name, category.getValue().getCssClass());
      nodeSelViewManager.addNodeSelectionView(view);
      addBhNodes(category.getValue());
    }
  }

  /**
   * {@code category} に属する {@link BhNode} を作成してノード選択ビューに追加する.
   *
   * @param category このカテゴリのノードを作成する
   */
  private void addBhNodes(BhNodeCategory category) {
    UserOperation userOpe = new UserOperation();
    for (BhNodeId id : category.getNodeIds()) {
      BhNode node = nodeFactory.create(id, MvcType.TEMPLATE, userOpe);
      // ノード選択ビューにテンプレートノードを追加
      nodeSelViewManager.getNodeSelectionView(category.name)
          .ifPresent(view -> view.getWorkspace().addNodeTree(node, userOpe));
      // ノード選択ビューに追加してからイベントハンドラを呼ぶ
      CallbackInvoker.CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
          .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
      CallbackInvoker.invoke(registry, node);
    }
  }
}
