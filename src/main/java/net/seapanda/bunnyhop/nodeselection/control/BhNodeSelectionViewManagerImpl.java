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
import java.util.Optional;
import java.util.function.Consumer;
import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.utility.reference.WritableInteger;

/**
 * ノードの選択ビューを操作するクラス.
 *
 * @author K.Koike
 */
public class BhNodeSelectionViewManagerImpl implements BhNodeSelectionViewManager {
  
  private final Map<String, BhNodeSelectionView> categoryNameToSelectionView = new HashMap<>();
  private final Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree;
  private final Consumer<BhNodeSelectionView> fnRemoveNodeSelectionViewFromGuiTree;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  private final Consumer<BhNodeSelectionView.NodeRemovedEvent> onNodeRemoved =
      event -> hideNodeSelectionViewIfEmpty(event.nodeSelectionView());
  private final EventHandler<ScrollEvent> onScrolled = this::onScrolled;
  private final WritableInteger currentNodeSelectionViewZoomLevel;
  /** 現在表示している BhNode のカテゴリ. */
  private String currentCategory = null;
  /** 全てのノード選択ビューのズームレベル. */
  private int zoomLevel;


  /**
   * コンストラクタ.
   *
   * @param fnAddNodeSelectionViewToGuiTree ノード選択ビューを GUI ツリーに追加する関数オブジェクト
   * @param fnRemoveNodeSelectionViewFromGuiTree ノード選択ビューを GUI ツリーから取り除く関数オブジェクト
   */
  public BhNodeSelectionViewManagerImpl(
      Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree,
      Consumer<BhNodeSelectionView> fnRemoveNodeSelectionViewFromGuiTree,
      WritableInteger currentNodeSelectionViewZoomLevel) {
    this.fnAddNodeSelectionViewToGuiTree = fnAddNodeSelectionViewToGuiTree;
    this.fnRemoveNodeSelectionViewFromGuiTree = fnRemoveNodeSelectionViewFromGuiTree;
    this.currentNodeSelectionViewZoomLevel = currentNodeSelectionViewZoomLevel;
    zoomLevel = currentNodeSelectionViewZoomLevel.get();
  }

  @Override
  public void addNodeSelectionView(BhNodeSelectionView view) {
    BhNodeSelectionView oldView = categoryNameToSelectionView.get(view.getCategoryName());
    if (oldView != null) {
      if (oldView == view) {
        return;
      }
      removeNodeSelectionView(oldView);
    }
    view.hide();
    view.zoom(zoomLevel);
    view.getCallbackRegistry().getOnNodeRemoved().add(onNodeRemoved);
    view.getRegion().addEventFilter(ScrollEvent.ANY, onScrolled);
    categoryNameToSelectionView.put(view.getCategoryName(), view);
    fnAddNodeSelectionViewToGuiTree.accept(view);
  }

  @Override
  public void removeNodeSelectionView(BhNodeSelectionView view) {
    if (!categoryNameToSelectionView.containsKey(view.getCategoryName())) {
      return;
    }
    if (view.getCategoryName().equals(currentCategory)) {
      hideCurrentView();
    }
    view.getCallbackRegistry().getOnNodeRemoved().remove(onNodeRemoved);
    view.getRegion().removeEventFilter(ScrollEvent.ANY, onScrolled);
    categoryNameToSelectionView.remove(view.getCategoryName());
    fnRemoveNodeSelectionViewFromGuiTree.accept(view);
  }

  private void onScrolled(ScrollEvent event) {
    if (event.isControlDown() && event.getDeltaY() != 0) {
      event.consume();
      boolean zoomIn = event.getDeltaY() >= 0;
      zoom(zoomIn);
    }
  }

  /**
   * {@code view} が表示されていてかつ, ノードビューを 1 つも持たない場合, {@code view} を非表示にして,
   * ノードカテゴリ変更時のイベントハンドラを呼ぶ.
   */
  private void hideNodeSelectionViewIfEmpty(BhNodeSelectionView view) {
    if (view.getNumNodeViewTrees() == 0 && view.isShowed()) {
      view.hide();
      String oldCategory = currentCategory;
      currentCategory = null;
      cbRegistry.onCurrentCategoryChangedInvoker
          .invoke(new CurrentCategoryChangedEvent(this, oldCategory, null));
    }
  }

  @Override
  public Optional<BhNodeSelectionView> getNodeSelectionView(String categoryName) {
    return Optional.ofNullable(categoryNameToSelectionView.get(categoryName));
  }

  @Override
  public Optional<BhNodeSelectionView> getCurrentNodeSelectionView() {
    return Optional.ofNullable(categoryNameToSelectionView.get(currentCategory));
  }

  @Override
  public void zoom(boolean zoomIn) {
    int inc = zoomIn ? 1 : -1;
    zoom(zoomLevel + inc);
  }

  @Override
  public void zoom(int level) {
    zoomLevel = Math.clamp(level, BhConstants.Ui.MIN_ZOOM_LEVEL, BhConstants.Ui.MAX_ZOOM_LEVEL);
    currentNodeSelectionViewZoomLevel.set(zoomLevel);
    categoryNameToSelectionView.values().forEach(view -> view.zoom(zoomLevel));
  }

  @Override
  public void show(String categoryName) {
    BhNodeSelectionView view = categoryNameToSelectionView.get(categoryName);
    if (view == null) {
      return;
    }
    Optional.ofNullable(currentCategory)
        .map(categoryNameToSelectionView::get)
        .ifPresent(BhNodeSelectionView::hide);
    view.show();

    String oldCategory = currentCategory;
    currentCategory = categoryName;
    cbRegistry.onCurrentCategoryChangedInvoker
        .invoke(new CurrentCategoryChangedEvent(this, oldCategory, categoryName));
  }

  @Override
  public void hideCurrentView() {
    if (currentCategory == null) {
      return;
    }
    BhNodeSelectionView view = categoryNameToSelectionView.get(currentCategory);
    view.hide();
    String oldCategory = currentCategory;
    currentCategory = null;
    cbRegistry.onCurrentCategoryChangedInvoker
        .invoke(new CurrentCategoryChangedEvent(this, oldCategory, null));
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link BhNodeSelectionViewManager} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  static class CallbackRegistryImpl implements CallbackRegistry {

    /** 選択中の BhNode のカテゴリが変更されたときのイベントハンドラを管理するオブジェクト. */
    final ConsumerInvoker<CurrentCategoryChangedEvent> onCurrentCategoryChangedInvoker =
        new SimpleConsumerInvoker<>();

    @Override
    public ConsumerInvoker<CurrentCategoryChangedEvent>.Registry getOnCurrentCategoryChanged() {
      return onCurrentCategoryChangedInvoker.getRegistry();
    }
  }
}
