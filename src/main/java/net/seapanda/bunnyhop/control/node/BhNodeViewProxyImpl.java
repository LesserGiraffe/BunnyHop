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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
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
    view.getPositionManager().setPosOnWorkspace(pos.x, pos.y);
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
    return view.getRegionManager().getNodeSizeIncludingOuters(includeCnctr);
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
    boolean hasParent = newNodeView.getParent() != null;
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
  public void setCompileErrorVisibility(boolean visible, UserOperation userOpe) {
    if (userOpe != null) {
      userOpe.pushCmdOfSetCompileError(view, view.getLookManager().isCompileErrorVisible());
    }
    view.getLookManager().setCompileErrorVisibility(visible);
  }

  @Override
  public boolean isTemplateNode() {
    return isTemplateNode;
  }

  @Override
  public void notifyNodeSelected() {
    view.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SELECTED, true);
    view.getModel().ifPresent(node -> hilightDerivatives(node, true));
  }

  @Override
  public void notifyNodeDeselected() {
    view.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SELECTED, false);
    view.getModel().ifPresent(node -> hilightDerivatives(node, false));
  }

  private void hilightDerivatives(BhNode node, boolean enable) {
    if (node instanceof Derivative orgNode) {
      orgNode.getDerivatives().forEach(derivative -> derivative
          .getViewProxy()
          .getView()
          .getLookManager()
          .switchPseudoClassState(BhConstants.Css.PSEUDO_HIGHLIGHT_DERIVATIVE, enable));      
    }
  }

  @Override
  public void lookAt() {
    view.getModel()
        .map(node -> node.getWorkspace().getViewProxy().getView())
        .ifPresent(wsView -> wsView.lookAt(view));
  }
}
