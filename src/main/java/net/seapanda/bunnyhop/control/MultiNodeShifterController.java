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
package net.seapanda.bunnyhop.control;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.view.MultiNodeShifterView;

/**
 * 複数ノードを同時に移動させるマルチノードシフタのコントローラ
 * @author K.Koike
 * */
public class MultiNodeShifterController {

	private final MultiNodeShifterView view; //!< 管理するビュー
	private final Workspace ws;	//!< view が存在するワークスぺース

	/**
	 * コンストラクタ
	 * @param view 管理するマルチノードシフタのビュー
	 * @param ws view があるワークスペース
	 * */
	public MultiNodeShifterController(MultiNodeShifterView view, Workspace ws) {
		this.view = view;
		this.ws = ws;
	}

	public void init() {

		Vec2D mousePressedPos = new Vec2D(0.0, 0.0);

		// マウスボタン押下
		view.setOnMousePressedHandler(mouseEvent -> {
			view.switchPseudoClassActivation(true, BhParams.CSS.PSEUDO_SELECTED);
			javafx.geometry.Point2D pos = view.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
			mousePressedPos.x = pos.getX();
			mousePressedPos.y = pos.getY();
			view.toFront();
			mouseEvent.consume();
		});

		// ドラッグ中
		view.setOnMouseDraggedHandler(mouseEvent -> {

			double diffX = mouseEvent.getX() - mousePressedPos.x;
			double diffY = mouseEvent.getY() - mousePressedPos.y;
			Vec2D wsSize = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_WORKSPACE_SIZE, ws).vec2d;

			if (mouseEvent.isShiftDown()) {
				view.move(new Vec2D(diffX, diffY), wsSize, true);
			}
			else {
				Vec2D distance = view.move(new Vec2D(diffX, diffY), wsSize, false);
				view.getLinkedNodeList().forEach(node -> MsgService.INSTANCE.setMoveNodeOnWS(node, distance));
			}
			mouseEvent.consume();
		});

		// マウスボタン離し
		view.setOnMouseReleasedHandler(mouseEvent -> {
			view.switchPseudoClassActivation(false, BhParams.CSS.PSEUDO_SELECTED);
			mouseEvent.consume();
		});
	}

	/**
	 * マルチノードシフタを更新する
	 * @param node マルチノードシフタの更新の原因を作ったノード
	 * */
	void updateMultiNodeShifter(BhNode node) {

		if (node.getWorkspace() == ws &&
			node.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS &&
			node.isSelected()) {

			if (view.isLinked(node))
				view.updateLinkPos(node);
			else
				view.createLink(node);

			return;
		}

		view.deleteLink(node);
	}
}
