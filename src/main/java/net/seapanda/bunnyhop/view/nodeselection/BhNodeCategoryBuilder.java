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

package net.seapanda.bunnyhop.view.nodeselection;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.control.TreeItem;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.TreeNode;
import net.seapanda.bunnyhop.view.ViewConstructionException;

/**
 * ノードカテゴリ一覧とノード選択ビューを構築する機能を提供するクラス.
 *
 * @author K.Koike
 */
public final class BhNodeCategoryBuilder implements BhNodeCategoryProvider {

  private final BhNodeFactory factory;
  private final BhNodeSelectionViewProxy nodeSelectionViewProxy;
  private final Path filePath;
  private final TreeNode<String> categoryRoot;

  /** BhNode のカテゴリ名一覧. */
  private final Set<String> categories = new HashSet<>();
  /** BhNode のカテゴリ一覧を構成する木のルート要素. */
  private TreeItem<BhNodeCategory> rootItem;

  /**
   * コンストラクタ.
   *
   * @param factory ノードの生成に関連する処理を行うオブジェクト
   * @param proxy ノード選択ビューのプロキシオブジェクト
   * @param nodeSelectionViewFilePath ノード選択ビューが定義されたファイルのパス
   * @param categoryRoot BhNode のカテゴリ情報を格納した木のルート要素
   */
  public BhNodeCategoryBuilder(
      BhNodeFactory factory,
      BhNodeSelectionViewProxy proxy,
      Path nodeSelectionViewFilePath,
      TreeNode<String> categoryRoot) {
    this.factory = factory;
    this.nodeSelectionViewProxy = proxy;
    this.filePath = nodeSelectionViewFilePath;
    this.categoryRoot = categoryRoot;
  }

  private TreeItem<BhNodeCategory> buildSelection(TreeNode<String> categoryRoot)
      throws ViewConstructionException {
    TreeItem<BhNodeCategory> rootItem = new TreeItem<>(new BhNodeCategory(categoryRoot.content));
    rootItem.addEventHandler(
        TreeItem.branchCollapsedEvent(), event -> event.getTreeItem().setExpanded(true));
    rootItem.setExpanded(true);
    addChildren(categoryRoot, rootItem);
    registerPrivateTemplateView();
    for (int i = 0; i < Math.abs(BhConstants.LnF.INITIAL_ZOOM_LEVEL); ++i) {
      nodeSelectionViewProxy.zoom(BhConstants.LnF.INITIAL_ZOOM_LEVEL > 0);
    }
    return rootItem;
  }

  /**
   * テンプレートツリーに子ノードを追加する.<br>.
   *
   * @param parent 追加する子ノード情報を持ったノード
   * @param parentItem 子ノードを追加したいノード
   */
  private void addChildren(TreeNode<String> parent, TreeItem<BhNodeCategory> parentItem)
      throws ViewConstructionException {
    for (TreeNode<String> child : parent.getChildren()) {
      switch (child.content) {
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:
          String cssClass = child.getChildAt(0).content;
          parentItem.getValue().setCssClass(cssClass);
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:
          for (TreeNode<String> id : child.getChildren()) {
            addBhNodeToNodeSelView(parentItem.getValue(), BhNodeId.of(id.content));
          }
          break;

        default:
          TreeItem<BhNodeCategory> childItem = new TreeItem<>(new BhNodeCategory(child.content));
          parentItem.getChildren().add(childItem);
          childItem.setExpanded(true);
          addChildren(child, childItem);
          break;
      }
    }
  }

  /**
   * ノード選択ビューにノードを追加する.
   *
   * @param category ノード選択ビューのカテゴリ
   * @param bhNodeId 追加するノードのID
   */
  private void addBhNodeToNodeSelView(BhNodeCategory category, BhNodeId bhNodeId)
      throws ViewConstructionException {
    if (!categories.contains(category.name)) {
      addNodeSelectionView(category);
      categories.add(category.name);
    }
    UserOperation userOpe = new UserOperation();
    BhNode node = factory.create(bhNodeId, MvcType.TEMPLATE, userOpe);
    // ノード選択ビューにテンプレートノードを追加
    nodeSelectionViewProxy.addNodeTree(category.name, node, userOpe);
    // ノード選択ビューに追加してからイベントハンドラを呼ぶ
    CallbackInvoker.CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
        .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
    CallbackInvoker.invoke(registry, node);
  }

  /**
   * {@code category} に対応する {@link BhNodeSelectionView} オブジェクトを作成し.
   * BhNode 選択ビューに登録する.
   */
  private void addNodeSelectionView(BhNodeCategory category) throws ViewConstructionException {
    var view = new FxmlBhNodeSelectionView(filePath, category.name, category.getCssClass());
    nodeSelectionViewProxy.addNodeSelectionView(view);
  }

  /** コンパニオンノード用の選択ビューを登録する. */
  private void registerPrivateTemplateView() throws ViewConstructionException {
    var category = new BhNodeCategory(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
    category.setCssClass(BhConstants.Css.CLASS_PRIVATE_NODE_TEMPLATE);
    addNodeSelectionView(category);
  }

  @Override
  public TreeItem<BhNodeCategory> getCategoryRoot() {
    if (rootItem == null) {
      try {
        rootItem = buildSelection(categoryRoot);
      } catch (ViewConstructionException e) {
        LogManager.logger().error("Failed to construct BhNode Category Selection View.\n" + e);
        rootItem = new TreeItem<>(new BhNodeCategory(""));
      }
    }
    return rootItem;
  }
}
