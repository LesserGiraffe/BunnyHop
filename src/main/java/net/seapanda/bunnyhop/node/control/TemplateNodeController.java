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

import java.util.Objects;
import java.util.Optional;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeMvcBuilder;
import net.seapanda.bunnyhop.node.model.factory.MvcType;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.MouseEventInfo;
import net.seapanda.bunnyhop.nodeselection.control.BhNodeSelectionViewManager;
import net.seapanda.bunnyhop.service.accesscontrol.ExclusiveTask;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionContext;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionManager;
import net.seapanda.bunnyhop.ui.control.MouseCtrlLock;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * ノード選択リストにあるノードのコントローラ.
 *
 * @author K.Koike
 */
public class TemplateNodeController implements BhNodeController {

  private final BhNode model;
  private final BhNodeView view;
  private final TransactionManager txnManager;
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock(MouseButton.PRIMARY);
  private final BhNodeMvcBuilder mvcBuilder;
  private final WorkspaceSet wss;
  private final BhNodeSelectionViewManager nodeSelViewManager;
  private DndEventInfo ddInfo = new DndEventInfo();

  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   * @param mvcBuilder ノード生成用オブジェクト
   * @param txnManager トランザクションを管理するオブジェクト
   * @param wss アプリケーション固有のワークスペースセット
   * @param nodeSelViewManager ノード選択ビューを操作するためのオブジェクト
   */
  public TemplateNodeController(
      BhNode model,
      BhNodeView view,
      BhNodeMvcBuilder mvcBuilder,
      TransactionManager txnManager,
      WorkspaceSet wss,
      BhNodeSelectionViewManager nodeSelViewManager) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(mvcBuilder);
    Objects.requireNonNull(txnManager);
    Objects.requireNonNull(wss);
    Objects.requireNonNull(nodeSelViewManager);
    this.model = model;
    this.view = view;
    this.mvcBuilder = mvcBuilder;
    this.txnManager = txnManager;
    this.wss = wss;
    this.nodeSelViewManager = nodeSelViewManager;
    model.setView(view);
    view.setController(this);
    setEventHandlers();
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    BhNodeView.CallbackRegistry registry = view.getCallbackRegistry();
    registry.getOnMousePressed().setFirst(this::onMousePressed);
    registry.getOnMouseDragged().setFirst(this::onMouseDragged);
    registry.getOnMouseDragDetected().setFirst(this::onMouseDragDetected);
    registry.getOnMouseReleased().setLast(this::onMouseReleased);
    model.getCallbackRegistry().getOnConnected().add(event -> replaceView(event.disconnected()));
  }

  /** マウスボタン押下時のイベントハンドラ. */
  private void onMousePressed(MouseEventInfo info) {
    try {
      MouseEvent event = info.event;
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      ddInfo.context = beginTransaction(info);
      if (ddInfo.context == null) {
        mouseCtrlLock.unlock();
        return;
      }
      Workspace currentWs = wss.getCurrentWorkspace();
      BhNode newNode = model.findRootNode().copy(ddInfo.context.userOpe());
      ddInfo.currentView = mvcBuilder.build(newNode, MvcType.DEFAULT);
      if (currentWs == null || ddInfo.currentView == null) {
        terminateDnd(info);
        return;
      }

      Vec2D position = calcClickPosition(event, currentWs);
      BhNodePlacer.moveToWs(currentWs, newNode, position.x, position.y, ddInfo.context.userOpe());
      ddInfo.currentView.getCallbackRegistry().forward(info);
      nodeSelViewManager.hideCurrentView();
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /** マウスドラッグ時のイベントハンドラ. */
  private void onMouseDragged(MouseEventInfo info) {
    try {
      MouseEvent event = info.event;
      if (!mouseCtrlLock.isLockedBy(event.getButton())) {
        return;
      }
      ddInfo.currentView.getCallbackRegistry().forward(info);
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /** マウスドラッグ検出検出時のイベントハンドラ. */
  private void onMouseDragDetected(MouseEventInfo info) {
    try {
      MouseEvent event = info.event;
      if (!mouseCtrlLock.isLockedBy(event.getButton())) {
        return;
      }
      ddInfo.currentView.getCallbackRegistry().forward(info);
    } catch (Throwable e) {
      terminateDnd(info);
      throw e;
    }
  }

  /** マウスボタンを離したときのイベントハンドラ. */
  private void onMouseReleased(MouseEventInfo info) {
    MouseEvent event = info.event;
    if (!mouseCtrlLock.isLockedBy(event.getButton())) {
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }

    try {
      ddInfo.currentView.getCallbackRegistry().forward(info);
    } finally {
      terminateDnd(info);
    }
  }

  /** クリックされたノードからの相対クリック位置をワークスペース上の位置に変換する. */
  private Vec2D calcClickPosition(MouseEvent event, Workspace currentWs) {
    // クリックされたテンプレートノードのルートノード上でのクリック位置
    Vec2D posOnRootView = calcRelativePosFromRoot();
    posOnRootView.add(event.getX(), event.getY());
    var posOnScene = new Vec2D(event.getSceneX(), event.getSceneY());
    Vec2D posOnWs = currentWs.getView()
        .map(wsv -> wsv.sceneToWorkspace(posOnScene))
        .orElse(new Vec2D());
    posOnWs.sub(posOnRootView);
    return posOnWs;
  }

  /** view の rootView からの相対位置を求める. */
  private Vec2D calcRelativePosFromRoot() {
    Vec2D pos = view.getGeometry().localToScene(new Vec2D(0, 0));
    return view.getTreeControl().getRootView().getGeometry().sceneToLocal(pos);
  }

  /** {@link #view} と {@code oldNode} のノードビューを入れ替える. */
  private void replaceView(BhNode oldNode) {
    Optional.ofNullable(oldNode)
        .flatMap(BhNode::getView)
        .ifPresent(oldView -> oldView.getTreeControl().replace(view));
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd(MouseEventInfo info) {
    mouseCtrlLock.unlock();
    endTransaction(info, ddInfo.context);
    ddInfo = new DndEventInfo();
  }

  /** トランザクションを開始する. */
  private TransactionContext beginTransaction(MouseEventInfo info) {
    TransactionContext context = txnManager.begin();
    if (info.src == null && !context.lockTasks(ExclusiveTask.NODE_TREE_MODIFICATION)) {
      txnManager.end();
      return null;
    }
    return context;
  }

  /** トランザクションを終了する. */
  private void endTransaction(MouseEventInfo info, TransactionContext context) {
    if (info.src == null) {
      context.releaseTasks(ExclusiveTask.NODE_TREE_MODIFICATION);
    }
    txnManager.end();
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
  public TransactionManager getTransactionManager() {
    return txnManager;
  }

  /**
   * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
   */
  private static class DndEventInfo {
    /** 現在 テンプレートの BhNodeView 上で発生したマウスイベントを送っているワークスペース上の view. */
    private BhNodeView currentView = null;
    /** ノードの作成に伴うコンテキスト. */
    private TransactionContext context;
  }
}
