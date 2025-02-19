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

package net.seapanda.bunnyhop.control.nodeselection;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryTree;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeShowcaseBuilder;

/**
 * BhNode のカテゴリ選択画面のコントローラ.
 *
 * @author K.Koike
 */
public class BhNodeCategoryListController {

  @FXML private ScrollPane nodeCategoryListViewBase;
  @FXML private TreeView<BhNodeCategory> categoryTree;

  /**
   * コントローラとビューの初期化を行う.
   *
   * @param builder ノード選択ビューを構築するためのオブジェクト.
   * @param categories カテゴリリストの情報を格納したオブジェクト
   */
  public boolean initialize(
      BhNodeShowcaseBuilder builder, 
      BhNodeCategoryTree categories) {
    try {
      categoryTree.setRoot(builder.buildFrom(categories.getRoot()));  
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
      return false;
    }
    categoryTree.setShowRoot(false);
    categoryTree.setCellFactory(templates -> builder.createBhNodeCategoryView());
    nodeCategoryListViewBase.setMinWidth(Region.USE_PREF_SIZE);
    nodeCategoryListViewBase.widthProperty().addListener(
        (obs, oldVal, newVal) -> nodeCategoryListViewBase.setMinWidth(Rem.VAL * 3));
    return true;
  }
}
