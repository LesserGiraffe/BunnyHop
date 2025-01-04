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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.TreeNode;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;

/**
 * BhNode のカテゴリ選択画面のビュー.
 *
 * @author K.Koike
 */
public final class BhNodeCategoryListView {

  private final BhNodeCategoryList model;
  private final TreeView<BhNodeCategory> categoryTree;
  /** BhNode 選択カテゴリと BhNode 選択ビューのマップ. */
  private final Map<BhNodeCategory, BhNodeSelectionView> categoryToSelectionView =
      new LinkedHashMap<>();

  /**
   * カテゴリリストを構築する.
   *
   * @param categoryTree カテゴリリストを追加するコンポーネント
   * @param model カテゴリリストのモデル
   * @throws ViewInitializationException カテゴリリストの構築に失敗した.
   */
  public BhNodeCategoryListView(TreeView<BhNodeCategory> categoryTree, BhNodeCategoryList model)
      throws ViewInitializationException {
    this.categoryTree = categoryTree;
    this.model = model;
    buildCategoryList(model.getRootNode());
    registerPrivateTemplateView();
    for (int i = 0; i < Math.abs(BhConstants.LnF.INITIAL_ZOOM_LEVEL); ++i) {
      model.getAppRoot().getNodeSelectionViewProxy().zoom(BhConstants.LnF.INITIAL_ZOOM_LEVEL > 0);
    }
  }

  /**
   * BhNode のカテゴリビューを構築する.
   *
   * @param root 選択リストのノード
   * @throws ViewInitializationException カテゴリリストの構築に失敗した.
   */
  private void buildCategoryList(TreeNode<String> root) throws ViewInitializationException {
    TreeItem<BhNodeCategory> rootItem = new TreeItem<>(new BhNodeCategory(root.content));
    rootItem.addEventHandler(
        TreeItem.branchCollapsedEvent(), event -> event.getTreeItem().setExpanded(true));
    rootItem.setExpanded(true);
    addChildren(root, rootItem);
    categoryTree.setRoot(rootItem);
    categoryTree.setShowRoot(false);
    categoryTree.setCellFactory(templates -> new BhNodeCategoryView());
  }

  /**
   * ノード選択ビューのリストを取得する.
   *
   * @return ノード選択ビューのリスト
   */
  public List<BhNodeSelectionView> getSelectionViewList() {
    return new ArrayList<>(categoryToSelectionView.values());
  }

  /**
   * テンプレートツリーに子ノードを追加する.<br>.
   *
   * @param parent 追加する子ノード情報を持ったノード
   * @param parentItem 子ノードを追加したいノード
   */
  private void addChildren(TreeNode<String> parent, TreeItem<BhNodeCategory> parentItem)
      throws ViewInitializationException {
    for (TreeNode<String> child : parent.getChildren()) {
      switch (child.content) {
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:
          String cssClass = child.getChildAt(0).content;
          parentItem.getValue().setCssClass(cssClass);
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:
          for (TreeNode<String> id : child.getChildren()) {
            addBhNodeToSelectionView(parentItem.getValue(), BhNodeId.of(id.content));
          }
          break;

        default:
          BhNodeCategory category = new BhNodeCategory(child.content);
          TreeItem<BhNodeCategory> childItem = new TreeItem<>(category);
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
  private void addBhNodeToSelectionView(BhNodeCategory category, BhNodeId bhNodeId)
      throws ViewInitializationException {
    if (!categoryToSelectionView.containsKey(category)) {
      BhNodeSelectionView selectionView = createNodeSelectionView(category);
      categoryToSelectionView.put(category, selectionView);
    }
    UserOperation userOpe = new UserOperation();
    BhNode node = BhService.bhNodeFactory().create(bhNodeId, userOpe);
    NodeMvcBuilder.buildTemplate(node);  //MVC構築
    // ノード選択ビューにテンプレートノードを追加
    model.getAppRoot().getNodeSelectionViewProxy().addNodeTree(
        category.categoryName, node, userOpe);
    // ノード選択ビューに追加してからフック処理を呼ぶ
    CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
        .setForAllNodes(bhNode -> bhNode.getHookAgent().execOnTemplateCreated(userOpe));
    CallbackInvoker.invoke(registry, node);
  }

  /** {@code category} に対応する {@link BhNodeSelectionView} オブジェクトを作成する. */
  private BhNodeSelectionView createNodeSelectionView(BhNodeCategory category)
      throws ViewInitializationException {
    var view = new BhNodeSelectionView(category.categoryName, category.cssClass, this);
    model.getAppRoot().getNodeSelectionViewProxy().addNodeSelectionView(view);
    return view;
  }

  /** ノード固有のノード選択ビューを登録する. */
  private void registerPrivateTemplateView() throws ViewInitializationException {
    var category = new BhNodeCategory(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
    category.setCssClass(BhConstants.Css.CLASS_PRIVATE_NODE_TEMPLATE);
    BhNodeSelectionView selectionView = createNodeSelectionView(category);
    categoryToSelectionView.put(category, selectionView);
  }

  /** TreeView の各セルのモデルクラス. */
  public class BhNodeCategory {
    public final String categoryName;
    private String cssClass = "";

    public BhNodeCategory(String category) {
      this.categoryName = category;
    }

    @Override
    public String toString() {
      return categoryName == null ? "" : categoryName;
    }

    public void setCssClass(String cssClass) {
      this.cssClass = cssClass;
    }

    public String getCssClass() {
      return cssClass;
    }
  }

  /**
   * TreeView の各セルのビュークラス.
   * <p>
   * TreeView のモデルとビューの結びつきは動的に変わる.
   * </p>
   */
  public class BhNodeCategoryView extends TreeCell<BhNodeCategory> {

    BhNodeCategory model;

    /** コンストラクタ. */
    public BhNodeCategoryView() {
      // BhNode のカテゴリクリック時の処理
      setOnMousePressed(event -> {
        BhNodeSelectionViewProxy selectionViewProxy =
            BhNodeCategoryListView.this.model.getAppRoot().getNodeSelectionViewProxy();
        // カテゴリ名の無い TreeCell がクリックされた場合と表示済みカテゴリを再度クリックした場合はそれを隠す
        if (isEmpty() || selectionViewProxy.isShowed(model.categoryName)) {
          selectionViewProxy.hideAll();
          select(false);
        } else {
          selectionViewProxy.show(model.categoryName);
          select(true);
        }
      });
    }

    /** TreeItemの選択状態を解除する. */
    public void select(boolean select) {
      pseudoClassStateChanged(PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_SELECTED), select);
    }

    @Override
    protected void updateItem(BhNodeCategory category, boolean empty) {
      model = category;
      super.updateItem(category, empty);
      if (empty || category == null) {
        select(false);
        getStyleClass().clear();
        getStyleClass().add("tree-cell");
        pseudoClassStateChanged(PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_EMPTY), true);
        setText(null);
      } else {
        getStyleClass().add(model.getCssClass());
        pseudoClassStateChanged(PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_EMPTY), false);
        setText(category.toString());
      }
    }
  }
}
