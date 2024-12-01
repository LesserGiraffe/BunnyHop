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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.command.BhCmd;
import net.seapanda.bunnyhop.command.CmdData;
import net.seapanda.bunnyhop.command.CmdProcessor;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースとそれに関連するビューのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceController implements CmdProcessor {

  private Workspace model;
  private WorkspaceView view;
  private MultiNodeShifterController nodeShifterController;

  /** コンストラクタ. */
  public WorkspaceController() {}

  /**
   * コンストラクタ.
   *
   * @param model 操作対象のモデル
   * @param view 操作対象のするビュー
   * @param nodeShifterView 操作対象のノードシフタビュー
   */
  public WorkspaceController(
      Workspace model, WorkspaceView view, MultiNodeShifterView nodeShifterView) {
    this.model = model;
    this.view = view;
    view.addtMultiNodeShifterView(nodeShifterView);
    nodeShifterController = new MultiNodeShifterController(nodeShifterView, model);
    setEventHandlers();
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    Vec2D mousePressedPos = new Vec2D(0.0, 0.0);
    view.setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent, mousePressedPos));
    view.setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent, mousePressedPos));
    view.setOnMouseReleased(mouseEvent -> onMouseReleased(mousePressedPos, mouseEvent));
  }

  /** 
   * マウスボタン押下時の処理.
   *
   * @param mouseEvent 発生したマウスイベント
   * @param mousePressedPos マウスボタン押下時のカーソル位置の格納先
   */
  private void onMousePressed(MouseEvent mouseEvent, Vec2D mousePressedPos) {
    // ワークスペースをクリックしたときにテキストフィールドのカーソルが消えなくなるので, マウスイベントを consume しない.
    if (!mouseEvent.isShiftDown()) {
      UserOperation userOpe = new UserOperation();
      BhService.bhNodeSelectionService().hideAll();
      model.clearSelectedNodeList(userOpe);
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    }
    BhNodeView.LookManager.removeShadow(model);
    mousePressedPos.x = mouseEvent.getX();
    mousePressedPos.y = mouseEvent.getY();
    view.showSelectionRectangle(mousePressedPos, mousePressedPos);
  }

  /**
   * マウスドラッグ時のイベントハンドラを登録する.
   *
   * @param mouseEvent 発生したマウスイベント
   * @param mousePressedPos マウスボタン押下時のカーソル位置
   */
  private void onMouseDragged(MouseEvent mouseEvent, Vec2D mousePressedPos) {
    view.showSelectionRectangle(
        mousePressedPos, new Vec2D(mouseEvent.getX(), mouseEvent.getY()));
  }

  /**
   * マウスボタンを離したときの処理.
   *
   * @param mouseEvent 発生したマウスイベント
   * @param mousePressedPos マウスボタンを押下時のカーソル位置
   */
  private void onMouseReleased(Vec2D mousePressedPos, MouseEvent mouseEvent) {
    view.hideSelectionRectangle();
    double minX = Math.min(mousePressedPos.x, mouseEvent.getX());
    double minY = Math.min(mousePressedPos.y, mouseEvent.getY());
    double maxX = Math.max(mousePressedPos.x, mouseEvent.getX());
    double maxY = Math.max(mousePressedPos.y, mouseEvent.getY());
    var selectionRange = new QuadTreeRectangle(minX, minY, maxX, maxY, null);
    List<BhNodeView> containedNodes =
        view.searchForOverlappedNodeViews(selectionRange, true, OverlapOption.CONTAIN).stream()
        .filter(WorkspaceController::isNodeSelectable)
        .collect(Collectors.toCollection(ArrayList::new));
    // 面積の大きい順にソート
    containedNodes.sort(this::compareViewSize);
    ModelExclusiveControl.lockForModification();
    try {
      var userOpe = new UserOperation();
      selectNodes(containedNodes, userOpe);
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** {@code view} が矩形選択可能かどうか調べる. */
  private static boolean isNodeSelectable(BhNodeView view) {
    return view.getModel().map(model -> model.isMovable() && !model.isSelected()).orElse(false);
  }

  private int compareViewSize(BhNodeView viewA, BhNodeView viewB) {
    Vec2D sizeA = viewA.getRegionManager().getBodySize(false);
    double areaA = sizeA.x * sizeA.y;
    Vec2D sizeB = viewB.getRegionManager().getBodySize(false);
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
      model.addSelectedNode(larger.getModel().get(), userOpe);  // ノード選択
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

  /**
   * メッセージ受信.
   *
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するもの
   * */
  @Override
  public CmdData process(BhCmd msg, CmdData data) {
    switch (msg) {
      case ADD_ROOT_NODE:
        model.addRootNode(data.node);
        view.addNodeView(data.nodeView);
        nodeShifterController.updateMultiNodeShifter(data.node);
        break;

      case REMOVE_ROOT_NODE:
        model.removeRootNode(data.node);
        view.removeNodeView(data.nodeView);
        nodeShifterController.updateMultiNodeShifter(data.node);
        break;

      case SET_QT_RECTANGLE:
        view.addRectangleToQtSpace(data.nodeView, data.userOpe);
        break;

      case CHANGE_WORKSPACE_VIEW_SIZE:
        view.changeWorkspaceViewSize(data.bool);
        break;

      case SCENE_TO_WORKSPACE:
        javafx.geometry.Point2D pos = view.sceneToWorkspace(data.vec2d.x, data.vec2d.y);
        return new CmdData(new Vec2D(pos.getX(), pos.getY()));

      case ZOOM:
        view.zoom(data.bool);
        break;

      case SET_ZOOM_LEVEL:
        view.setZoomLevel(data.integer);
        break;

      case GET_WORKSPACE_SIZE:
        Vec2D size = view.getWorkspaceSize();
        return new CmdData(new Vec2D(size.x, size.y));

      case ADD_WORKSPACE:
        return new CmdData(model, view, data.userOpe);

      case DELETE_WORKSPACE:
        return deleteWorkspace(data);

      case UPDATE_MULTI_NODE_SHIFTER:
        nodeShifterController.updateMultiNodeShifter(data.node);
        break;

      case LOOK_AT_NODE_VIEW:
        view.lookAt(data.nodeView);
        break;

      default:
        throw new AssertionError("receive an unknown msg " + msg);
    }
    return null;
  }

  private CmdData deleteWorkspace(CmdData data) {
    Collection<BhNode> rootNodes = model.getRootNodeList();
    rootNodes.forEach(node -> node.getEventAgent().execOnDeletionRequested(
        rootNodes, CauseOfDeletion.WORKSPACE_DELETION, data.userOpe));
    List<Swapped> swappedNodes =
        BhService.bhNodePlacer().deleteNodes(model.getRootNodeList(), data.userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          data.userOpe);
    }
    return new CmdData(model, view, data.userOpe);
  }

  //デバッグ用
  private void printDebugInfo() {
    //4 分木登録ノード数表示
    Class<WorkspaceView> c = WorkspaceView.class;
    Field f = null;
    try {
      f = c.getDeclaredField("quadTreeMngForConnector");
      f.setAccessible(true);
      QuadTreeManager quadTreeMngForConnector = (QuadTreeManager) f.get(view);
      BhService.msgPrinter().println(
          "num of QuadTreeNodes: " + quadTreeMngForConnector.calcRegisteredNodeNum());
    } catch (IllegalAccessException
        | IllegalArgumentException
        | NoSuchFieldException
        | SecurityException e) {
      BhService.msgPrinter().errForDebug(e.toString());
    }
    BhService.msgPrinter().println("num of root nodes: " + model.getRootNodeList().size());
    BhService.msgPrinter().println(
        "num of selected nodes: " + model.getSelectedNodeList().size());
  }
}
