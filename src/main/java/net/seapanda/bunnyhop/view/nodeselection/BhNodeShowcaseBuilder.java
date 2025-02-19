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

import javafx.scene.control.TreeItem;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.utility.TreeNode;
import net.seapanda.bunnyhop.view.ViewConstructionException;

/**
 * ノードカテゴリビューとノード選択ビューを構築するメソッドを規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeShowcaseBuilder {
  
  /**
   * カテゴリリストと各カテゴリに対応するノード選択ビューを構築する.
   *
   * @param categoryRoot カテゴリリストの情報を格納した木構造のルート要素
   * @return ノードのカテゴリ一覧に対応した {@link TreeItem} オブジェクト
   * @throws ViewConstructionException ビューの構築に失敗した場合.
   */
  TreeItem<BhNodeCategory> buildFrom(TreeNode<String> categoryRoot)
      throws ViewConstructionException;

  /** ノードカテゴリ選択ビューの各セルのビューを作成する. */
  BhNodeCategoryView createBhNodeCategoryView();
}
