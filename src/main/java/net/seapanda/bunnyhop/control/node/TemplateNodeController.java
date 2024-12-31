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
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.control.MouseCtrlLock;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.model.traverse.TextPrompter;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.TextInputNodeView;
import net.seapanda.bunnyhop.view.proxy.TextNodeViewProxy;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * ノード選択リストにあるノードのコントローラ.
 *
 * @author K.Koike
 */
public class TemplateNodeController {

  private final BhNode model;
  /** テンプレートリストのビュー. */
  private final BhNodeView view;
  /** 上のview ルートとなる view. */
  private final BhNodeView rootView;
  /** 現在、テンプレートのBhNodeView 上で発生したマウスイベントを送っているワークスペース上の view. */
  private final MutableObject<BhNodeView> currentView = new MutableObject<>(null);
  private final MouseCtrlLock mouseCtrlLock = new MouseCtrlLock();
  
  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   */
  public TemplateNodeController(BhNode model, BhNodeView view, BhNodeView rootView) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    Objects.requireNonNull(rootView);
    this.model = model;
    this.view = view;
    this.rootView = rootView;
    setEventHandlers();

    switch (model) {
      case TextNode textNode ->
          textNode.setViewProxy(new TextNodeViewProxyImpl(view));

      case ConnectiveNode connectiveNode ->
          connectiveNode.setViewProxy(new BhNodeViewProxyImpl(view, true));
      
      default -> throw new IllegalArgumentException("Unknown BhNode  (%s)".formatted(model));
    }

    if (model instanceof TextNode textNode) {
      switch (view) {
        case TextInputNodeView textInputView ->
            TextInputNodeController.setEventHandlers(textNode, textInputView);
          
        case ComboBoxNodeView comboBoxView ->
            ComboBoxNodeController.setEventHandlers(textNode, comboBoxView);

        case LabelNodeView labelView ->
            LabelNodeController.setInitStr(textNode, labelView);
        
        default -> { /* do nothing */ }
      }
    }
  }

  /** 各種イベントハンドラをセットする. */
  private void setEventHandlers() {
    view.getEventManager().setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent));
    view.getEventManager().setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent));
    view.getEventManager().setOnDragDetected(mouseEvent -> onDragDetected(mouseEvent));
    view.getEventManager().setOnMouseReleased(mouseEvent -> onMouseReleased(mouseEvent));
    view.getEventManager().addEventFilter(
        MouseEvent.ANY,
        mouseEvent -> consumeIfNotAcceptable(mouseEvent));
  }

  /** マウスボタン押下時のイベントハンドラ. */
  private void onMousePressed(MouseEvent event) {
    if (!mouseCtrlLock.tryLock(event.getButton())) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = BhService.getAppRoot().getWorkspaceSet().getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      BhNode newNode = model.findRootNode().copy(userOpe);
      BhNodeView nodeView = NodeMvcBuilder.build(newNode); //MVC構築
      TextPrompter.prompt(newNode);
      currentView.setValue(nodeView);
      Vec2D posOnWs = calcClickPosOnWs(event, currentWs);
      BhService.bhNodePlacer().moveToWs(currentWs, newNode, posOnWs.x, posOnWs.y, userOpe);
      // undo 用コマンドセット
      nodeView.getController().ifPresent(ctrl -> ctrl.setUserOpeCmdForDnd(userOpe));
      currentView.getValue().getEventManager().propagateEvent(event);
      BhService.getAppRoot().getNodeSelectionViewProxy().hideAll();
      event.consume();
    } catch (Exception e) {
      mouseCtrlLock.unlock();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスドラッグ時のイベントハンドラ. */
  private void onMouseDragged(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || currentView.getValue() == null) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      currentView.getValue().getEventManager().propagateEvent(event);
    } catch (Exception e) {
      mouseCtrlLock.unlock();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスドラッグ検出検出時のイベントハンドラ. */
  private void onDragDetected(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || currentView.getValue() == null) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      currentView.getValue().getEventManager().propagateEvent(event);
    }  catch (Exception e) {
      mouseCtrlLock.unlock();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスボタンを離したときのイベントハンドラ. */
  private void onMouseReleased(MouseEvent event) {
    if (!mouseCtrlLock.isLockedBy(event.getButton()) || currentView.getValue() == null) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      currentView.getValue().getEventManager().propagateEvent(event);
    } finally {
      currentView.setValue(null);
      mouseCtrlLock.unlock();
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** クリックされたノードからの相対クリック位置をワークスペース上の位置に変換する. */
  private Vec2D calcClickPosOnWs(MouseEvent event, Workspace currentWs) {
    // クリックされたテンプレートノードのルートノード上でのクリック位置
    Vec2D posOnRootView = calcRelativePosFromRoot();
    posOnRootView.add(event.getX(), event.getY());
    var posOnScene = new Vec2D(event.getSceneX(), event.getSceneY());
    Vec2D posOnWs = currentWs.getViewProxy().sceneToWorkspace(posOnScene);
    posOnWs.sub(posOnRootView);
    return posOnWs;
  }

  /** view の rootView からの相対位置を求める. */
  private Vec2D calcRelativePosFromRoot() {
    Point2D pos = view.localToScene(0.0, 0.0);
    Point2D posFromRoot = rootView.sceneToLocal(pos);
    return new Vec2D(posFromRoot.getX(), posFromRoot.getY());
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  private static void matchViewToModel(BhNode model, BhNodeView view) {
    if (model instanceof TextNode textNode) {
      if (view instanceof TextInputNodeView textInputView) {
        TextInputNodeController.matchViewToModel(textNode, textInputView);
      } else if (view instanceof LabelNodeView labelView) {
        LabelNodeController.matchViewToModel(textNode, labelView);
      } else if (view instanceof ComboBoxNodeView comboBoxView) {
        ComboBoxNodeController.matchViewToModel(textNode, comboBoxView);
      }
    }
  }

  private class TextNodeViewProxyImpl extends BhNodeViewProxyImpl implements TextNodeViewProxy {
    
    public TextNodeViewProxyImpl(BhNodeView view) {
      super(view, true);
    }

    @Override
    public void matchViewContentToModel() {
      TemplateNodeController.matchViewToModel(model, view);
    }
  }
}
