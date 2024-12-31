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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.proxy.WorkspaceViewProxy;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースとそれに関連するビューのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceController {

  private Workspace model;
  private WorkspaceView view;

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
    model.setViewProxy(new WorkspaceViewProxyImpl());
    view.addtMultiNodeShifterView(nodeShifterView);
    new MultiNodeShifterController(nodeShifterView, model);
    setEventHandlers();
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    Vec2D mousePressedPos = new Vec2D(0.0, 0.0);
    view.addOnMousePressed(mouseEvent -> onMousePressed(mouseEvent, mousePressedPos));
    view.addOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent, mousePressedPos));
    view.addOnMouseReleased(mouseEvent -> onMouseReleased(mousePressedPos, mouseEvent));
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
      BhService.getAppRoot().getNodeSelectionViewProxy().hideAll();
      UserOperation userOpe = new UserOperation();
      for (BhNode selectedNode : model.getSelectedNodes()) {
        selectedNode.deselect(userOpe);
      }
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
      UserOperation userOpe = new UserOperation();
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
    BhService.msgPrinter().println("num of root nodes: " + model.getRootNodes().size());
    BhService.msgPrinter().println(
        "num of selected nodes: " + model.getSelectedNodes().size());
  }

  private class WorkspaceViewProxyImpl implements WorkspaceViewProxy {

    @Override
    public WorkspaceView getView() {
      return view;
    }

    @Override
    public void notifyNodeSpecifiedAsRoot(BhNode node) {
      BhNodeView nodeView = node.getViewProxy().getView();
      if (nodeView != null) {
        view.specifyNodeViewAsRoot(nodeView);
      }
    }

    @Override
    public void notifyNodeSpecifiedAsNotRoot(BhNode node) {
      BhNodeView nodeView = node.getViewProxy().getView();
      if (nodeView != null) {
        view.specifyNodeViewAsNotRoot(nodeView);
      }
    }

    @Override
    public void notifyNodeAdded(BhNode node) {
      BhNodeView nodeView = node.getViewProxy().getView();
      if (nodeView != null) {
        view.addNodeView(nodeView);
      }
    }

    @Override
    public void notifyNodeRemoved(BhNode node) {
      BhNodeView nodeView = node.getViewProxy().getView();
      if (nodeView != null) {
        view.removeNodeView(nodeView);
      }
    }

    @Override
    public void changeViewSize(boolean widen) {
      view.changeViewSize(widen);
    }

    @Override
    public Vec2D getViewSize() {
      return view.getWorkspaceSize();
    }

    @Override
    public Vec2D sceneToWorkspace(Vec2D posOnScene) {
      if (posOnScene == null) {
        return null;
      }
      return view.sceneToWorkspace(posOnScene.x, posOnScene.y);
    }

    @Override
    public void zoom(boolean zoomIn) {
      view.zoom(zoomIn);
    }

    @Override
    public void setZoomLevel(int level) {
      view.setZoomLevel(level);
    }
  }
}
