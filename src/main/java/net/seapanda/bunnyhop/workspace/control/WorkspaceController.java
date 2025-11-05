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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.LookManager.EffectTarget;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.control.MouseCtrlLock;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.view.NodeShifterView;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle.OverlapOption;

/**
 * ワークスペースとそれに関連するビューのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceController {

  private Workspace model;
  private WorkspaceView view;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final ModelAccessNotificationService notifService;
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock(MouseButton.PRIMARY);
  private final BhNodeSelectionViewProxy nodeSelectionViewProxy;
  private final MessageService msgService;
  private DndEventInfo ddInfo = new DndEventInfo();

  /**
   * コンストラクタ.
   *
   * @param model 操作対象のモデル
   * @param view 操作対象のするビュー
   * @param nodeShifterView 操作対象のノードシフタビュー
   * @param notifService モデルへのアクセスの通知先となるオブジェクト
   * @param proxy ノード選択ビューのプロキシオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public WorkspaceController(
      Workspace model,
      WorkspaceView view,
      NodeShifterView nodeShifterView,
      ModelAccessNotificationService notifService,
      BhNodeSelectionViewProxy proxy,
      MessageService msgService) {
    this.model = model;
    this.view = view;
    this.notifService = notifService;
    this.nodeSelectionViewProxy = proxy;
    this.msgService = msgService;
    model.setView(view);
    view.addNodeShifterView(nodeShifterView);
    new NodeShifterController(nodeShifterView, model, notifService);
    setViewEventHandlers();
    setWorkspaceEventHandlers();
  }

  private void setViewEventHandlers() {
    WorkspaceView.CallbackRegistry registry = view.getCallbackRegistry();
    registry.getOnMousePressed().add(info -> onMousePressed(info.event()));
    registry.getOnMouseDragged().add(info -> onMouseDragged(info.event()));
    registry.getOnMouseReleased().add(info -> onMouseReleased(info.event()));
    registry.setOnCloseRequested(this::onCloseRequest);
    registry.getOnClosed().add(event -> onClosed());
  }

  private void setWorkspaceEventHandlers() {
    Workspace.CallbackRegistry registry = model.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> addNodeView(event.node()));
    registry.getOnNodeRemoved().add(event -> removeNodeView(event.node()));
    registry.getOnRootNodeAdded().add(event -> specifyNodeViewAsRoot(event.node()));
    registry.getOnRootNodeRemoved().add(event -> specifyNodeViewAsNotRoot(event.node()));
  }

  /** マウスボタン押下時の処理. */
  private void onMousePressed(MouseEvent event) {
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = notifService.beginWrite();
      ddInfo.isDndFinished = false;
      if (!event.isShiftDown()) {
        nodeSelectionViewProxy.hideCurrentView();
        model.getSelectedNodes().forEach(node -> node.deselect(ddInfo.context.userOpe()));
      }
      view.getRootNodeViews().forEach(
          nodeView -> nodeView.getLookManager().hideShadow(EffectTarget.CHILDREN));
      ddInfo.mousePressedPos = new Vec2D(event.getX(), event.getY());
      view.showSelectionRectangle(ddInfo.mousePressedPos, ddInfo.mousePressedPos);
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    }
    // ワークスペースをクリックしたときにテキストフィールドのカーソルが消えなくなるので, マウスイベントを consume しない.
  }

  /** マウスドラッグ時のイベントハンドラを登録する. */
  private void onMouseDragged(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      view.showSelectionRectangle(
          ddInfo.mousePressedPos, new Vec2D(event.getX(), event.getY()));
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    }
  }

  /** マウスボタンを離したときの処理. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }
    try {
      view.hideSelectionRectangle();
      double minX = Math.min(ddInfo.mousePressedPos.x, event.getX());
      double minY = Math.min(ddInfo.mousePressedPos.y, event.getY());
      double maxX = Math.max(ddInfo.mousePressedPos.x, event.getX());
      double maxY = Math.max(ddInfo.mousePressedPos.y, event.getY());
      var selectionRange = new QuadTreeRectangle(minX, minY, maxX, maxY, null);
      List<BhNodeView> containedNodes =
          view.searchForOverlappedNodeViews(selectionRange, true, OverlapOption.CONTAIN).stream()
          .filter(WorkspaceController::isNodeSelectable)
          .collect(Collectors.toCollection(ArrayList::new));
      // 面積の大きい順にソート
      containedNodes.sort(this::compareViewSize);
      selectNodes(containedNodes, ddInfo.context.userOpe());
    } finally {
      terminateDnd();
    }
  }

  /** {@code view} が矩形選択可能かどうか調べる. */
  private static boolean isNodeSelectable(BhNodeView view) {
    return view.getModel().map(model -> model.isMovable() && !model.isSelected()).orElse(false);
  }

  private int compareViewSize(BhNodeView viewA, BhNodeView viewB) {
    Vec2D sizeA = viewA.getRegionManager().getNodeSize(false);
    double areaA = sizeA.x * sizeA.y;
    Vec2D sizeB = viewB.getRegionManager().getNodeSize(false);
    double areaB = sizeB.x * sizeB.y;
    if (areaA < areaB) {
      return 1;
    } else if (areaA > areaB) {
      return -1;
    }
    return 0;
  }

  /**
   * 矩形選択するノードを選び出す.
   *
   * @param candidates 矩形選択される候補ノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void selectNodes(List<BhNodeView> candidates, UserOperation userOpe) {
    // 親ノードが選択候補でかつ, 親ノードのボディの領域に包含されているノードは選択対象としない.
    LinkedList<BhNodeView> nodesToSelect = new LinkedList<>(candidates);
    while (!nodesToSelect.isEmpty()) {
      BhNodeView larger = nodesToSelect.pop();
      larger.getModel().get().select(userOpe); // ノード選択
      // 子孫 - 先祖関係にあってかつ領域が包含関係にある -> 矩形選択の対象としない
      nodesToSelect.removeIf(
          smaller -> larger.getRegionManager().overlapsWith(smaller, OverlapOption.CONTAIN)
            && smaller.getModel().get().isDescendantOf(larger.getModel().get()));
    }
  }

  /** {@code node} のノードビューを {@link #view} に追加する. */
  private void addNodeView(BhNode node) {
    node.getView().ifPresent(view::addNodeView);
  }

  /** {@code node} のノードビューを {@link #view} から削除する. */
  private void removeNodeView(BhNode node) {
    node.getView().ifPresent(view::removeNodeView);
  }

  /** {@code node} をルートノードとしてワークスペースビューに設定する. */
  private void specifyNodeViewAsRoot(BhNode node) {
    node.getView().ifPresent(view::specifyNodeViewAsRoot);
  }

  /** {@code node} を非ルートノードとしてワークスペースビューに設定する. */
  private void specifyNodeViewAsNotRoot(BhNode node) {
    node.getView().ifPresent(view::specifyNodeViewAsNotRoot);
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    mouseCtrlLock.unlock();
    ddInfo = new DndEventInfo();
    notifService.endWrite();
  }

  /** ワークスペースビューの削除命令を受けた時の処理. */
  private boolean onCloseRequest() {
    // 空のワークスペースビュー削除時は警告なし
    if (model.getRootNodes().isEmpty()) {
      return true;
    }
    // ワークスペースタブの削除確認ダイアログはマウスイベントを起点とするイベントハンドラの中で表示される.
    // マウスイベントを起点とするイベントハンドラ内で通常のダイアログウィンドウを表示すると, その後のマウスイベントが正常に取得できない.
    // 例えば, ノードをドラッグしても Drag Detected イベントが発生しないなどの現象が発生する.
    // しかし, Modality.NONE を指定してダイアログを表示するとこの不具合を回避可能なので, ここではそれを使用する.
    Optional<ButtonType> buttonType = msgService.alert(
        Alert.AlertType.CONFIRMATION,
        Modality.NONE,
        TextDefs.Workspace.AskIfDeleteWs.title.get(),
        null,
        TextDefs.Workspace.AskIfDeleteWs.body.get(model.getName()));
    return buttonType.map(type -> type.equals(ButtonType.OK)).orElse(false);
  }

  /** ワークスペースビュー削除時の処理. */
  private void onClosed() {
    Context context = notifService.beginWrite();
    try {
      WorkspaceSet wss = model.getWorkspaceSet();
      if (wss != null) {
        wss.removeWorkspace(model, context.userOpe());
      }
    } finally {
      notifService.endWrite();
    }
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private static class DndEventInfo {
    Vec2D mousePressedPos = new Vec2D();
    /** モデルの操作に伴うコンテキスト. */
    ModelAccessNotificationService.Context context = null;
    /** D&D が終了しているかどうかのフラグ. */
    private boolean isDndFinished = true;
  }
}
