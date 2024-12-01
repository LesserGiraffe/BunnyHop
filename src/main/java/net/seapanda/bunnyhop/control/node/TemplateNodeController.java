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

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.command.BhCmd;
import net.seapanda.bunnyhop.command.CmdData;
import net.seapanda.bunnyhop.command.CmdProcessor;
import net.seapanda.bunnyhop.model.node.BhNode;
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
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * ノード選択リストにあるノードのコントローラ.
 *
 * @author K.Koike
 */
public class TemplateNodeController implements CmdProcessor {

  private final BhNode model;
  /** テンプレートリストのビュー. */
  private final BhNodeView view;
  /** 上のview ルートとなる view. */
  private final BhNodeView rootView;
  /** 現在、テンプレートのBhNodeView 上で発生したマウスイベントを送っているワークスペース上の view. */
  private final MutableObject<BhNodeView> currentView = new MutableObject<>(null);
  
  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   */
  public TemplateNodeController(BhNode model, BhNodeView view, BhNodeView rootView) {
    this.model = model;
    this.view = view;
    this.rootView = rootView;
    setEventHandlers();

    if (model instanceof TextNode textNode) {
      if (view instanceof TextInputNodeView textInputView) {
        TextInputNodeController.setEventHandlers(textNode, textInputView);
      } else if (view instanceof ComboBoxNodeView comboBoxView) {
        ComboBoxNodeController.setEventHandlers(textNode, comboBoxView);
      } else if (view instanceof LabelNodeView labelView) {
        LabelNodeController.setInitStr(textNode, labelView);
      }
    }
  }

  /** 各種イベントハンドラをセットする. */
  private void setEventHandlers() {
    view.getEventManager().setOnMousePressed(mouseEvent -> onMousePressed(mouseEvent));
    view.getEventManager().setOnMouseDragged(mouseEvent -> onMouseDragged(mouseEvent));
    view.getEventManager().setOnDragDetected(mouseEvent -> onDragDetected(mouseEvent));
    view.getEventManager().setOnMouseReleased(mouseEvent -> onMouseReleased(mouseEvent));
  }

  /** マウスボタン押下時のイベントハンドラ. */
  private void onMousePressed(MouseEvent mouseEvent) {
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = BhService.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      BhNode newNode = model.findRootNode().copy(userOpe);
      BhNodeView nodeView = NodeMvcBuilder.build(newNode); //MVC構築
      TextPrompter.prompt(newNode);
      currentView.setValue(nodeView);
      Vec2D posOnRootView = calcRelativePosFromRoot();  //クリックされたテンプレートノードのルートノード上でのクリック位置
      posOnRootView.add(mouseEvent.getX(), mouseEvent.getY());
      Vec2D posOnWs = BhService.cmdProxy().sceneToWorkspace(
          mouseEvent.getSceneX(), mouseEvent.getSceneY(), currentWs);
      posOnWs.sub(posOnRootView);
      BhService.bhNodePlacer().moveToWs(currentWs, newNode, posOnWs.x, posOnWs.y, userOpe);
      BhService.cmdProxy().setUserOpeCmd(newNode, userOpe);  // undo 用コマンドセット
      currentView.getValue().getEventManager().propagateEvent(mouseEvent);
      BhService.bhNodeSelectionService().hideAll();
      mouseEvent.consume();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスドラッグ時のイベントハンドラ. */
  private void onMouseDragged(MouseEvent mouseEvent) {
    if (currentView.getValue() == null) {
      return;
    }
    currentView.getValue().getEventManager().propagateEvent(mouseEvent);
  }

  /** マウスドラッグ検出検出時のイベントハンドラ. */
  private void onDragDetected(MouseEvent mouseEvent) {
    ModelExclusiveControl.lockForModification();
    try {
      onMouseDragged(mouseEvent);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** マウスボタンを離したときのイベントハンドラ. */
  private void onMouseReleased(MouseEvent mouseEvent) {
    ModelExclusiveControl.lockForModification();
    try {
      onMouseDragged(mouseEvent);
      currentView.setValue(null);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** view の rootView からの相対位置を求める. */
  private Vec2D calcRelativePosFromRoot() {
    Point2D pos = view.localToScene(0.0, 0.0);
    Point2D posFromRoot = rootView.sceneToLocal(pos);
    return new Vec2D(posFromRoot.getX(), posFromRoot.getY());
  }

  /**
   * 受信したメッセージを処理する.
   *
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するデータ
   * @return メッセージを処理した結果返すデータ
   */
  @Override
  public CmdData process(BhCmd msg, CmdData data) {
    switch (msg) {
      case ADD_ROOT_NODE: // model がWorkSpace のルートノードとして登録された
        return new CmdData(model, view);

      case REMOVE_ROOT_NODE:
        return new CmdData(model, view);

      case GET_POS_ON_WORKSPACE:
        var pos = view.getPositionManager().getPosOnWorkspace();
        return new CmdData(pos);

      case REPLACE_NODE_VIEW:
        view.getTreeManager().replace(data.nodeView);
        break;

      case SWITCH_PSEUDO_CLASS_ACTIVATION:
        view.getLookManager().switchPseudoClassActivation(data.bool, data.text);
        break;

      case GET_VIEW:
        return new CmdData(view);

      case REMOVE_FROM_GUI_TREE:
        view.getTreeManager().removeFromGuiTree();
        break;

      case SET_VISIBLE:
        view.getLookManager().setVisible(data.bool);
        data.userOpe.pushCmdOfSetVisible(view, data.bool);
        break;

      case MATCH_VIEW_CONTENT_TO_MODEL:
        matchViewToModel(model, view);
        break;

      case IS_TEMPLATE_NODE:
        return new CmdData(true);

      default:
        // do nothing
    }
    return null;
  }

  private void matchViewToModel(BhNode model, BhNodeView view) {
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
}
