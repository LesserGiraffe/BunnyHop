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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.view.workspace.NodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

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
  private final DndEventInfo ddInfo = new DndEventInfo();
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();
  private final BhNodeSelectionViewProxy nodeSelectionViewProxy;
  private final MessageService msgService;

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
    view.getEventManager().addOnMousePressed(this::onMousePressed);
    view.getEventManager().addOnMouseDragged(this::onMouseDragged);
    view.getEventManager().addOnMouseReleased(this::onMouseReleased);
    view.getEventManager().setOnCloseRequest(this::onCloseRequest);
    view.getEventManager().addOnClosed(this::onClosed);
  }

  private void setWorkspaceEventHandlers() {
    Workspace.CallbackRegistry registry = model.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> addNodeView(event.node()));
    registry.getOnNodeRemoved().add(event -> removeNodeView(event.node()));
    registry.getOnRootNodeAdded().add(event -> speficyNodeViewAsRoot(event.node()));
    registry.getOnRootNodeRemoved().add(event -> speficyNodeViewAsNotRoot(event.node()));
  }

  /** マウスボタン押下時の処理. */
  private void onMousePressed(MouseEvent event) {
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = notifService.begin();   
      ddInfo.isDndFinished = false;
      if (!event.isShiftDown()) {
        nodeSelectionViewProxy.hideAll();
        model.getSelectedNodes().forEach(node -> node.deselect(ddInfo.context.userOpe()));
      }
      view.getRootNodeViews().forEach(nodeView -> nodeView.getLookManager().hideShadow(false));
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
      event.consume();
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
    while (nodesToSelect.size() != 0) {
      BhNodeView larger = nodesToSelect.pop();
      larger.getModel().get().select(userOpe); // ノード選択
      var iter = nodesToSelect.iterator();
      while (iter.hasNext()) {
        BhNodeView smaller = iter.next();
        // 子孫 - 先祖関係にあってかつ領域が包含関係にある -> 矩形選択の対象としない
        if (larger.getRegionManager().overlapsWith(smaller, OverlapOption.CONTAIN)
            && smaller.getModel().get().isDescendantOf(larger.getModel().get())) {
          iter.remove();
        }
      }
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
  private void speficyNodeViewAsRoot(BhNode node) {
    node.getView().ifPresent(view::specifyNodeViewAsRoot);
  }

  /** {@code node} を非ルートノードとしてワークスペースビューに設定する. */
  private void speficyNodeViewAsNotRoot(BhNode node) {
    node.getView().ifPresent(view::specifyNodeViewAsNotRoot);
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    mouseCtrlLock.unlock();
    ddInfo.reset();
    notifService.end();
  }

  /** ワークスペースビューの削除命令を受けた時の処理. */
  private boolean onCloseRequest() {
    // 空のワークスペースビュー削除時は警告なし
    if (model.getRootNodes().isEmpty()) {
      return true;
    }
    Optional<ButtonType> buttonType = msgService.alert(
        Alert.AlertType.CONFIRMATION,
        TextDefs.Workspace.AskIfDeleteWs.title.get(),
        null,
        TextDefs.Workspace.AskIfDeleteWs.body.get(model.getName()));
    return buttonType.map(type -> type.equals(ButtonType.OK)).orElse(false);
  }

  /** ワークスペースビュー削除時の処理. */
  private void onClosed() {
    Context context = notifService.begin();
    try {
      WorkspaceSet wss = model.getWorkspaceSet();
      if (wss != null) {
        wss.removeWorkspace(model, context.userOpe());
      }
    } finally {
      notifService.end();
    }
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private class DndEventInfo {
    Vec2D mousePressedPos = null;
    /** モデルの操作に伴うコンテキスト. */
    ModelAccessNotificationService.Context context;
    /** D&D が終了しているかどうかのフラグ. */
    private boolean isDndFinished = true;

    /** D&Dイベント情報を初期化する. */
    private void reset() {
      mousePressedPos = null;
      context = null;
      isDndFinished = true;
    }
  }
}
