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

package net.seapanda.bunnyhop.node.view;

import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.Panes;
import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.Shapes;

import java.util.Optional;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.node.view.BhNodeView.ParentViewChangedEvent;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * ノードビューのツリーに関する操作を提供するクラス.
 *
 * @author K.Koike
 */
class TreeControlImpl implements BhNodeView.TreeControl {

  private final BhNodeViewBase view;
  /** {@link TreeControlImpl#view} を保持するグループ.  このノードビューがルートノードビューの場合は null. */
  protected BhNodeViewGroup parent;

  /** コンストラクタ. */
  TreeControlImpl(BhNodeViewBase view) {
    this.view = view;
  }

  @Override
  public BhNodeViewGroup getParentGroup() {
    return parent;
  }

  @Override
  public ConnectiveNodeView getParentView() {
    if (parent == null) {
      return null;
    }
    return parent.getParentView();
  }

  @Override
  public void replace(BhNodeView newNode) {
    if (parent == null
        || view == newNode
        || !(newNode instanceof BhNodeViewBase)) {
      return;
    }
    parent.replace(view, (BhNodeViewBase) newNode);
    BhNodeViewGroup group = newNode.getTreeControl().getParentGroup();
    if (group != null) {
      group.getGeometry().notifySubTreeSizeChanged();
    }
    WorkspaceView wsView = newNode.getWorkspaceView();
    if (wsView != null) {
      wsView.moveNodeViewToFront(newNode);
    } else {
      // ノード選択ビュー用の処理
      newNode.getGeometry().setTreeZposition(view.getGeometry().getZposition());
    }
  }


  @Override
  public void removeFromTree() {
    // JDK-8205092 対策. view order を使うとノード削除後に NullPointerException が発生するのを防ぐ.
    view.getPanes().root().setMouseTransparent(true);
    removeComponentsFromParent();
    NvbCallbackInvoker.invokeForGroups(
        BhNodeViewGroup::removePseudoViewFromGuiTree,
        view);
  }

  /** このノードの描画物 (ボディや影など) をそれぞれの親要素から取り除く. */
  private void removeComponentsFromParent() {
    Panes panes = view.getPanes();
    Shapes shapes = view.getShapes();
    Parent parent = panes.root().getParent();
    if (parent instanceof Group group) {
      group.getChildren().remove(panes.root());
      group.getChildren().remove(shapes.compileError());
      view.getCallbackRegistry().onParentViewChangedInvoker.invoke(
          new ParentViewChangedEvent(view, group, null));
    } else if (parent instanceof Pane pane) {
      pane.getChildren().remove(panes.root());
      pane.getChildren().remove(shapes.compileError());
      view.getCallbackRegistry().onParentViewChangedInvoker.invoke(
          new ParentViewChangedEvent(view, pane, null));
    }
  }

  @Override
  public void addToTree(Group parent) {
    if (parent == null) {
      return;
    }
    // JDK-8205092 対策
    view.getPanes().root().setMouseTransparent(false);
    addComponentsToParent(parent);
    NvbCallbackInvoker.invokeForGroups(
        group -> group.addPseudoViewToGuiTree(parent),
        view);
  }

  @Override
  public void addToTree(Pane parent) {
    if (parent == null) {
      return;
    }
    // JDK-8205092 対策
    view.getPanes().root().setMouseTransparent(false);
    addComponentsToParent(parent);
    NvbCallbackInvoker.invokeForGroups(
        group -> group.addPseudoViewToGuiTree(parent),
        view);
  }

  /** このノードの描画物 (ボディや影など) を {@code parent} に追加する. */
  private void addComponentsToParent(Group parent) {
    Parent oldParent = view.getPanes().root().getParent();
    if (oldParent != parent) {
      parent.getChildren().add(view.getPanes().root());
      parent.getChildren().add(view.getShapes().compileError());
      view.getCallbackRegistry().onParentViewChangedInvoker.invoke(
          new ParentViewChangedEvent(view, oldParent, parent));
    }
  }

  /** このノードの描画物 (ボディや影など) を {@code parent} に追加する. */
  private void addComponentsToParent(Pane parent) {
    Parent oldParent = view.getPanes().root().getParent();
    if (oldParent != parent) {
      parent.getChildren().add(view.getPanes().root());
      parent.getChildren().add(view.getShapes().compileError());
      view.getCallbackRegistry().onParentViewChangedInvoker.invoke(
          new ParentViewChangedEvent(view, oldParent, parent));
    }
  }

  @Override
  public BhNodeView getRootView() {
    BhNodeView parent = getParentView();
    if (parent == null) {
      return view;
    }
    return parent.getTreeControl().getRootView();
  }

  @Override
  public boolean isRoot() {
    return getParentView() == null;
  }

  @Override
  public boolean isOuter() {
    return Optional.ofNullable(getParentGroup())
        .map(BhNodeViewGroup::isOuter)
        .orElse(false);
  }

  /** 関連するノードビューの親となる {@link BhNodeViewGroup} をセットする. */
  void setParentGroup(BhNodeViewGroup parent) {
    if (this.parent == parent) {
      return;
    }
    BhNodeViewGroup oldParent = this.parent;
    this.parent = parent;
    view.getCallbackRegistry().onParentGroupChangedInvoker.invoke(
        new BhNodeView.ParentGroupChangedEvent(view, oldParent, this.parent));
  }
}
