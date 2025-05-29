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
import java.util.Optional;
import java.util.SequencedSet;
import javafx.event.Event;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.node.event.MouseEventInfo;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.Trashbox;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースの {@link BhNode} を操作するコントローラクラス.
 *
 * @author K.Koike
 */
public class DefaultBhNodeController implements BhNodeController {

  private final BhNode model;
  private final BhNodeView view;
  private final ModelAccessNotificationService notifService;
  private final Trashbox trashbox;
  private final DndEventInfo ddInfo = this.new DndEventInfo();
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();

  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   * @param service モデルへのアクセスの通知先となるオブジェクト
   * @param trashbox ゴミ箱のビューを操作するためのオブジェクト
   */
  public DefaultBhNodeController(
      BhNode model,
      BhNodeView view,
      ModelAccessNotificationService service,
      Trashbox trashbox) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(service);
    Objects.requireNonNull(trashbox);
    this.model = model;
    this.view = view;
    this.notifService = service;
    this.trashbox = trashbox;
    model.setView(view);
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
    model.getEventManager().addOnNodeReplaced((oldNode, newNode, userOpe) -> replaceView(newNode));
    model.getEventManager().addOnSelectionStateChanged((node, isSelected, userOpe) -> {
      view.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SELECTED, isSelected);
      hilightDerivatives(model, isSelected);
    });
    model.getEventManager().addOnCompileErrStateChanged((node, hasCompileError, userOpe) -> 
        view.getLookManager().setCompileErrorVisibility(hasCompileError));
  }

  /** マウスボタン押下時の処理. */
  private void onMousePressed(MouseEvent event) {
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = notifService.begin();
      ddInfo.isDndFinished = false;
      if (!model.isMovable()) {
        ddInfo.forwardEvent = true;
        sendEvent(model.findParentNode(), event);
        return;
      }
      view.getWorkspaceView().getRootNodeViews().forEach(
          nodeView -> nodeView.getLookManager().hideShadow(false));
      view.getLookManager().showShadow(true);
      toFront();
      selectNode(event);
      Vec2D mousePressedPos = new Vec2D(event.getSceneX(), event.getSceneY());
      ddInfo.mousePressedPos = view.getPositionManager().sceneToLocal(mousePressedPos);
      ddInfo.posOnWorkspace = view.getPositionManager().getPosOnWorkspace();
      view.setMouseTransparent(true);
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /** マウスドラッグ時の処理. */
  private void onMouseDragged(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      if (ddInfo.forwardEvent) {
        sendEvent(model.findParentNode(), event);
        return;
      }
      if (event.isShiftDown()) {
        event.setDragDetect(false);
        return;
      }
      if (ddInfo.dragging) {
        double diffX = event.getX() - ddInfo.mousePressedPos.x;
        double diffY = event.getY() - ddInfo.mousePressedPos.y;
        view.getPositionManager().move(diffX, diffY);
        // ドラッグ検出されていない場合, 強調は行わない.
        // 子ノードがワークスペース直下にいないのに, 重なったノード (入れ替え候補) が検出されるのを防ぐ
        highlightOverlappedNode();
        trashbox.auto(event.getSceneX(), event.getSceneY());
      }
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /**
   * マウスドラッグを検出した時の処理.
   * 先に {@code onMouseDragged} が呼ばれ, ある程度ドラッグしたときにこれが呼ばれる.
   */
  private void onMouseDragDetected(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      if (ddInfo.forwardEvent) {
        sendEvent(model.findParentNode(), event);
        return;
      }
      if (event.isShiftDown()) {
        return;
      }
      ddInfo.dragging = true;
      model.getEventInvoker().onDragStarted(toEventInfo(event), ddInfo.context.userOpe());
      // 子ノードでかつ取り外し可能 -> ワークスペースへ
      if (model.isRemovable()) {
        toWorkspace();
      }
      toFront();
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
      if (ddInfo.forwardEvent) {
        sendEvent(model.findParentNode(), event);
        ddInfo.forwardEvent = false;
        return;
      }
      if (ddInfo.currentOverlapped != null) {
        ddInfo.currentOverlapped.getLookManager()
            .switchPseudoClassState(BhConstants.Css.PSEUDO_OVERLAPPED, false);
      }
      if (ddInfo.currentOverlapped != null) {
        // ワークスペース -> 子ノード
        ddInfo.currentOverlapped.getModel().ifPresent(this::toChildNode);
      } else {
        //同一ワークスペース上で移動
        toSameWorkspace();
      }
      deleteTrashedNode(event);
    } finally {
      event.consume();
      terminateDnd();
    }
  }

  /** {@link #model} を子ノードからワークスペース直下に移動させる. */
  private void toWorkspace() {
    ddInfo.latestParent = model.findParentNode();
    ddInfo.latestRoot = model.findRootNode();
    SequencedSet<Swapped> swappedNodes = BhNodePlacer.moveToWs(
        model.getWorkspace(),
        model,
        ddInfo.posOnWorkspace.x,
        ddInfo.posOnWorkspace.y,
        ddInfo.context.userOpe());
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          ddInfo.context.userOpe());
    }
    model.getEventInvoker().onMovedFromChildToWs(
        ddInfo.latestParent,
        ddInfo.latestRoot,
        model.getLastReplaced(),
        ddInfo.context.userOpe());
  }

  /**
   * {@link #model} をワークスペース直下から子ノードに移動する.
   *
   * @param oldChildNode 入れ替え対象の古い子ノード
   */
  private void toChildNode(BhNode oldChildNode) {
    UserOperation userOpe = ddInfo.context.userOpe();
    //ワークスペースから移動する場合
    if (model.isRoot()) {
      userOpe.pushCmdOfSetNodePos(view, ddInfo.posOnWorkspace);
    }
    // 入れ替えられるノードの親ノード
    final ConnectiveNode oldParentOfReplaced = oldChildNode.findParentNode();
    // 入れ替えられるノードのルートノード
    final BhNode oldRootOfReplaced = oldChildNode.findRootNode();
    // 重なっているノードをこのノードと入れ替え
    final SequencedSet<Swapped> swappedNodes =
        BhNodePlacer.replaceChild(oldChildNode, model, userOpe);
    // ワークスペースに移ったノードの位置更新
    oldChildNode.getView().ifPresent(oldChildView -> {
      var len = BhConstants.LnF.REPLACED_NODE_SHIFT;
      shift(oldChildView, new Vec2D(len, len), userOpe);
    });     
    // Workspace から 子ノードに移動したときのスクリプト実行
    model.getEventInvoker().onMovedFromWsToChild(oldChildNode, userOpe);
    // 子ノードから Workspace に移動したときのスクリプト実行
    oldChildNode.getEventInvoker().onMovedFromChildToWs(
        oldParentOfReplaced, oldRootOfReplaced, model, userOpe);
    // 子ノード入れ替え時のスクリプト実行
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /** {@code view} の位置を {@code amount} だけずらす. */
  private static void shift(BhNodeView view, Vec2D amount, UserOperation userOpe) {
    Vec2D pos = view.getPositionManager().getPosOnWorkspace();
    view.getPositionManager().setTreePosOnWorkspace(pos.x + amount.x, pos.y + amount.y);
    userOpe.pushCmdOfSetNodePos(view, pos);
  }

  /** 同一ワークスペースへの移動処理. */
  private void toSameWorkspace() {
    if (ddInfo.dragging && model.isRoot()) {
      ddInfo.context.userOpe().pushCmdOfSetNodePos(view, ddInfo.posOnWorkspace);
    }
  }

  /** ゴミ箱に入れられたノードを削除する. */
  private void deleteTrashedNode(MouseEvent event) {
    if (model.isRoot() && trashbox.isOpened()) {
      boolean canDelete = model.getEventInvoker().onDeletionRequested(
          new ArrayList<BhNode>() {{
            add(model);
          }},
          CauseOfDeletion.TRASH_BOX,
          ddInfo.context.userOpe());
      if (!canDelete) {
        return;
      }
      SequencedSet<Swapped> swappedNodes = BhNodePlacer.deleteNode(model, ddInfo.context.userOpe());
      for (Swapped swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            ddInfo.context.userOpe());
      }
    }
  }

  /** view と重なっている BhNodeView を強調する. */
  private void highlightOverlappedNode() {
    if (ddInfo.currentOverlapped != null) {
      // 前回重なっていたものをライトオフ
      ddInfo.currentOverlapped.getLookManager()
          .switchPseudoClassState(BhConstants.Css.PSEUDO_OVERLAPPED, false);
    }
    ddInfo.currentOverlapped = null;
    List<BhNodeView> overlappedList = view.getRegionManager().searchForOverlapped();
    for (BhNodeView overlapped : overlappedList) {
      if (overlapped.getModel().map(node -> node.canBeReplacedWith(model)).orElse(false)) {
        overlapped.getLookManager()
            .switchPseudoClassState(BhConstants.Css.PSEUDO_OVERLAPPED, true);
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
    UserOperation userOpe = ddInfo.context.userOpe();
    // 右クリック
    if (event.getButton() == MouseButton.SECONDARY) {
      if (!model.isSelected()) {
        model.select(userOpe);
      }
      return;
    }
    // 左クリック
    if (event.isShiftDown()) {
      if (model.isSelected()) {
        model.deselect(userOpe);
      } else {
        model.select(userOpe);
      }
    } else if (model.isSelected() && event.getClickCount() == 2) {
      // 末尾ノードまで一気に選択
      BhNode outerNode = model.findOuterNode(-1);
      while (outerNode != model) {
        if (!outerNode.isSelected() && outerNode.isMovable()) {
          outerNode.select(userOpe);
        }
        outerNode = outerNode.findParentNode();
      }
    } else {
      model.getWorkspace().getSelectedNodes().forEach(selected -> selected.deselect(userOpe));
      model.select(userOpe);
    }
  }

  /**
   * {@code node} に対応するビューに {@code event} を送る.
   *
   * @param node このノードのビューにイベントを送る
   * @param event {@code node} に送るイベント
   */
  private static void sendEvent(BhNode node, Event event) {
    Optional.ofNullable(node)
        .flatMap(BhNode::getView)
        .ifPresent(view -> view.getEventManager().dispatch(event));
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    MouseButton button = event.getButton();
    if (button != MouseButton.PRIMARY && button != MouseButton.SECONDARY) {
      event.consume();
    }
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    trashbox.close();
    mouseCtrlLock.unlock();
    view.setMouseTransparent(false);
    ddInfo.reset();
    notifService.end();
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

  /** {@link #view} を最前面に移動させる. */
  private void toFront() {
    WorkspaceView wsView = view.getWorkspaceView();
    if (wsView != null) {
      wsView.moveNodeViewToFront(view);
    }
  }

  /** {@link #view} と {@code newNode} のノードビューを入れ替える. */
  private void replaceView(BhNode newNode) {
    Optional.ofNullable(newNode)
        .flatMap(BhNode::getView)
        .ifPresent(view.getTreeManager()::replace);
  }

  /** {@code node} の派生ノードを強調表示を切り返る. */
  private static void hilightDerivatives(BhNode node, boolean enable) {
    if (node instanceof Derivative orgNode) {
      for (Derivative derv : orgNode.getDerivatives()) {
        derv.getView().ifPresent(view -> view.getLookManager()
            .switchPseudoClassState(BhConstants.Css.PSEUDO_HIGHLIGHT_DERIVATIVE, enable));
      }
    }
  }

  @Override
  public BhNode getModel() {
    return model;
  }

  @Override
  public BhNodeView getView() {
    return view;
  }

  @Override
  public ModelAccessNotificationService getNotificationService() {
    return notifService;
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private class DndEventInfo {
    private Vec2D mousePressedPos = null;
    private Vec2D posOnWorkspace = null;
    /** 現在重なっている View. */
    private BhNodeView currentOverlapped = null;
    /** イベントを親ノードに転送する場合 true. */
    private boolean forwardEvent = false;
    /** ドラッグ中のとき true. */
    private boolean dragging = false;
    /** {@code model} に最後につながっていた親ノード. */
    private ConnectiveNode latestParent = null;
    /** {@code latestParent} のルートノード. */
    private BhNode latestRoot = null;
    /** モデルの操作に伴うコンテキスト. */
    private ModelAccessNotificationService.Context context;
    /** D&D が終了しているかどうかのフラグ. */
    private boolean isDndFinished = true;

    /** D&Dイベント情報を初期化する. */
    private void reset() {
      mousePressedPos = null;
      posOnWorkspace = null;
      currentOverlapped = null;
      forwardEvent = false;
      dragging = false;
      latestParent = null;
      latestRoot = null;
      context = null;
      isDndFinished = true;
    }
  }
}
