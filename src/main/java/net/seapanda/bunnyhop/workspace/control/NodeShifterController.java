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

package net.seapanda.bunnyhop.workspace.control;

import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.ui.control.MouseCtrlLock;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.view.NodeShifterView;

/**
 * 複数ノードを同時に移動させるノードシフタのコントローラ.
 *
 * @author K.Koike
 */
public class NodeShifterController {

  /** 管理するビュー. */
  private final NodeShifterView view;
  /** {@code view} が存在するワークスぺース. */
  private final Workspace ws;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final ModelAccessNotificationService notifService;
  private final DndEventInfo ddInfo = this.new DndEventInfo();
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();

  /**
   * コンストラクタ.
   *
   * @param view 管理するノードシフタのビュー
   * @param ws view があるワークスペース
   * @param service モデルへのアクセスの通知先となるオブジェクト
   */
  public NodeShifterController(
      NodeShifterView view, Workspace ws, ModelAccessNotificationService service) {
    this.view = view;
    this.ws = ws;
    this.notifService = service;
    view.setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent));
    view.setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent));
    view.setOnMouseReleased(mouseEvent -> onMouseReleased(mouseEvent));
    view.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
    ws.getView().ifPresent(wsView -> {
      ws.getCallbackRegistry().getOnNodeSelectionStateChanged().add(
          event -> event.node().getView().ifPresent(this::onNodeSelectionStateChanged));
      wsView.getCallbackRegistry().getOnNodeMoved().add(event -> onNodeMoved(event.nodeView()));
    });
  }

  /**
   * マウスボタン押下時の処理.
   *
   * @param event 発生したマウスイベント.
   */
  private void onMousePressed(MouseEvent event) {
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        event.consume();
        return;
      }
      ddInfo.context = notifService.beginWrite();
      ddInfo.isDndFinished = false;
      view.switchPseudoClassActivation(true, BhConstants.Css.PSEUDO_SELECTED);
      Point2D pos = view.sceneToLocal(event.getSceneX(), event.getSceneY());
      ddInfo.mousePressedPos = new Vec2D(pos.getX(), pos.getY());
      view.toFront();
      view.getLinkedNodes().forEach(
          view -> ddInfo.viewToOrgPos.put(view, view.getPositionManager().getPosOnWorkspace()));
      ddInfo.dragging = true;
      event.consume();
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /**
   * マウスドラッグ時の処理.
   *
   * @param event 発生したマウスイベント.
   */
  private void onMouseDragged(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      double diffX = event.getX() - ddInfo.mousePressedPos.x;
      double diffY = event.getY() - ddInfo.mousePressedPos.y;
      Vec2D wsSize = ViewUtil.getWorkspaceView(view).getSize();
      if (event.isShiftDown()) {
        view.move(new Vec2D(diffX, diffY), wsSize, true);
      } else {
        Vec2D distance = view.move(new Vec2D(diffX, diffY), wsSize, false);
        view.getLinkedNodes().forEach(nodeView -> nodeView.getPositionManager().move(distance));
      }
      event.consume();
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
      event.consume();
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }
    try {
      view.switchPseudoClassActivation(false, BhConstants.Css.PSEUDO_SELECTED);
      event.consume();
      ddInfo.viewToOrgPos.forEach(ddInfo.context.userOpe()::pushCmdOfSetNodePos);
    } finally {
      event.consume();
      terminateDnd();
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
   * このノードシフタがあるワークスペースのノードの位置が変更されたときの処理.
   *
   * @param nodeView 位置が変更されたノードのビュー
   */
  private void onNodeMoved(BhNodeView nodeView) {
    if (nodeView == null || nodeView.getModel().isEmpty()) {
      return;
    }
    updateNodeShifter(nodeView);
  }

  /**
   * このノードシフタがあるワークスペースのノードの選択状態が変更されたときの処理.
   *
   * @param nodeView 選択状態が変更されたノードのビュー
   */
  private void onNodeSelectionStateChanged(BhNodeView nodeView) {
    updateNodeShifter(nodeView);
  }

  /** ノードシフタの表示を更新する. */
  private void updateNodeShifter(BhNodeView nodeView) {
    BhNode node = nodeView.getModel().orElse(null);
    if (node != null
        && node.getWorkspace() == ws
        && node.isRoot()
        && node.isSelected()) {
      if (view.isLinkedWith(nodeView)) {
        if (ddInfo.dragging) {
          view.updateLinkPos(nodeView);
        } else {
          // undo 時にノードシフタの位置を更新する
          view.updateShifterAndAllLinkPositions();
        }
      } else {
        view.createLink(nodeView);
      }
      return;
    }
    view.deleteLink(nodeView);
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    mouseCtrlLock.unlock();
    ddInfo.reset();
    notifService.endWrite();
  }

  private class DndEventInfo {
    private Vec2D mousePressedPos = null;
    /** ノードとノードシフタで移動させる前の位置のマップ. */
    private final Map<BhNodeView, Vec2D> viewToOrgPos = new HashMap<>();
    /** ドラッグ中のとき true. */
    private boolean dragging = false;
    /** モデルの操作に伴うコンテキスト. */
    private ModelAccessNotificationService.Context context;
    /** D&D が終了しているかどうかのフラグ. */
    private boolean isDndFinished = true;

    /** D&Dイベント情報を初期化する. */
    private void reset() {
      mousePressedPos = null;
      viewToOrgPos.clear();
      dragging = false;
      context = null;
      isDndFinished = true;
    }
  }
}
