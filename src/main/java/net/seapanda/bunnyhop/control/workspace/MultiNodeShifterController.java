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

import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;

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

  /**
   * コンストラクタ.
   *
   * @param view 管理するマルチノードシフタのビュー
   * @param ws view があるワークスペース
   * */
  public MultiNodeShifterController(MultiNodeShifterView view, Workspace ws) {
    this.view = view;
    this.ws = ws;
    Vec2D mousePressedPos = new Vec2D(0.0, 0.0);
    view.setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent, mousePressedPos));
    view.setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent, mousePressedPos));
    view.setOnMouseReleased(mouseEvent -> onMouseReleased(mouseEvent));
  }

  /**
   * マウスボタン押下時の処理.
   *
   * @param mouseEvent 発生したマウスイベント.
   * @param mousePressedPos マウスボタン押下時のカーソル位置の格納先
   */
  private void onMousePressed(MouseEvent mouseEvent, Vec2D mousePressedPos) {
    view.switchPseudoClassActivation(true, BhConstants.Css.PSEUDO_SELECTED);
    javafx.geometry.Point2D pos =
        view.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
    mousePressedPos.x = pos.getX();
    mousePressedPos.y = pos.getY();
    view.toFront();
    mouseEvent.consume();
  }

  /**
   * マウスドラッグ時の処理.
   *
   * @param mouseEvent 発生したマウスイベント.
   * @param mousePressedPos マウスボタン押下時のカーソル位置
   */
  private void onMouseDragged(MouseEvent mouseEvent, Vec2D mousePressedPos) {
    double diffX = mouseEvent.getX() - mousePressedPos.x;
    double diffY = mouseEvent.getY() - mousePressedPos.y;
    Vec2D wsSize = ViewHelper.INSTANCE.getWorkspaceView(view).getWorkspaceSize();

    if (mouseEvent.isShiftDown()) {
      view.move(new Vec2D(diffX, diffY), wsSize, true);
    } else {
      Vec2D distance = view.move(new Vec2D(diffX, diffY), wsSize, false);
      view.getLinkedNodeList().forEach(node -> MsgService.INSTANCE.moveNodeOnWs(node, distance));
    }
    mouseEvent.consume();
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEvent mouseEvent) {
    view.switchPseudoClassActivation(false, BhConstants.Css.PSEUDO_SELECTED);
    mouseEvent.consume();
  }

  /**
   * マルチノードシフタを更新する.
   *
   * @param node マルチノードシフタの更新の原因を作ったノード
   * */
  void updateMultiNodeShifter(BhNode node) {
    if (node.getWorkspace() == ws
        && node.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS
        && node.isSelected()) {

      if (view.isLinked(node)) {
        view.updateLinkPos(node);
      } else {
        view.createLink(node);
      }
      return;
    }
    view.deleteLink(node);
  }
}
