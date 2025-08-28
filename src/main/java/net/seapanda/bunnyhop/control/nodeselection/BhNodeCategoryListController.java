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

import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeCategoryProvider;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeCategoryView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

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
  private BhNodeSelectionViewProxy proxy;

  /** このコントローラを初期化する. */
  public boolean initialize(BhNodeCategoryProvider provider, BhNodeSelectionViewProxy proxy) {
    this.proxy = proxy;
    TreeItem<BhNodeCategory> root = provider.getCategoryRoot();
    collectCategories(root);
    categoryTree.setRoot(root);
    categoryTree.setShowRoot(false);
    categoryTree.setCellFactory(templates -> new BhNodeCategoryView(proxy));
    setEventHandlers();
    return true;
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    categoryTree.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldVal, newVal) -> {
            String categoryName = (newVal == null || newVal.getValue() == null)
                ? null : newVal.getValue().name;
            proxy.show(categoryName);
        });
    nodeCategoryListViewBase.setMinWidth(Region.USE_PREF_SIZE);
    nodeCategoryListViewBase.widthProperty().addListener(
        (obs, oldVal, newVal) -> nodeCategoryListViewBase.setMinWidth(Rem.VAL * 3));
    proxy.getCallbackRegistry().getOnCurrentCategoryChanged()
        .add(event -> selectViewItem(event.newVal()));
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

  private void collectCategories(TreeItem<BhNodeCategory> item) {
    nameToCategory.put(item.getValue().name, item);
    item.getChildren().forEach(this::collectCategories);
  }
}
