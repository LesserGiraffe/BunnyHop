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


import static net.seapanda.bunnyhop.node.view.effect.VisualEffectTarget.OUTERS;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.BREAKPOINT;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.COMPILE_ERROR;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.CORRUPTION;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.MOVE_GROUP;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.OVERLAP;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.RELATED_NODE_GROUP;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.SELECTION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.BiFunction;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.model.event.UiEvent;
import net.seapanda.bunnyhop.node.model.event.UiEventType;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.MouseEventInfo;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.service.BhNodeLocation;
import net.seapanda.bunnyhop.service.accesscontrol.ExclusionId;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionContext;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.control.MouseCtrlLock;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;
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
  private final TransactionNotificationService notifService;
  private final TrashCan trashCan;
  private final VisualEffectManager effectManager;
  private final MouseCtrlLock mouseCtrlLock =
      new MouseCtrlLock(MouseButton.PRIMARY, MouseButton.SECONDARY);
  private DndEventInfo ddInfo = new DndEventInfo();
  private CommonEventInfo commonEventInfo = new CommonEventInfo(false, false, false);

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
      TransactionNotificationService service,
      TrashCan trashCan,
      VisualEffectManager  visualEffectManager) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(service);
    Objects.requireNonNull(trashCan);
    Objects.requireNonNull(visualEffectManager);
    this.model = model;
    this.view = view;
    this.notifService = service;
    this.trashCan = trashCan;
    this.effectManager = visualEffectManager;
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
    registry.getOnSelectionStateChanged().add(
        event -> effectManager.setEffectEnabled(view, event.isSelected(), SELECTION));
    registry.getOnCompileErrorStateUpdated().add(
        event -> effectManager.setEffectEnabled(view, event.hasError(), COMPILE_ERROR));
    registry.getOnBreakpointSet().add(
        event -> effectManager.setEffectEnabled(view, event.isBreakpointSet(), BREAKPOINT));
    registry.getOnCorruptionStateChanged().add(
        event -> effectManager.setEffectEnabled(view, event.isCorrupted(), CORRUPTION));

    effectManager.setEffectEnabled(view, model.isSelected(), SELECTION);
    effectManager.setEffectEnabled(view, model.hasCompileErr(), COMPILE_ERROR);
    effectManager.setEffectEnabled(view, model.isBreakpointSet(), BREAKPOINT);
    effectManager.setEffectEnabled(view, model.isCorrupted(), CORRUPTION);
  }

  /** マウスボタン押下時の処理. */
  private void onMousePressed(MouseEventInfo info) {
    setUserData(info);
    MouseEvent event = info.event;
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = beginTransaction(info);
      if (ddInfo.context == null) {
        mouseCtrlLock.unlock();
        return;
      }
      setBreakpoint(info);
      highlightRelatedNodes(info);
      ddInfo.forwardEvent = !model.isMovable();
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
      if (BhSettings.Ui.nodeManipMode != NodeManipulationMode.MODE_2) {
        effectManager.disableEffects(RELATED_NODE_GROUP, ddInfo.context.userOpe());
      }
      effectManager.disableEffects(model.getWorkspace(), MOVE_GROUP, ddInfo.context.userOpe());
      if (isInNodeMoveMode(info.event)) {
        effectManager.setEffectEnabled(view, true, MOVE_GROUP, OUTERS, ddInfo.context.userOpe());
      }
      toFront();
      selectNode(event);
      savePositions(event);
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /** マウスドラッグ時の処理. */
  private void onMouseDragged(MouseEventInfo info) {
    setUserData(info);
    MouseEvent event = info.event;
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton())) {
        return;
      }
      if (ddInfo.forwardEvent) {
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      if (ddInfo.isDragDetected && isInNodeMoveMode(info.event)) {
        double diffX = event.getX() - ddInfo.mousePressedPos.x;
        double diffY = event.getY() - ddInfo.mousePressedPos.y;
        view.getPositionManager().move(diffX, diffY);
        // ドラッグ検出されていない場合, 強調は行わない.
        // 子ノードがワークスペース直下にいないのに, 重なったノード (入れ替え候補) が検出されるのを防ぐ
        highlightOverlappedNode();
        trashCan.auto(event.getSceneX(), event.getSceneY());
      }
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /**
   * マウスドラッグを検出した時の処理.
   * 先に {@code onMouseDragged} が呼ばれ, ある程度ドラッグしたときにこれが呼ばれる.
   */
  private void onMouseDragDetected(MouseEventInfo info) {
    setUserData(info);
    MouseEvent event = info.event;
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton())) {
        return;
      }
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
      ddInfo.isDragDetected = true;
      if (model.isRemovable() && isInNodeMoveMode(info.event)) {
        // 子ノードでかつ取り外し可能 -> ワークスペースへ
        toWorkspace();
      }
      toFront();
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEventInfo info) {
    setUserData(info);
    MouseEvent event = info.event;
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }
    try {
      jumpToRelatedNode(info);
      if (ddInfo.forwardEvent) {
        invokeOnUiEventReceived(event, false);
        dispatchEvent(model.findParentNode(), info);
        return;
      }
      invokeOnUiEventReceived(event, true);
      if (ddInfo.currentOverlapped != null) {
        effectManager.setEffectEnabled(ddInfo.currentOverlapped, false, OVERLAP);
        // ワークスペース -> 子ノード
        ddInfo.currentOverlapped.getModel().ifPresent(this::toChildNode);
      } else {
        //同一ワークスペース上で移動
        toSameWorkspace();
      }
      deleteTrashedNode();
      pushViewpointChangeCmd(info);
    } finally {
      terminateDnd(info);
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
      var len = BhConstants.Ui.REPLACED_NODE_SHIFT;
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
    if (ddInfo.isDragDetected && model.isRoot()) {
      ViewUtil.pushReverseMoveCmd(view, ddInfo.posOnWorkspace, ddInfo.context.userOpe());
    }
  }

  /** ゴミ箱に入れられたノードを削除する. */
  private void deleteTrashedNode() {
    if (model.isRoot() && trashCan.isOpened()) {
      UserOperation userOpe = ddInfo.context.userOpe();
      boolean canDelete = model.getEventInvoker().onDeletionRequested(
          new ArrayList<>() {{ add(model); }}, CauseOfDeletion.TRASH_CAN, userOpe);
      if (!canDelete) {
        return;
      }
      SequencedSet<Swapped> swappedNodes = BhNodePlacer.deleteNode(model, true, userOpe);
      for (Swapped swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            userOpe);
      }
    }
  }

  /** view と重なっている BhNodeView を強調する. */
  private void highlightOverlappedNode() {
    if (ddInfo.currentOverlapped != null) {
      // 前回重なっていたノードの強調を止める
      effectManager.setEffectEnabled(ddInfo.currentOverlapped, false, OVERLAP);
    }
    ddInfo.currentOverlapped = null;
    List<BhNodeView> overlappedList = view.getRegionManager().searchForOverlapped();
    for (BhNodeView overlapped : overlappedList) {
      if (overlapped.getModel().map(node -> node.canBeReplacedWith(model)).orElse(false)) {
        effectManager.setEffectEnabled(overlapped, true, OVERLAP);
        ddInfo.currentOverlapped = overlapped;
        break;
      }
    }
  }

  /** {@link #model} と関連のあるノードを強調する. */
  private void highlightRelatedNodes(MouseEventInfo info) {
    var commonEventInfo = (CommonEventInfo) info.getUserData();
    if (BhSettings.Ui.nodeManipMode != NodeManipulationMode.MODE_2
        || info.event.getButton() != MouseButton.PRIMARY
        || info.getRootEventInfo().view.isTemplate()
        || commonEventInfo.isRelatedNodeHighlighted) {
      return;
    }
    Collection<BhNode> relatedNodes = model.getEventInvoker().onRelatedNodesRequired();
    commonEventInfo.isRelatedNodeHighlighted = !relatedNodes.isEmpty();
    relatedNodes.stream()
        .filter(node -> node.getView().isPresent())
        .forEach(node -> effectManager.setEffectEnabled(
            node.getView().get(), true, RELATED_NODE_GROUP, ddInfo.context.userOpe()));
  }

  /** ノードの選択処理を行う. */
  private void selectNode(MouseEvent event) {
    UserOperation userOpe = ddInfo.context.userOpe();
    switch (BhSettings.Ui.nodeManipMode) {
      case NodeManipulationMode.MODE_0 -> {
        if (event.getButton() == MouseButton.PRIMARY) {
          model.getWorkspace().getSelectedNodes().forEach(selected -> selected.deselect(userOpe));
          model.select(userOpe);
        }
      }
      case NodeManipulationMode.MODE_1 -> {
        if (event.getButton() == MouseButton.PRIMARY) {
          model.select(userOpe);
        } else if (event.getButton() == MouseButton.SECONDARY) {
          model.deselect(userOpe);
        }
      }
      default -> { /* Do nothing. */ }
    }
  }

  /**
   * {@code node} に対応するビューに {@code event} を送る.
   *
   * @param node このノードのビューにイベントを送る
   * @param info {@code node} に送るイベント
   */
  private static void dispatchEvent(BhNode node, MouseEventInfo info) {
    Optional.ofNullable(node)
        .flatMap(BhNode::getView)
        .ifPresent(view -> view.getCallbackRegistry().forward(info));
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd(MouseEventInfo info) {
    trashCan.close();
    mouseCtrlLock.unlock();
    ddInfo = new DndEventInfo();
    commonEventInfo = new CommonEventInfo(false, false, false);
    endTransaction(info);
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

  /** {@link #model} にブレークポイントを設定すべきである場合, 設定する. */
  private void setBreakpoint(MouseEventInfo info) {
    var commonEventInfo = (CommonEventInfo) info.getUserData();
    if (!BhSettings.Debug.canSetBreakpoint
        || BhSettings.Ui.nodeManipMode != NodeManipulationMode.MODE_0
        || info.event.getButton() != MouseButton.SECONDARY
        || commonEventInfo.isBreakpointSet) {
      return;
    }
    model.findBreakpointGroupLeader().ifPresent(
        node -> {
          node.setBreakpoint(!node.isBreakpointSet(), ddInfo.context.userOpe());
          commonEventInfo.isBreakpointSet = true;
        });
  }

  /** {@link #model} に関連するノードがある場合, そのノードにジャンプする. */
  private void jumpToRelatedNode(MouseEventInfo info) {
    var commonEventInfo = (CommonEventInfo) info.getUserData();
    if (BhSettings.Ui.nodeManipMode != NodeManipulationMode.MODE_2
        || info.event.getButton() != MouseButton.PRIMARY
        || info.event.getClickCount() != 2
        || commonEventInfo.isJumpTargetSet) {
      return;
    }
    BhNode jumpTarget = model.getEventInvoker().onJumpTargetRequired();
    if (jumpTarget != null) {
      commonEventInfo.isJumpTargetSet = true;
      Platform.runLater(() -> jumpTarget.getView().ifPresent(ViewUtil::jump));
    }
  }

  /** {@link #model} が UI イベントを受け取ったときのフック処理を呼ぶ. */
  private void invokeOnUiEventReceived(MouseEvent event, boolean isDndTarget) {
    UiEvent uiEvent = toEventInfo(event, isDndTarget);
    model.getEventInvoker().onUiEventReceived(uiEvent, ddInfo.context.userOpe());
  }

  /** {@link MouseEvent} オブジェクトの情報を {@link UiEvent} オブジェクトに格納して返す. */
  private UiEvent toEventInfo(MouseEvent event, boolean isDndTarget) {
    UiEventType eventType;
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
        BhSettings.Ui.nodeManipMode,
        event.getButton() == MouseButton.PRIMARY,
        event.getButton() == MouseButton.SECONDARY,
        event.getButton() == MouseButton.MIDDLE,
        event.getButton() == MouseButton.BACK,
        event.getButton() == MouseButton.FORWARD,
        event.isShiftDown(),
        event.isControlDown(),
        event.isAltDown(),
        isDndTarget,
        event.getClickCount());
  }

  /** D&D 操作に必要な位置を {@link #ddInfo} に保存する. */
  private void savePositions(MouseEvent event) {
    Vec2D mousePressedPos = new Vec2D(event.getSceneX(), event.getSceneY());
    ddInfo.mousePressedPos = view.getPositionManager().sceneToLocal(mousePressedPos);
    ddInfo.posOnWorkspace = view.getPositionManager().getPosOnWorkspace();
    ddInfo.nodeLocation = new BhNodeLocation(model);
  }

  /** ワークスペース上の注視点を変更するコマンドを追加する. */
  private void pushViewpointChangeCmd(MouseEventInfo info) {
    BiFunction<WorkspaceView, Vec2D, Boolean> fnShouldChange
        = (wsView, pos) -> wsView.getWorkspace().isCurrentWorkspace()
        ? BhSettings.Ui.trackNodeInCurrentWorkspace : BhSettings.Ui.trackNodeInInactiveWorkspace;
    var oldLocation =
        info.getRootEventInfo().view.isTemplate() ? new BhNodeLocation() : ddInfo.nodeLocation;
    var newLocation = model.isDeleted() ? new BhNodeLocation() : new BhNodeLocation(model);
    ViewUtil.pushViewpointChangeCmd(
        oldLocation.wsView,
        oldLocation.center,
        newLocation.wsView,
        newLocation.center,
        fnShouldChange,
        ddInfo.context.userOpe());
  }

  /** {@link MouseEventInfo} に {@link DefaultBhNodeController} が共通で参照するデータを格納する. */
  private void setUserData(MouseEventInfo info) {
    MouseEventInfo src = info.src;
    while (src != null) {
      if (src.getUserData() instanceof CommonEventInfo common) {
        info.setUserData(common);
        return;
      }
      src = src.src;
    }
    info.setUserData(commonEventInfo);
  }

  /** {@link TransactionNotificationService} にトランザクションの開始を通知する. */
  private TransactionContext beginTransaction(MouseEventInfo info) {
    if (info.src == null) {
      return notifService.begin(ExclusionId.NODE_MANIPULATION).orElse(null);
    } else {
      return notifService.begin();
    }
  }

  /** {@link TransactionNotificationService} にトランザクションの終了を通知する. */
  private void endTransaction(MouseEventInfo info) {
    if (info.src == null) {
      notifService.end(ExclusionId.NODE_MANIPULATION);
    } else {
      notifService.end();
    }
  }

  /** 現在のノード操作モードがノードの移動をサポートするモードか調べる. */
  private boolean isInNodeMoveMode(MouseEvent event) {
    return (BhSettings.Ui.nodeManipMode == NodeManipulationMode.MODE_0
        || BhSettings.Ui.nodeManipMode == NodeManipulationMode.MODE_1)
        && event.getButton() == MouseButton.PRIMARY;
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
  public TransactionNotificationService getNotificationService() {
    return notifService;
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private static class DndEventInfo {
    private Vec2D mousePressedPos = new Vec2D();
    private Vec2D posOnWorkspace = new Vec2D();
    private BhNodeLocation nodeLocation = new BhNodeLocation();
    /** 現在重なっている View. */
    private BhNodeView currentOverlapped = null;
    /** イベントを親ノードに転送する場合 true. */
    private boolean forwardEvent = false;
    /** マウスドラッグを検出済みのとき true. */
    private boolean isDragDetected = false;
    /** {@code model} に最後につながっていた親ノード. */
    private ConnectiveNode latestParent = null;
    /** {@code latestParent} のルートノード. */
    private BhNode latestRoot = null;
    /** モデルの操作に伴うコンテキスト. */
    private TransactionContext context;
  }

  /**
   * 複数の {@link DefaultBhNodeController} 間で共有されるイベント情報を保持するクラス.
   */
  private static class CommonEventInfo {
    /** 今回の操作で関連するノードの強調表示が完了している場合 true. */
    private boolean isRelatedNodeHighlighted;
    /** 今回の操作でブレークポイントを設定済みの場合 true. */
    private boolean isBreakpointSet;
    /** 今回の操作でジャンプ先の指定を行っている場合 true. */
    private boolean isJumpTargetSet;

    /** コンストラクタ. */
    public CommonEventInfo(
        boolean isRelatedNodeHighlighted, boolean isBreakpointSet, boolean isJumpTargetSet) {
      this.isRelatedNodeHighlighted = isRelatedNodeHighlighted;
      this.isBreakpointSet = isBreakpointSet;
      this.isJumpTargetSet = isJumpTargetSet;
    }
  }
}
