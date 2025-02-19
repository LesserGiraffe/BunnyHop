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

import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;

/**
 * ノード選択用 TreeView の各セルのビュークラス.
 * <p>
 * TreeView のモデルとビューの結びつきは動的に変わる.
 * </p>
 */
public class BhNodeCategoryView extends TreeCell<BhNodeCategory> {

  private BhNodeCategory model;

  /** コンストラクタ. */
  public BhNodeCategoryView(BhNodeSelectionViewProxy proxy) {
    // BhNode のカテゴリクリック時の処理
    setOnMousePressed(event -> {
      // カテゴリ名の無い TreeCell がクリックされた場合と表示済みカテゴリを再度クリックした場合はそれを隠す
      if (isEmpty() || proxy.isShowed(model.categoryName)) {
        proxy.hideAll();
        select(false);
      } else {
        proxy.show(model.categoryName);
        select(true);
      }
    });
  }

  /** TreeItemの選択状態を解除する. */
  private void select(boolean select) {
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
