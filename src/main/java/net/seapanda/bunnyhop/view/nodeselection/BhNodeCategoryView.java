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
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;

/**
 * ノード選択用 TreeView の各セルのビュークラス.
 *
 * <p>TreeView のモデルとビューの結びつきは動的に変わる.
 *
 * @author K.Koike
 */
public class BhNodeCategoryView extends TreeCell<BhNodeCategory> {

  private BhNodeCategory model;

  /** コンストラクタ. */
  public BhNodeCategoryView(BhNodeSelectionViewProxy proxy) {
    selectedProperty().addListener((obs, oldVal, newVal) -> onSelected(newVal));

    addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      String categoryName = (model == null) ? null : model.name;
      boolean isModelSelected = proxy.getCurrentCategoryName()
          .map(category -> category.equals(categoryName))
          .orElse(false);
      if (isModelSelected) {
        proxy.hideCurrentView();
        event.consume();
      }
    });
  }

  private void onSelected(boolean isSelected) {
    pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_SELECTED), isSelected);
  }

  @Override
  protected void updateItem(BhNodeCategory category, boolean empty) {
    super.updateItem(category, empty);
    if (model != null) {
      getStyleClass().removeLast();
    }
    if (empty || category == null) {
      pseudoClassStateChanged(PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_EMPTY), true);
      setText(null);
    } else {
      getStyleClass().add(category.getCssClass());
      pseudoClassStateChanged(PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_EMPTY), false);
      setText(category.toString());
    }
    model = category;
  }
}
