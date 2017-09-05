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
package pflab.bunnyhop.control;

import pflab.bunnyhop.modelprocessor.NodeMVCBuilder;
import pflab.bunnyhop.common.Single;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.root.BunnyHop;
import pflab.bunnyhop.view.BhNodeView;
import pflab.bunnyhop.view.TextFieldNodeView;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.common.Point2D;
import pflab.bunnyhop.modelhandler.BhNodeHandler;
import pflab.bunnyhop.view.ComboBoxNodeView;
import pflab.bunnyhop.view.LabelNodeView;

/**
 * ノード選択リストにあるBhNodeのコントローラ
 * @author K.Koike
 * */
public class BhNodeControllerInSelectionView {

	private final BhNode model;
	private final BhNodeView view;	//!< テンプレートリストのビュー
	private final BhNodeView rootView;	//!< 上のview ルートとなるview

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public BhNodeControllerInSelectionView(BhNode model, BhNodeView view, BhNodeView rootView) {

		this.model = model;
		this.view = view;
		this.rootView = rootView;

		setMouseEventHandler();

		if (view instanceof TextFieldNodeView) {
			TextFieldNodeController.setTextChangeHandler((TextNode)model, (TextFieldNodeView)view);
		}
		else if (view instanceof ComboBoxNodeView) {
			ComboBoxNodeController.setItemChangeHandler((TextNode)model, (ComboBoxNodeView)view);
		}
		else if (view instanceof LabelNodeView) {
			LabelNodeController.setInitStr((TextNode)model, (LabelNodeView)view);
		}
	}

	/**
	 * View が走査されたときのイベントハンドラをセットする
	 * */
	private void setMouseEventHandler() {

		Single<BhNodeView> currentView = new Single<>();	//現在、テンプレートのBhNodeView 上に発生したてマウスイベントを送っているワークスペース上の view

		//マウスボタンを押したとき
		view.getEventManager().setOnMousePressedHandler(mouseEvent -> {
			
			Workspace currentWS = BunnyHop.instance.getCurrentWorkspace();
			if (currentWS == null)
				return;
			
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			NodeMVCBuilder builder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default);
			BhNode newNode = model.findRootNode().copy(userOpeCmd);
			newNode.accept(builder);	//MVC構築
			currentView.content = builder.getTopNodeView();
			Point2D posOnRootView = BhNodeView.getRelativePos(rootView, view);	//クリックされたテンプレートノードのルートノード上でのクリック位置
			posOnRootView.x += mouseEvent.getX();
			posOnRootView.y += mouseEvent.getY();
			MsgData posOnWS = MsgTransporter.instance.sendMessage(BhMsg.SCENE_TO_WORKSPACE, new MsgData(mouseEvent.getSceneX(), mouseEvent.getSceneY()) ,currentWS);
			BhNodeHandler.instance.addRootNode(
				currentWS,
				newNode,
				posOnWS.doublePair._1 - posOnRootView.x ,
				posOnWS.doublePair._2- posOnRootView.y,
				userOpeCmd);
			MsgTransporter.instance.sendMessage(BhMsg.SET_USER_OPE_CMD, new MsgData(userOpeCmd), newNode);	//undo用コマンドセット
			currentView.content.getEventManager().propagateEvent(mouseEvent);
			BunnyHop.instance.hideTemplatePanel();
			mouseEvent.consume();
		});

		//ドラッグ中
		view.getEventManager().setOnMouseDraggedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
		});

		//ドラッグを検出(先にsetOnMouseDraggedが呼ばれ、ある程度ドラッグしたときにこれが呼ばれる)
		view.getEventManager().setOnDragDetectedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
		});

		//マウスボタンを離したとき
		view.getEventManager().setOnMouseReleasedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
			currentView.content = null;
		});
	}
}










