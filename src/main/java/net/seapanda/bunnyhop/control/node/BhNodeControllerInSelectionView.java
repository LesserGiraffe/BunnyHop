/**
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
import net.seapanda.bunnyhop.common.Single;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.modelprocessor.NodeMVCBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.TextInputNodeView;

/**
 * ノード選択リストにあるBhNodeのコントローラ
 * @author K.Koike
 * */
public class BhNodeControllerInSelectionView {

	private final BhNode model;
	private final BhNodeView view;	//!< テンプレートリストのビュー
	private final BhNodeView rootView;	//!< 上のview ルートとなるview
	private final Single<BhNodeView> currentView = new Single<>();	//!< 現在、テンプレートのBhNodeView 上で発生したマウスイベントを送っているワークスペース上の view
	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public BhNodeControllerInSelectionView(BhNode model, BhNodeView view, BhNodeView rootView) {

		this.model = model;
		this.view = view;
		this.rootView = rootView;
		setEventHandlers();

		if (view instanceof TextInputNodeView) {
			TextInputNodeController.setTextChangeHandlers((TextNode)model, (TextInputNodeView)view);
		}
		else if (view instanceof ComboBoxNodeView) {
			ComboBoxNodeController.setItemChangeHandler((TextNode)model, (ComboBoxNodeView)view);
		}
		else if (view instanceof LabelNodeView) {
			LabelNodeController.setInitStr((TextNode)model, (LabelNodeView)view);
		}
	}

	/**
	 * 各種イベントハンドラをセットする
	 */
	private void setEventHandlers() {

		setOnMousePressed();
		setOnMouseDragged();
		setOnDeagDetected();
		setOnMouseReleased();
	}

	/**
	 * マウスボタン押下時のイベントハンドラを登録する
	 */
	private void setOnMousePressed() {

		view.getEventManager().setOnMousePressed(
			mouseEvent -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					Workspace currentWS = BunnyHop.INSTANCE.getCurrentWorkspace();
					if (currentWS == null)
						return;

					UserOperationCommand userOpeCmd = new UserOperationCommand();
					BhNode newNode = model.findRootNode().copy(userOpeCmd, (bhNode) -> true);
					BhNodeView nodeView = NodeMVCBuilder.build(newNode); //MVC構築
					TextImitationPrompter.prompt(newNode);
					currentView.content = nodeView;
					Vec2D posOnRootView = calcRelativePosFromRoot();	//クリックされたテンプレートノードのルートノード上でのクリック位置
					posOnRootView.x += mouseEvent.getX();
					posOnRootView.y += mouseEvent.getY();
					Vec2D posOnWS = MsgService.INSTANCE.sceneToWorkspace(
						mouseEvent.getSceneX(), mouseEvent.getSceneY(), currentWS);
					BhNodeHandler.INSTANCE.addRootNode(
						currentWS,
						newNode,
						posOnWS.x - posOnRootView.x ,
						posOnWS.y - posOnRootView.y,
						userOpeCmd);
					MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_USER_OPE_CMD, new MsgData(userOpeCmd), newNode);	//undo用コマンドセット
					currentView.content.getEventManager().propagateEvent(mouseEvent);
					BunnyHop.INSTANCE.hideTemplatePanel();
					mouseEvent.consume();
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * マウスドラッグ時のイベントハンドラを登録する
	 */
	private void setOnMouseDragged() {

		view.getEventManager().setOnMouseDragged(
			mouseEvent -> {
				if (currentView.content == null)
					return;
				currentView.content.getEventManager().propagateEvent(mouseEvent);
			});
	}

	/**
	 * マウスドラッグ検出検出時のイベントハンドラを登録する
	 */
	private void setOnDeagDetected() {

		view.getEventManager().setOnDragDetected(
			mouseEvent -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					if (currentView.content == null)
						return;
					currentView.content.getEventManager().propagateEvent(mouseEvent);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * マウスボタンを離したときのイベントハンドラを登録する
	 */
	private void setOnMouseReleased() {

		view.getEventManager().setOnMouseReleased(
			mouseEvent -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					if (currentView.content == null)
						return;
					currentView.content.getEventManager().propagateEvent(mouseEvent);
					currentView.content = null;
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * view の rootView からの相対位置を求める
	 * */
	private Vec2D calcRelativePosFromRoot() {

		Point2D pos = view.localToScene(0.0, 0.0);
		Point2D posFromRoot = rootView.sceneToLocal(pos);
		return new Vec2D(posFromRoot.getX(), posFromRoot.getY());
	}
}










