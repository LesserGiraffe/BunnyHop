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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.common.TreeNode;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.modelprocessor.CallbackInvoker;
import net.seapanda.bunnyhop.modelprocessor.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextPrompter;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.ViewInitializationException;

/**
 * BhNode のカテゴリ選択画面のビュー.
 *
 * @author K.Koike
 */
public final class BhNodeCategoryListView {

  private final TreeView<BhNodeCategory> categoryTree;
  /** BhNode 選択カテゴリと BhNode 選択ビューのマップ. */
  private final Map<BhNodeCategory, BhNodeSelectionView> categoryToSselectionView =
      new HashMap<>();
  /** BhNode 選択ビューのリスト. */
  private final List<BhNodeSelectionView> selectionViewList = new ArrayList<>();
  private final List<BhNodeCategory> categoryList = new ArrayList<>();

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
    buildCategoryList(model.getRootNode());
    registerPrivateTemplateView();
    for (int i = 0; i < Math.abs(BhConstants.LnF.INITIAL_ZOOM_LEVEL); ++i) {
      BhNodeSelectionService.INSTANCE.zoomAll(BhConstants.LnF.INITIAL_ZOOM_LEVEL > 0);
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
    return new ArrayList<>(selectionViewList);
  }

  /**
   * テンプレートツリーに子ノードを追加する.<br>.
   *
   * @param parent 追加する子ノード情報を持ったノード
   * @param parentItem 子ノードを追加したいノード
   */
  private void addChildren(TreeNode<String> parent, TreeItem<BhNodeCategory> parentItem)
      throws ViewInitializationException {
    for (TreeNode<String> child : parent.children) {
      switch (child.content) {
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:
          String cssClass = child.children.get(0).content;
          parentItem.getValue().setCssClass(cssClass);
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:
          for (TreeNode<String> id : child.children) {
            addBhNodeToSelectionView(parentItem.getValue(), BhNodeId.of(id.content));
          }
          break;

        default:
          BhNodeCategory category = new BhNodeCategory(child.content);
          categoryList.add(category);
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
    if (!categoryToSselectionView.containsKey(category)) {
      var selectionView = new BhNodeSelectionView(category.categoryName, category.cssClass, this);
      BhNodeSelectionService.INSTANCE.registerView(selectionView);
      selectionView.setVisible(false);
      categoryToSselectionView.put(category, selectionView);
      selectionViewList.add(selectionView);
    }
    UserOperation userOpe = new UserOperation();
    BhNode node = BhNodeFactory.INSTANCE.create(bhNodeId, userOpe);
    CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
        .setForAllNodes(bhNode -> bhNode.getEventAgent().execOnTemplateCreated(userOpe));
    CallbackInvoker.invoke(registry, node);
    NodeMvcBuilder.buildTemplate(node);  //MVC構築
    TextPrompter.prompt(node);
    node.getEventAgent().execOnTemplateCreated(userOpe);
    //BhNode テンプレートリストパネルにBhNodeテンプレートを追加
    BhNodeSelectionService.INSTANCE.addTemplateNode(category.categoryName, node, userOpe);
  }

  /** ノード固有のノード選択ビューを登録する. */
  private void registerPrivateTemplateView()
      throws ViewInitializationException {
    var selectionView = new BhNodeSelectionView(
        BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE,
        BhConstants.Css.CLASS_PRIVATE_NODE_TEMPLATE,
        this);
    BhNodeSelectionService.INSTANCE.registerView(selectionView);
    selectionView.setVisible(false);
    selectionViewList.add(selectionView);
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
      setOnMousePressed(evenet -> {
        // カテゴリ名の無い TreeCell がクリックされたときと表示済みカテゴリを再度クリックした場合はそれを隠す
        if (isEmpty() || BhNodeSelectionService.INSTANCE.isShowed(model.categoryName)) {
          BhNodeSelectionService.INSTANCE.hideAll();
          select(false);
        } else {
          BhNodeSelectionService.INSTANCE.show(model.categoryName);
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
