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

import java.util.Objects;
import java.util.Optional;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

/**
 * ノード選択リストにあるノードのコントローラ.
 *
 * @author K.Koike
 */
public class TemplateNodeController implements BhNodeController {

  private final BhNode model;
  private final BhNodeView view;
  private final ModelAccessNotificationService notifService;
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();
  private final DndEventInfo ddInfo = this.new DndEventInfo();
  private final BhNodeFactory factory;
  private final WorkspaceSet wss;
  private final BhNodeSelectionViewProxy nodeSelectionViewProxy;
  
  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   * @param factory ノード生成用オブジェクト
   * @param service モデルへのアクセスの通知先となるオブジェクト
   * @param wss アプリケーション固有のワークスペースセット
   * @param proxy ノード選択ビュー
   */
  public TemplateNodeController(
      BhNode model,
      BhNodeView view,
      BhNodeFactory factory,
      ModelAccessNotificationService service,
      WorkspaceSet wss,
      BhNodeSelectionViewProxy proxy) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(factory);
    Objects.requireNonNull(service);
    Objects.requireNonNull(wss);
    Objects.requireNonNull(proxy);
    this.model = model;
    this.view = view;
    this.factory = factory;
    this.notifService = service;
    this.wss = wss;
    this.nodeSelectionViewProxy = proxy;
    model.setView(view);
    view.setController(this);
    setEventHandlers();
  }

  /** 各種イベントハンドラをセットする. */
  private void setEventHandlers() {
    view.getEventManager().setOnMousePressed(this::onMousePressed);
    view.getEventManager().setOnMouseDragged(this::onMouseDragged);
    view.getEventManager().setOnDragDetected(this::onDragDetected);
    view.getEventManager().setOnMouseReleased(this::onMouseReleased);
    view.getEventManager().addEventFilter(
        MouseEvent.ANY,
        mouseEvent -> consumeIfNotAcceptable(mouseEvent));
    model.getCallbackRegistry().getOnConnected().add(event -> replaceView(event.disconnected()));
  }

  /** マウスボタン押下時のイベントハンドラ. */
  private void onMousePressed(MouseEvent event) {
    try {
      if (!mouseCtrlLock.tryLock(event.getButton())) {
        return;
      }
      Context context = notifService.begin();
      Workspace currentWs = wss.getCurrentWorkspace();
      BhNode newNode = model.findRootNode().copy(context.userOpe());
      factory.setMvc(model, MvcType.DEFAULT);
      ddInfo.currentView = factory.setMvc(newNode, MvcType.DEFAULT);
      if (currentWs == null || ddInfo.currentView == null) {
        terminateDnd();
        return;
      }
  
      ddInfo.isDndFinished = false;
      Vec2D posOnWs = calcClickPosOnWs(event, currentWs);
      BhNodePlacer.moveToWs(currentWs, newNode, posOnWs.x, posOnWs.y, context.userOpe());
      ddInfo.currentView.getEventManager().dispatch(event);
      nodeSelectionViewProxy.hideAll();
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /** マウスドラッグ時のイベントハンドラ. */
  private void onMouseDragged(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      ddInfo.currentView.getEventManager().dispatch(event);
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /** マウスドラッグ検出検出時のイベントハンドラ. */
  private void onDragDetected(MouseEvent event) {
    try {
      if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
        return;
      }
      ddInfo.currentView.getEventManager().dispatch(event);
    } catch (Throwable e) {
      terminateDnd();
      throw e;
    } finally {
      event.consume();
    }
  }

  /** マウスボタンを離したときのイベントハンドラ. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || ddInfo.isDndFinished) {
      event.consume();
      // 余分に D&D の終了処理をしてしまうので terminateDnd を呼ばないこと.
      return;
    }

    try {
      ddInfo.currentView.getEventManager().dispatch(event);
    } finally {
      event.consume();
      terminateDnd();
    }
  }

  /** クリックされたノードからの相対クリック位置をワークスペース上の位置に変換する. */
  private Vec2D calcClickPosOnWs(MouseEvent event, Workspace currentWs) {
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
    Vec2D pos = view.getPositionManager().localToScene(new Vec2D(0, 0));
    return view.getTreeManager().getRootView().getPositionManager().sceneToLocal(pos);
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /** {@link #view} と {@code oldNode} のノードビューを入れ替える. */
  private void replaceView(BhNode oldNode) {
    Optional.ofNullable(oldNode)
        .flatMap(BhNode::getView)
        .ifPresent(oldView -> oldView.getTreeManager().replace(view));
  }

  /** D&D を終えたときの処理. */
  private void terminateDnd() {
    mouseCtrlLock.unlock();
    ddInfo.reset();
    notifService.end();
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
    /** 現在、テンプレートの BhNodeView 上で発生したマウスイベントを送っているワークスペース上の view. */
    private BhNodeView currentView = null;
    /** D&D が終了しているかどうかのフラグ. */
    private boolean isDndFinished = true;

    /** D&Dイベント情報を初期化する. */
    private void reset() {
      currentView = null;
      isDndFinished = true;
    }
  }  
}
