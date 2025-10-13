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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.input.ScrollEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.nodeselection.view.FxmlBhNodeSelectionView;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.function.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.Workspace;

/**
 * ノードの選択ビューを操作するクラス.
 *
 * @author K.Koike
 */
public class BhNodeSelectionViewProxyImpl implements BhNodeSelectionViewProxy {
  
  private final Map<String, BhNodeSelectionView> categoryNameToSelectionView = new HashMap<>();
  private final Map<String, Workspace> categoryNameToWorkspace = new HashMap<>();
  private final Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  private final Path nodeSelectionViewFilePath;
  /** 現在表示している BhNode のカテゴリ. */
  private String currentCategory = null;


  /**
   * コンストラクタ.
   *
   * @param fnAddNodeSelectionViewToGuiTree ノード選択ビューを GUI ツリーに追加する関数オブジェクト
   */
  public BhNodeSelectionViewProxyImpl(
      Path nodeSelectionViewFilePath,
      Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree) {
    this.nodeSelectionViewFilePath = nodeSelectionViewFilePath;
    this.fnAddNodeSelectionViewToGuiTree = fnAddNodeSelectionViewToGuiTree;
  }

  @Override
  public void addNodeSelectionView(String name, String cssClass) throws ViewConstructionException {
    var view = new FxmlBhNodeSelectionView(nodeSelectionViewFilePath, name, cssClass);
    categoryNameToSelectionView.putIfAbsent(name, view);
    Workspace workspace = categoryNameToWorkspace.computeIfAbsent(name, Workspace::new);
    Workspace.CallbackRegistry registry = workspace.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> addNodeView(event.node(), view));
    registry.getOnNodeRemoved().add(event -> removeNodeView(event.node(), view));
    registry.getOnRootNodeAdded().add(event -> specifyNodeViewAsRoot(event.node(), view));
    registry.getOnRootNodeRemoved().add(event -> specifyNodeViewAsNotRoot(event.node(), view));
    view.getRegion().addEventFilter(ScrollEvent.ANY, this::onScrolled);
    fnAddNodeSelectionViewToGuiTree.accept(view);
  }


  private void onScrolled(ScrollEvent event) {
    if (event.isControlDown() && event.getDeltaY() != 0) {
      event.consume();
      boolean zoomIn = event.getDeltaY() >= 0;
      zoom(zoomIn);
    }
  }

  /** {@code node} のノードビューをノード選択リストに追加する. */
  private void addNodeView(BhNode node, BhNodeSelectionView view) {
    node.getView().ifPresent(view::addNodeViewTree);
  }

  /** {@code node} のノードビューをノード選択リストから削除する. */
  private void removeNodeView(BhNode node, BhNodeSelectionView view) {
    node.getView().ifPresent(view::removeNodeViewTree);
    if (view.getNumNodeViewTrees() == 0 && view.isShowed()) {
      view.hide();
      String oldCategory = currentCategory;
      currentCategory = null;
      cbRegistry.onCurrentCategoryChangedInvoker
          .invoke(new CurrentCategoryChangedEvent(this, oldCategory, null));
    }
  }

  /** {@code node} をルートノードとしてノード選択ビューに設定する. */
  private void specifyNodeViewAsRoot(BhNode node, BhNodeSelectionView view) {
    node.getView().ifPresent(view::specifyNodeViewAsRoot);
  }

  /** {@code node} を非ルートノードとしてノード選択ビューに設定する. */
  private void specifyNodeViewAsNotRoot(BhNode node, BhNodeSelectionView view) {
    node.getView().ifPresent(view::specifyNodeViewAsNotRoot);
  }

  @Override
  public void addNodeTree(String categoryName, BhNode root, UserOperation userOpe) {
    Workspace ws = categoryNameToWorkspace.get(categoryName);
    if (ws != null) {
      ws.addNodeTree(root, userOpe);
    }
  }

  @Override
  public void removeNodeTree(BhNode root, UserOperation userOpe) {
    Workspace ws = root.getWorkspace();
    if (ws != null) {
      ws.removeNodeTree(root, userOpe);
    }
  }

  @Override
  public SequencedSet<BhNode> getNodeTrees(String categoryName) {
    BhNodeSelectionView view = categoryNameToSelectionView.get(categoryName);
    if (view == null) {
      return new LinkedHashSet<>();
    }
    return view.getNodeViewList().stream()
        .filter(nodeView -> nodeView.getModel().isPresent())
        .map(nodeView -> nodeView.getModel().get())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public void zoom(boolean zoomIn) {
    categoryNameToSelectionView.values().forEach(view -> view.zoom(zoomIn));
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
  public Optional<String> getCurrentCategoryName() {
    return Optional.ofNullable(currentCategory);
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link BhNodeSelectionViewProxy} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  class CallbackRegistryImpl implements CallbackRegistry {

    /** 選択中の BhNode のカテゴリが変更されたときのイベントハンドラを管理するオブジェクト. */
    ConsumerInvoker<CurrentCategoryChangedEvent> onCurrentCategoryChangedInvoker =
        new SimpleConsumerInvoker<>();

    @Override
    public ConsumerInvoker<CurrentCategoryChangedEvent>.Registry getOnCurrentCategoryChanged() {
      return onCurrentCategoryChangedInvoker.getRegistry();
    }
  }
}
