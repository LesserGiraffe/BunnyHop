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

package net.seapanda.bunnyhop.control.workspace;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * 複数ノードを同時に移動させるマルチノードシフタのコントローラ.
 *
 * @author K.Koike
 */
public class MultiNodeShifterController {

  /** 管理するビュー. */
  private final MultiNodeShifterView view;
  /** {@code view} が存在するワークスぺース. */
  private final Workspace ws;
  private final DndEventInfo ddInfo = this.new DndEventInfo();
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();

  /**
   * コンストラクタ.
   *
   * @param view 管理するマルチノードシフタのビュー
   * @param ws view があるワークスペース
   */
  public MultiNodeShifterController(MultiNodeShifterView view, Workspace ws) {
    this.view = view;
    this.ws = ws;
    view.setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent));
    view.setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent));
    view.setOnMouseReleased(mouseEvent -> onMouseReleased(mouseEvent));
    view.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
    WorkspaceView wsView = ws.getViewProxy().getView();
    if (wsView != null) {
      ws.getEventManager().addOnNodeSelectionStateChanged(
          (node, isSelected, userOpe) -> notifyNodeSelectionStateChanged(node));
      wsView.addOnNodeMoved((nodeView, pos) -> notifyNodeMoved(nodeView));
    }
  }

  /**
   * マウスボタン押下時の処理.
   *
   * @param event 発生したマウスイベント.
   * @param mousePressedPos マウスボタン押下時のカーソル位置の格納先
   */
  private void onMousePressed(MouseEvent event) {
    if (!mouseCtrlLock.tryLock(event.getButton())) {
      event.consume();
      return;
    }
    try {
      view.switchPseudoClassActivation(true, BhConstants.Css.PSEUDO_SELECTED);
      javafx.geometry.Point2D pos = view.sceneToLocal(event.getSceneX(), event.getSceneY());
      ddInfo.mousePressedPos = new Vec2D(pos.getX(), pos.getY());
      view.toFront();
      view.getLinkedNodes().forEach(
          node -> ddInfo.nodeToOrgPos.put(node, node.getViewProxy().getPosOnWorkspace()));
      ddInfo.dragging = true;
      ddInfo.userOpe = new UserOperation();
      event.consume();
    } catch (Throwable e) {
      mouseCtrlLock.unlock();
    }
  }

  /**
   * マウスドラッグ時の処理.
   *
   * @param event 発生したマウスイベント.
   * @param mousePressedPos マウスボタン押下時のカーソル位置
   */
  private void onMouseDragged(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      event.consume();
      return;
    }
    try {
      double diffX = event.getX() - ddInfo.mousePressedPos.x;
      double diffY = event.getY() - ddInfo.mousePressedPos.y;
      Vec2D wsSize = ViewUtil.getWorkspaceView(view).getWorkspaceSize();
      if (event.isShiftDown()) {
        view.move(new Vec2D(diffX, diffY), wsSize, true);
      } else {
        Vec2D distance = view.move(new Vec2D(diffX, diffY), wsSize, false);
        view.getLinkedNodes().forEach(node -> node.getViewProxy().move(distance));
      }
      event.consume();
    } catch (Throwable e) {
      mouseCtrlLock.unlock();
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      event.consume();
      return;
    }
    try {
      view.switchPseudoClassActivation(false, BhConstants.Css.PSEUDO_SELECTED);
      event.consume();
      ddInfo.nodeToOrgPos.forEach(ddInfo.userOpe::pushCmdOfSetNodePos);
      BhService.undoRedoAgent().pushUndoCommand(ddInfo.userOpe);
      ddInfo.reset();
    } finally {
      mouseCtrlLock.unlock();
    }
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    MouseButton button = event.getButton();
    if (button != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /**
   * このマルチノードシフタがあるワークスペースのノードの位置が変更されたことをこのオブジェクトに通知する.
   *
   * @param node 位置が変更されたノード
   */
  private void notifyNodeMoved(BhNodeView nodeView) {
    if (nodeView == null || nodeView.getModel().isEmpty()) {
      return;
    }
    updateMultiNodeShifter(nodeView.getModel().get());
  }

  /**
   * このマルチノードシフタがあるワークスペースのノードの選択状態が変更されたことをこのオブジェクトに通知する.
   *
   * @param node 位置が変更されたノード
   */
  private void notifyNodeSelectionStateChanged(BhNode node) {
    updateMultiNodeShifter(node);
  }

  /** マルチノードシフタの表示を更新する. */
  private void updateMultiNodeShifter(BhNode node) {
    if (node != null
        && node.getWorkspace() == ws
        && node.isRoot()
        && node.isSelected()) {
      if (view.isLinkedWith(node)) {
        if (ddInfo.dragging) {
          view.updateLinkPos(node);
        } else {
          // undo 時にマルチノードシフタの位置を更新する
          view.updateShifterAndAllLinkPositions();
        }
      } else {
        view.createLink(node);
      }
      return;
    }
    view.deleteLink(node);
  }

  private class DndEventInfo {
    Vec2D mousePressedPos = null;
    /** undo 用コマンドオブジェクト. */
    UserOperation userOpe;
    /** ノードとマルチノードシフタで移動させる前の位置のマップ. */
    final Map<BhNode, Vec2D> nodeToOrgPos = new HashMap<>();
    /** ドラッグ中のとき true. */
    boolean dragging = false;

    /** D&Dイベント情報を初期化する. */
    public void reset() {
      mousePressedPos = null;
      userOpe = null;
      nodeToOrgPos.clear();
      dragging = false;
    }
  }
}
