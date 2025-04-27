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

package net.seapanda.bunnyhop.control.node;


import java.util.Objects;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.proxy.BhNodeViewProxy;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link BhNodeViewProxy} の処理を実装したクラス.
 *
 * @author K.Koike
 */
class BhNodeViewProxyImpl implements BhNodeViewProxy {
  
  private final BhNodeView view;
  private final boolean isTemplateNode;

  BhNodeViewProxyImpl(BhNodeView view, boolean isTemplateNode) {
    Objects.requireNonNull(view);
    this.view = view;
    this.isTemplateNode = isTemplateNode;
  }

  public BhNodeView getView() {
    return view;
  }

  @Override
  public Vec2D getPosOnWorkspace() {
    return view.getPositionManager().getPosOnWorkspace();
  }

  @Override
  public void setPosOnWorkspace(Vec2D pos, UserOperation userOpe) {
    Objects.requireNonNull(pos);
    Vec2D oldPos = view.getPositionManager().getPosOnWorkspace();
    view.getPositionManager().setTreePosOnWorkspace(pos.x, pos.y);
    if (userOpe != null && view.getModel().isPresent()) {
      userOpe.pushCmdOfSetNodePos(view.getModel().get(), oldPos);
    }
  }

  @Override
  public void move(Vec2D distance) {
    view.getPositionManager().move(distance.x, distance.y);
  }

  @Override
  public Vec2D getSizeIncludingOuters(boolean includeCnctr) {
    return view.getRegionManager().getNodeTreeSize(includeCnctr);
  }

  @Override
  public void replace(BhNode newNode, UserOperation userOpe) {
    if (newNode == null) {
      return;
    }
    BhNodeView newNodeView = newNode.getViewProxy().getView();
    if (newNodeView == null) {
      return;
    }
    boolean hasParent = !newNodeView.getTreeManager().isRootView();
    view.getTreeManager().replace(newNodeView);
    view.getModel().ifPresent(
        oldNode -> userOpe.pushCmdOfReplaceNodeView(oldNode, newNode, hasParent));
  }

  @Override
  public void switchPseudoClassState(String className, boolean enable) {
    if (StringUtils.isEmpty(className)) {
      return;
    }
    view.getLookManager().switchPseudoClassState(className, enable);
  }

  @Override
  public void removeFromGuiTree() {
    view.getTreeManager().removeFromGuiTree();
  }

  @Override
  public boolean isTemplateNode() {
    return isTemplateNode;
  }

  /** ビューを持っているか調べる. */
  public boolean hasView() {
    return true;
  }

  @Override
  public void lookAt() {
    if (view.getWorkspaceView() == null) {
      return;
    }
    view.getWorkspaceView().lookAt(view);
  }
}
