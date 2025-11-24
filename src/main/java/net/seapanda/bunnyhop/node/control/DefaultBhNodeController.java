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

package net.seapanda.bunnyhop.node.control;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.derivative.Derivative;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.model.event.UiEvent;
import net.seapanda.bunnyhop.node.model.event.UiEventType;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.LookManager.EffectTarget;
import net.seapanda.bunnyhop.node.view.BhNodeView.MouseEventInfo;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.control.MouseCtrlLock;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.TrashCan;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * ワークスペースの {@link BhNode} を操作するコントローラクラス.
 *
 * @author K.Koike
 */
public class DefaultBhNodeController implements BhNodeController {

  private final BhNode model;
  private final BhNodeView view;
  private final ModelAccessNotificationService notifService;
  private final TrashCan trashCan;
  private final MouseCtrlLock mouseCtrlLock =
      new MouseCtrlLock(MouseButton.PRIMARY, MouseButton.SECONDARY);
  private DndEventInfo ddInfo = new DndEventInfo();

  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   * @param service モデルへのアクセスの通知先となるオブジェクト
   * @param trashCan ゴミ箱のビューを操作するためのオブジェクト
   */
  public DefaultBhNodeController(
      BhNode model,
      BhNodeView view,
      ModelAccessNotificationService service,
      TrashCan trashCan) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(service);
    Objects.requireNonNull(trashCan);
    this.model = model;
    this.view = view;
    this.notifService = service;
    this.trashCan = trashCan;
    model.setView(view);
    view.setController(this);
    setViewEventHandlers();
    setNodeEventHandlers();
  }

  private void setViewEventHandlers() {
    BhNodeView.CallbackRegistry registry = view.getCallbackRegistry();
    registry.getOnMousePressed().setFirst(this::onMousePressed);
    registry.getOnMouseDragged().setFirst(this::onMouseDragged);
    registry.getOnMouseDragDetected().setFirst(this::onMouseDragDetected);
    registry.getOnMouseReleased().setLast(this::onMouseReleased);
  }

  private void setNodeEventHandlers() {
    BhNode.CallbackRegistry registry = model.getCallbackRegistry();
    registry.getOnConnected().add(event -> replaceView(event.disconnected()));
    registry.getOnSelectionStateChanged().add(event -> {
      boolean isSelected = event.isSelected();
      view.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SELECTED, isSelected);
      highlightDerivatives(model, isSelected);
    });
    registry.getOnCompileErrorStateUpdated()
        .add(event -> view.getLookManager().setCompileErrorVisibility(event.hasError()));
    registry.getOnBreakpointSet()
        .add(event -> view.getLookManager().setBreakpointVisibility(event.isBreakpointSet()));
    registry.getOnCorruptionStateChanged()
        .add(event -> view.getLookManager().setCorruptionMarkVisibility(event.isCorrupted()));
  }

  /** マウスボタン押下時の処理. */
  private void onMousePressed(MouseEventInfo info) {
    MouseEvent event = info.event();
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = notifService.beginWrite();
      ddInfo.isDndFinished = false;
      ddInfo.isBreakpointOperation = shouldSetBreakpoint(event);
      ddInfo.forwardEvent = !model.isMovable();
      if (ddInfo.isBreakpointOperation) {
        invokeOnUiEventReceived(event, false);
        setBreakpoint(ddInfo.context.userOpe());
        return;
      }
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
      view.getWorkspaceView().getRootNodeViews().forEach(
          nodeView -> nodeView.getLookManager().hideShadow(EffectTarget.CHILDREN));
      view.getLookManager().showShadow(EffectTarget.OUTERS);
      toFront();
      selectNode(event);
      Vec2D mousePressedPos = new Vec2D(event.getSceneX(), event.getSceneY());
      ddInfo.mousePressedPos = view.getPositionManager().sceneToLocal(mousePressedPos);
      ddInfo.posOnWorkspace = view.getPositionManager().getPosOnWorkspace();
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    }
  }

  /** マウスドラッグ時の処理. */
  private void onMouseDragged(MouseEventInfo info) {
    MouseEvent event = info.event();
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton())
          || ddInfo.isDndFinished
          || ddInfo.isBreakpointOperation) {
        return;
      }
      if (ddInfo.forwardEvent) {
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      if (ddInfo.dragging) {
        double diffX = event.getX() - ddInfo.mousePressedPos.x;
        double diffY = event.getY() - ddInfo.mousePressedPos.y;
        view.getPositionManager().move(diffX, diffY);
        // ドラッグ検出されていない場合, 強調は行わない.
        // 子ノードがワークスペース直下にいないのに, 重なったノード (入れ替え候補) が検出されるのを防ぐ
        highlightOverlappedNode();
        trashCan.auto(event.getSceneX(), event.getSceneY());
      }
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    }
  }

  /**
   * マウスドラッグを検出した時の処理.
   * 先に {@code onMouseDragged} が呼ばれ, ある程度ドラッグしたときにこれが呼ばれる.
   */
  private void onMouseDragDetected(MouseEventInfo info) {
    MouseEvent event = info.event();
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      if (ddInfo.isBreakpointOperation) {
        invokeOnUiEventReceived(event, false);
        return;
      }
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
      ddInfo.dragging = true;
      // 子ノードでかつ取り外し可能 -> ワークスペースへ
      if (model.isRemovable()) {
        toWorkspace();
      }
      toFront();
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEventInfo info) {
    MouseEvent event = info.event();
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }
    try {
      if (ddInfo.isBreakpointOperation) {
        invokeOnUiEventReceived(event, false);
        return;
      }
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
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
      deleteTrashedNode();
    } finally {
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
      ViewUtil.pushReverseMoveCmd(view, ddInfo.posOnWorkspace, userOpe);
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
    ViewUtil.pushReverseMoveCmd(view, pos, userOpe);
  }

  /** 同一ワークスペースへの移動処理. */
  private void toSameWorkspace() {
    if (ddInfo.dragging && model.isRoot()) {
      ViewUtil.pushReverseMoveCmd(view, ddInfo.posOnWorkspace, ddInfo.context.userOpe());
    }
  }

  /** ゴミ箱に入れられたノードを削除する. */
  private void deleteTrashedNode() {
    if (model.isRoot() && trashCan.isOpened()) {
      boolean canDelete = model.getEventInvoker().onDeletionRequested(
          new ArrayList<>() {{
            add(model);
          }},
          CauseOfDeletion.TRASH_CAN,
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

  /** ノードの選択処理を行う. */
  private void selectNode(MouseEvent event) {
    UserOperation userOpe = ddInfo.context.userOpe();
    // 右クリック
    if (event.getButton() == MouseButton.SECONDARY) {
      if (!BhSettings.Debug.isBreakpointSettingEnabled && !model.isSelected()) {
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
      selectToOutermost(userOpe);
    } else {
      model.getWorkspace().getSelectedNodes().forEach(selected -> selected.deselect(userOpe));
      model.select(userOpe);
    }
  }

  /** 外部ノードの末尾まで選択する. */
  private void selectToOutermost(UserOperation userOpe) {
    BhNode outerNode = model.findOuterNode(-1);
    while (outerNode != model) {
      if (!outerNode.isSelected() && outerNode.isMovable()) {
        outerNode.select(userOpe);
      }
      outerNode = outerNode.findParentNode();
    }
  }

  /**
   * {@code node} に対応するビューに {@code event} を送る.
   *
   * @param node このノードのビューにイベントを送る
   * @param info {@code node} に送るイベント
   */
  private static void dispatchEvent(BhNode node, BhNodeView.MouseEventInfo info) {
    Optional.ofNullable(node)
        .flatMap(BhNode::getView)
        .ifPresent(view -> view.getCallbackRegistry().forward(info));
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    trashCan.close();
    mouseCtrlLock.unlock();
    ddInfo = new DndEventInfo();
    notifService.endWrite();
  }

  /** {@link #view} を最前面に移動させる. */
  private void toFront() {
    WorkspaceView wsView = view.getWorkspaceView();
    if (wsView != null) {
      wsView.moveNodeViewToFront(view);
    }
  }

  /** {@link #view} と {@code oldNode} のノードビューを入れ替える. */
  private void replaceView(BhNode oldNode) {
    Optional.ofNullable(oldNode)
        .flatMap(BhNode::getView)
        .ifPresent(oldView -> oldView.getTreeManager().replace(view));
  }

  /**
   * ブレークポイントの設定が有効でかつ {@link #model} がブレークポイントグループに含まれている場合,
   * そのブレークポイントグループのリーダノードにブレークポイントを設定する.
   */
  private void setBreakpoint(UserOperation userOpe) {
    model.findBreakpointGroupLeader().ifPresent(
        node -> node.setBreakpoint(!node.isBreakpointSet(), userOpe));
  }

  /** {@code node} の派生ノードを強調表示を切り返る. */
  private static void highlightDerivatives(BhNode node, boolean enable) {
    if (node instanceof Derivative orgNode) {
      for (Derivative derv : orgNode.getDerivatives()) {
        derv.getView().ifPresent(view -> view.getLookManager()
            .switchPseudoClassState(BhConstants.Css.PSEUDO_HIGHLIGHT_DERIVATIVE, enable));
      }
    }
  }

  /** マウスイベントとアプリケーションの設定からブレークポイントをセットする操作かどうか判別する. */
  private boolean shouldSetBreakpoint(MouseEvent event) {
    return BhSettings.Debug.isBreakpointSettingEnabled
        && event.getButton() == MouseButton.SECONDARY;
  }

  /** {@link #model} が UI イベントを受け取ったときのフック処理を呼ぶ. */
  private void invokeOnUiEventReceived(MouseEvent event, boolean isEventTarget) {
    UiEvent uiEvent = toEventInfo(event);
    model.getEventInvoker().onUiEventReceived(uiEvent, isEventTarget, ddInfo.context.userOpe());
  }

  /** {@link MouseEvent} オブジェクトの情報を {@link UiEvent} オブジェクトに格納して返す. */
  private UiEvent toEventInfo(MouseEvent event) {
    UiEventType eventType = null;
    if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
      eventType = UiEventType.MOUSE_PRESSED;
    } else if (event.getEventType() == MouseEvent.DRAG_DETECTED) {
      eventType = UiEventType.DRAG_DETECTED;
    } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
      eventType = UiEventType.MOUSE_RELEASED;
    } else {
      throw new AssertionError("Invalid Mouse Event");
    }
    return new UiEvent(
        eventType,
        event.isPrimaryButtonDown(),
        event.isSecondaryButtonDown(),
        event.isMiddleButtonDown(),
        event.isBackButtonDown(),
        event.isForwardButtonDown(),
        event.isShiftDown(),
        event.isControlDown(),
        event.isAltDown());
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
  private static class DndEventInfo {
    private Vec2D mousePressedPos = new Vec2D();
    private Vec2D posOnWorkspace = new Vec2D();
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
    /** 今回の操作が, ブレークポイントの設定を行うためのものであるかどうかのフラグ. */
    private boolean isBreakpointOperation = false;
  }
}
