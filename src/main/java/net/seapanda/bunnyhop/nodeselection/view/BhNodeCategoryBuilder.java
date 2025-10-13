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

package net.seapanda.bunnyhop.nodeselection.view;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.control.TreeItem;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.nodeselection.model.BhNodeCategory;
import net.seapanda.bunnyhop.utility.TreeNode;

/**
 * ノードカテゴリ一覧を作成するクラス.
 *
 * @author K.Koike
 */
public final class BhNodeCategoryBuilder {

  /** BhNode のカテゴリ名一覧. */
  private final Set<String> categories = new HashSet<>();
  /** BhNode のカテゴリ一覧を構成する木のルート要素. */
  private final TreeItem<BhNodeCategory> rootItem;

  /**
   * コンストラクタ.
   *
   * @param categoryRoot BhNode のカテゴリ情報を格納した木のルート要素
   */
  public BhNodeCategoryBuilder(TreeNode<String> categoryRoot) {
    rootItem = new TreeItem<>(new BhNodeCategory(categoryRoot.content));
    addChildren(categoryRoot, rootItem);
  }

  /**
   * テンプレートツリーに子ノードを追加する.<br>.
   *
   * @param parent 追加する子ノード情報を持ったノード
   * @param parentItem 子ノードを追加したいノード
   */
  private void addChildren(TreeNode<String> parent, TreeItem<BhNodeCategory> parentItem) {
    for (TreeNode<String> child : parent.getChildren()) {
      switch (child.content) {
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:
          String cssClass = child.getChildAt(0).content;
          parentItem.getValue().setCssClass(cssClass);
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:
          for (TreeNode<String> id : child.getChildren()) {
            parentItem.getValue().addNodeId(BhNodeId.of(id.content));
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

  /** {@link BhNode} のカテゴリ一覧を格納した木のルート要素を返す. */
  public TreeItem<BhNodeCategory> getCategoryRoot() {
    return rootItem;
  }
}
