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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.input.ScrollEvent;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

/**
 * ノードの選択ビューを操作するクラス.
 *
 * @author K.Koike
 */
public class BhNodeSelectionViewProxyImpl implements BhNodeSelectionViewProxy {
  
  private final Map<String, BhNodeSelectionView> categoryNameToSelectionView = new HashMap<>();
  private final Map<String, Workspace> categoryNameToWorkspace = new HashMap<>();
  private final Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree;

  /**
   * コンストラクタ.
   *
   * @param fnAddNodeSelectionViewToGuiTree ノード選択ビューを GUI ツリーに追加する関数オブジェクト
   */
  public BhNodeSelectionViewProxyImpl(
      Consumer<BhNodeSelectionView> fnAddNodeSelectionViewToGuiTree) {
    this.fnAddNodeSelectionViewToGuiTree = fnAddNodeSelectionViewToGuiTree;
  }

  @Override
  public void addNodeSelectionView(BhNodeSelectionView view) {
    String categoryName = view.getCategoryName();
    categoryNameToSelectionView.putIfAbsent(categoryName, view);
    Workspace workspace = categoryNameToWorkspace.computeIfAbsent(categoryName, Workspace::new);
    Workspace.CallbackRegistry registry = workspace.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> addNodeView(event.node(), view));
    registry.getOnNodeRemoved().add(event -> removeNodeView(event.node(), view));
    registry.getOnRootNodeAdded().add(event -> speficyNodeViewAsRoot(event.node(), view));
    registry.getOnRootNodeRemoved().add(event -> speficyNodeViewAsNotRoot(event.node(), view));
    view.getRegion().addEventFilter(ScrollEvent.ANY, event -> onScrolled(event));
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
    if (view.getNumNodeViewTrees() == 0) {
      view.hide();
    }
  }

  /** {@code node} をルートノードとしてノード選択ビューに設定する. */
  private void speficyNodeViewAsRoot(BhNode node, BhNodeSelectionView view) {
    node.getView().ifPresent(view::specifyNodeViewAsRoot);
  }

  /** {@code node} を非ルートノードとしてノード選択ビューに設定する. */
  private void speficyNodeViewAsNotRoot(BhNode node, BhNodeSelectionView view) {
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
    BhNodeSelectionView selectionView = categoryNameToSelectionView.get(categoryName);
    if (selectionView == null) {
      return new LinkedHashSet<>();
    }
    return selectionView.getNodeViewList().stream()
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
    hideAll();
    view.show();
  }

  @Override
  public void hideAll() {
    for (BhNodeSelectionView view : categoryNameToSelectionView.values()) {
      if (view.isShowed()) {
        view.hide();
      }
    }
  }

  @Override
  public boolean isShowed(String categoryName) {
    BhNodeSelectionView view = categoryNameToSelectionView.get(categoryName);
    if (view == null) {
      return false;
    }
    return view.isShowed();
  }

  @Override
  public boolean isAnyShowed() {
    return categoryNameToSelectionView.values().stream()
        .anyMatch(view -> view.getRegion().visibleProperty().get());
  }
}
