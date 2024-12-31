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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.event.BhNodeEventAgent.MouseEventInfo;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * BhNode のコントローラクラスに共通の処理をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhNodeController {

  private final BhNode model;
  private final BhNodeView view;
  private final DndEventInfo ddInfo = this.new DndEventInfo();
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();

  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   */
  protected BhNodeController(BhNode model, BhNodeView view) {
    this.model = model;
    this.view = view;
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    view.setController(this);
    setEventHandlers();
  }

  /** 各種イベントハンドラをセットする. */
  private void setEventHandlers() {
    view.getEventManager().setOnMousePressed(this::onMousePressed);
    view.getEventManager().setOnMouseDragged(this::onMouseDragged);
    view.getEventManager().setOnDragDetected(this::onMouseDragDetected);
    view.getEventManager().setOnMouseReleased(this::onMouseReleased);
    view.getEventManager().addEventFilter(
        MouseEvent.ANY,
        mouseEvent -> consumeIfNotAcceptable(mouseEvent));
  }

  /** マウスボタン押下時の処理. */
  private void  onMousePressed(MouseEvent event) {
    if (!mouseCtrlLock.tryLock(event.getButton())) {
      event.consume();
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      if (!model.isMovable()) {
        ddInfo.propagateEvent = true;
        propagateGuiEvent(model.findParentNode(), event);
        return;
      }
      // BhNode の新規追加の場合, すでに undo 用コマンドオブジェクトがセットされている
      if (ddInfo.userOpe == null) {
        ddInfo.userOpe = new UserOperation();
      }
      view.getLookManager().drawShadow();
      view.getPositionManager().toFront();
      selectNode(event);  //選択処理
      Point2D mousePressedPos = view.sceneToLocal(event.getSceneX(), event.getSceneY());
      ddInfo.mousePressedPos = new Vec2D(mousePressedPos.getX(), mousePressedPos.getY());
      ddInfo.posOnWorkspace = view.getPositionManager().getPosOnWorkspace();
      view.setMouseTransparent(true);
      event.consume();
    } catch (Throwable e) {
      mouseCtrlLock.unlock();
      throw e;
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスドラッグ時の処理. */
  private void onMouseDragged(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      event.consume();
      return;
    }
    try {
      if (ddInfo.propagateEvent) {
        propagateGuiEvent(model.findParentNode(), event);
        return;
      }
      if (event.isShiftDown()) {
        event.setDragDetect(false);
        event.consume();
        return;
      }
      if (ddInfo.dragging) {
        double diffX = event.getX() - ddInfo.mousePressedPos.x;
        double diffY = event.getY() - ddInfo.mousePressedPos.y;
        view.getPositionManager().move(diffX, diffY);
        // ドラッグ検出されていない場合, 強調は行わない.
        // 子ノードがダングリングになっていないのに, 重なったノード (入れ替え候補) が検出されるのを防ぐ
        highlightOverlappedNode();
        BhService.trashboxCtrl().auto(event.getSceneX(), event.getSceneY());
      }
      event.consume();
    } catch (Throwable e) {
      mouseCtrlLock.unlock();
      throw e;
    }
  }

  /**
   * マウスドラッグを検出した時の処理.
   * 先に {@code onMouseDragged} が呼ばれ, ある程度ドラッグしたときにこれが呼ばれる.
   */
  private void onMouseDragDetected(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      event.consume();
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      if (ddInfo.propagateEvent) {
        propagateGuiEvent(model.findParentNode(), event);
        return;
      }
      if (event.isShiftDown()) {
        event.consume();
        return;
      }
      ddInfo.dragging = true;
      // 子ノードでかつ取り外し可能 -> 親ノードから切り離し, ダングリング状態へ
      if (model.isRemovable()) {
        ddInfo.latestParent = model.findParentNode();
        ddInfo.latestRoot = model.findRootNode();
        List<Swapped> swappedNodes = BhService.bhNodePlacer().removeChild(model, ddInfo.userOpe);
        for (var swapped : swappedNodes) {
          swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
              swapped.oldNode(),
              swapped.newNode(),
              swapped.newNode().getParentConnector(),
              ddInfo.userOpe);
        }
      }
      model.getEventAgent().execOnDragStarted(toEventInfo(event), ddInfo.userOpe);
      view.getPositionManager().toFront();
    } catch (Throwable e) {
      mouseCtrlLock.unlock();
      throw e;
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      event.consume();
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      if (ddInfo.propagateEvent) {
        propagateGuiEvent(model.findParentNode(), event);
        ddInfo.propagateEvent = false;
        return;
      }
      if (ddInfo.currentOverlapped != null) {
        ddInfo.currentOverlapped.getViewProxy().switchPseudoClassState(
            BhConstants.Css.PSEUDO_OVERLAPPED, false);
      }
      //子ノード -> ワークスペース
      if (model.isRootDangling() && ddInfo.currentOverlapped == null) {
        toWorkspace(model.getWorkspace());
      } else if (ddInfo.currentOverlapped != null) {
        // (ワークスペース or 子ノード) -> 子ノード
        toChildNode(ddInfo.currentOverlapped);
      } else {
        //同一ワークスペース上で移動
        toSameWorkspace();
      }
      view.getPositionManager().updateZpos();
      deleteTrashedNode(event);
      BhService.compileErrNodeManager().updateErrorNodeIndicator(ddInfo.userOpe);
      BhService.compileErrNodeManager().unmanageNonErrorNodes(ddInfo.userOpe);
      BhService.derivativeCache().clearAll();
      BhService.undoRedoAgent().pushUndoCommand(ddInfo.userOpe);
      ddInfo.reset();
      view.setMouseTransparent(false);
      BhService.trashboxCtrl().close();
      event.consume();
    } finally {
      mouseCtrlLock.unlock();
      ModelExclusiveControl.unlockForModification();
    }
  }

  /**
   * (ワークスペース or 子ノード) から 子ノード に移動する.
   *
   * @param oldChildNode 入れ替え対象の古い子ノード
   */
  private void toChildNode(BhNode oldChildNode) {
    //ワークスペースから移動する場合
    if (model.isRootOnWs()) {
      ddInfo.userOpe.pushCmdOfSetNodePos(model, ddInfo.posOnWorkspace);
    }
    // 入れ替えられるノードの親ノード
    final ConnectiveNode oldParentOfReplaced = oldChildNode.findParentNode();
    // 入れ替えられるノードのルートノード
    final BhNode oldRootOfReplaced = oldChildNode.findRootNode();
    // 重なっているノードをこのノードと入れ替え
    final List<Swapped> swappedNodes =
        BhService.bhNodePlacer().replaceChild(oldChildNode, model, ddInfo.userOpe);
    // 接続変更時のスクリプト実行
    model.getEventAgent().execOnMovedToChild(
        ddInfo.latestParent, ddInfo.latestRoot, oldChildNode, ddInfo.userOpe);

    Vec2D posOnWs = oldChildNode.getViewProxy().getPosOnWorkspace();
    double newXposInWs = posOnWs.x + BhConstants.LnF.REPLACED_NODE_SHIFT;
    double newYposInWs = posOnWs.y + BhConstants.LnF.REPLACED_NODE_SHIFT;
    // 重なっているノードを WS に移動
    BhService.bhNodePlacer().moveToWs(
        oldChildNode.getWorkspace(), oldChildNode, newXposInWs, newYposInWs, ddInfo.userOpe);
    // 接続変更時のスクリプト実行
    oldChildNode.getEventAgent().execOnMovedFromChildToWs(
        oldParentOfReplaced, oldRootOfReplaced, model, false, ddInfo.userOpe);
    // 子ノード入れ替え時のスクリプト実行
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          ddInfo.userOpe);
    }
  }

  /**
   * 子ノードからワークスペースに移動する.
   *
   * @param ws 移動先のワークスペース
   */
  private void toWorkspace(Workspace ws) {
    Vec2D absPosInWs = view.getPositionManager().getPosOnWorkspace();
    BhService.bhNodePlacer().moveToWs(ws, model, absPosInWs.x, absPosInWs.y, ddInfo.userOpe);
    model.getEventAgent().execOnMovedFromChildToWs(
        ddInfo.latestParent,
        ddInfo.latestRoot,
        model.getLastReplaced(),
        true,
        ddInfo.userOpe);  //接続変更時のスクリプト実行
    view.getLookManager().arrangeAndResize();
  }

  /** 同一ワークスペースへの移動処理. */
  private void toSameWorkspace() {
    if (ddInfo.dragging && (model.getState() == BhNode.State.ROOT_ON_WS)) {
      ddInfo.userOpe.pushCmdOfSetNodePos(model, ddInfo.posOnWorkspace);
    }
    view.getLookManager().arrangeAndResize();
  }

  /** ゴミ箱に入れられたノードを削除する. */
  private void deleteTrashedNode(MouseEvent mouseEvent) {
    boolean isInTrashboxArea = BhService.trashboxCtrl().isPointInTrashBoxArea(
        mouseEvent.getSceneX(), mouseEvent.getSceneY());
    if (model.isRootOnWs() && isInTrashboxArea) {
      model.getEventAgent().execOnDeletionRequested(
          new ArrayList<BhNode>() {{
            add(model);
          }},
          CauseOfDeletion.TRASH_BOX, ddInfo.userOpe);
      List<Swapped> swappedNodes = BhService.bhNodePlacer().deleteNode(model, ddInfo.userOpe);
      for (var swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            ddInfo.userOpe);
      }
    }
  }

  /** view と重なっている BhNodeView を強調する. */
  private void highlightOverlappedNode() {
    if (ddInfo.currentOverlapped != null) {
      // 前回重なっていたものをライトオフ
      ddInfo.currentOverlapped.getViewProxy().switchPseudoClassState(
          BhConstants.Css.PSEUDO_OVERLAPPED, false);
    }
    ddInfo.currentOverlapped = null;
    List<BhNode> overlappedList = view.getRegionManager().searchForOverlappedModels();
    for (BhNode overlapped : overlappedList) {
      if (overlapped.canBeReplacedWith(model)) {  //このノードと入れ替え可能
        // 今回重なっているものをライトオン
        overlapped.getViewProxy().switchPseudoClassState(BhConstants.Css.PSEUDO_OVERLAPPED, true);
        ddInfo.currentOverlapped = overlapped;
        break;
      }
    }
  }

  /**
   * ノードの選択処理を行う.
   *
   * @param isShiftDown シフトボタンが押されている場合 true
   */
  private void selectNode(MouseEvent event) {
    // 右クリック
    if (event.getButton() == MouseButton.SECONDARY) {
      if (!model.isSelected()) {
        model.select(ddInfo.userOpe);
      }
      return;
    }
    // 左クリック
    if (event.isShiftDown()) {
      if (model.isSelected()) {
        model.deselect(ddInfo.userOpe);
      } else {
        model.select(ddInfo.userOpe);
      }
    } else if (model.isSelected()) {
      // 末尾ノードまで一気に選択
      BhNode outerNode = model.findOuterNode(-1);
      while (outerNode != model) {
        if (!outerNode.isSelected() && outerNode.isMovable()) {
          outerNode.select(ddInfo.userOpe);
        }
        outerNode = outerNode.findParentNode();
      }
    } else {
      model.getWorkspace().getSelectedNodes().forEach(
          selected -> selected.deselect(ddInfo.userOpe));
      model.select(ddInfo.userOpe);
    }
  }

  /**
   * 引数で指定したノードに対応するビューに GUI イベントを伝播する.
   *
   * @param node イベントを伝播したい {@link BhNodeView} に対応する {@link BhNode}
   * @param event 伝播したいイベント
   */
  private void propagateGuiEvent(BhNode node, Event event) {
    if (node == null) {
      return;
    }
    BhNodeView nodeView = node.getViewProxy().getView();
    if (nodeView == null) {
      return;
    }
    nodeView.getEventManager().propagateEvent(event);
    event.consume();
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    MouseButton button = event.getButton();
    if (button != MouseButton.PRIMARY && button != MouseButton.SECONDARY) {
      event.consume();
    }
  }

  /** {@link MouseEvent} オブジェクトの情報を {@link MouseEventInfo} オブジェクトに格納して返す. */
  private MouseEventInfo toEventInfo(MouseEvent event) {
    return new MouseEventInfo(
        event.getButton() == MouseButton.PRIMARY,
        event.getButton() == MouseButton.SECONDARY,
        event.getButton() == MouseButton.MIDDLE,
        event.getButton() == MouseButton.BACK,
        event.getButton() == MouseButton.FORWARD,
        event.isShiftDown(),
        event.isControlDown(),
        event.isAltDown());
  }

  /** このオブジェクトが次の D&D 操作で使用する undo 用コマンドオブジェクトをセットする. */
  public void setUserOpeCmdForDnd(UserOperation userOpe) {
    ddInfo.userOpe = userOpe;
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private class DndEventInfo {
    Vec2D mousePressedPos = null;
    Vec2D posOnWorkspace = null;
    /** 現在重なっている View. */
    BhNode currentOverlapped = null;
    /** イベントを親ノードに伝播する場合 true. */
    boolean propagateEvent = false;
    /** ドラッグ中のとき true. */
    boolean dragging = false;
    /** {@code model} に最後につながっていた親ノード. */
    ConnectiveNode latestParent = null;
    /** {@code latestParent} のルートノード. */
    BhNode latestRoot = null;
    /** undo 用コマンドオブジェクト. */
    UserOperation userOpe;

    /** D&Dイベント情報を初期化する. */
    public void reset() {
      mousePressedPos = null;
      posOnWorkspace = null;
      currentOverlapped = null;
      propagateEvent = false;
      dragging = false;
      latestParent = null;
      latestRoot = null;
      userOpe = null;
    }
  }
}
