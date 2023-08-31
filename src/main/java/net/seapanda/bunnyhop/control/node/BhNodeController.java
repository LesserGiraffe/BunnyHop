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

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.DelayedDeleter;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.modelservice.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.TrashboxService;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * BhNode のコントローラクラスに共通の処理をまとめたクラス
 * @author K.Koike
 * */
public class BhNodeController implements MsgProcessor {

	private final BhNode model;
	private final BhNodeView view;
	private final DragAndDropEventInfo ddInfo = this.new DragAndDropEventInfo();
	private final MsgProcessor msgProcessor = this.new MsgProcessor();

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	protected BhNodeController(BhNode model, BhNodeView view) {
		this.model = model;
		this.view = view;
		setEventHandlers();
	}

	/**
	 * 引数で指定したノードに対応するビューにGUIイベントを伝播する
	 * @param node イベントを伝播したいBhNodeView に対応するBhNode
	 * @param event 伝播したいイベント
	 */
	private void propagateGUIEvent(BhNode node, Event event) {

		if (node == null)
			return;
		BhNodeView nodeView = MsgService.INSTANCE.getBhNodeView(node);
		nodeView.getEventManager().propagateEvent(event);
		event.consume();
	}

	/**
	 * 各種イベントハンドラをセットする
	 */
	private void setEventHandlers() {

		view.getEventManager().setOnMousePressed(this::onMousePressed);
		view.getEventManager().setOnMouseDragged(this::onMouseDragged);
		view.getEventManager().setOnDragDetected(this::onMouseDragDetected);
		view.getEventManager().setOnMouseReleased(this::onMouseReleased);
	}

	/**
	 * マウスボタン押下時の処理
	 */
	private void  onMousePressed(MouseEvent event) {

		ModelExclusiveControl.INSTANCE.lockForModification();
		try {
			//model.show(0);	//for debug
			if (!model.isMovable()) {
				ddInfo.propagateEvent = true;
				propagateGUIEvent(model.findParentNode(), event);
				return;
			}

			//BhNode の新規追加の場合, すでにundo用コマンドオブジェクトがセットされている
			if (ddInfo.userOpeCmd == null)
				ddInfo.userOpeCmd = new UserOperationCommand();

			ViewHelper.INSTANCE.drawShadow(view);
			view.getPositionManager().toFront(true);
			selectNode(event.isShiftDown());	//選択処理
			javafx.geometry.Point2D mousePressedPos = view.sceneToLocal(event.getSceneX(), event.getSceneY());
			ddInfo.mousePressedPos = new Vec2D(mousePressedPos.getX(), mousePressedPos.getY());
			ddInfo.posOnWorkspace = view.getPositionManager().getPosOnWorkspace();
			view.setMouseTransparent(true);
			event.consume();
		}
		finally {
			ModelExclusiveControl.INSTANCE.unlockForModification();
		}
	}

	/**
	 * マウスドラッグ時の処理
	 */
	private void onMouseDragged(MouseEvent event) {

		if (ddInfo.propagateEvent) {
			propagateGUIEvent(model.findParentNode(), event);
			return;
		}

		if (event.isShiftDown()) {
			event.setDragDetect(false);
			event.consume();
			return;
		}

		if (ddInfo.dragging) {
			double diffX = event.getX() - ddInfo.mousePressedPos.x;
			double diffY = event.getY() - ddInfo.mousePressedPos.y;
			moveNodeOnWorkspace(diffX, diffY);
			// ドラッグ検出されていない場合、強調は行わない. 子ノードがダングリングになっていないのに、重なったノード (入れ替え対象) だけが検出されるのを防ぐ
			highlightOverlappedNode();
			TrashboxService.INSTANCE.openCloseTrashbox(event.getSceneX(), event.getSceneY());
		}
		event.consume();
	}

	/**
	 * マウスドラッグを検出した時の処理.
	 * 先に {@code onMouseDragged} が呼ばれ, ある程度ドラッグしたときにこれが呼ばれる.
	 */
	private void onMouseDragDetected(MouseEvent mouseEvent) {

		ModelExclusiveControl.INSTANCE.lockForModification();
		try {
			if (ddInfo.propagateEvent) {
				propagateGUIEvent(model.findParentNode(), mouseEvent);
				return;
			}

			if (mouseEvent.isShiftDown()) {
				mouseEvent.consume();
				return;
			}

			ddInfo.dragging = true;
			//子ノードでかつ取り外し可能 -> 親ノードから切り離し, ダングリング状態へ
			if(model.isRemovable()) {
				ddInfo.latestParent = model.findParentNode();
				ddInfo.latestRoot = model.findRootNode();
				BhNode newNode = BhNodeHandler.INSTANCE.removeChild(model, ddInfo.userOpeCmd);
				ddInfo.latestParent.execScriptOnChildReplaced(
					model, newNode, newNode.getParentConnector(), ddInfo.userOpeCmd);
			}
			mouseEvent.consume();
		}
		finally {
			ModelExclusiveControl.INSTANCE.unlockForModification();
		}
	}

	/**
	 * マウスボタンを離したときの処理
	 */
	private void onMouseReleased(MouseEvent mouseEvent) {

		ModelExclusiveControl.INSTANCE.lockForModification();
		try {
			if (ddInfo.propagateEvent) {
				propagateGUIEvent(model.findParentNode(), mouseEvent);
				ddInfo.propagateEvent = false;
				return;
			}

			if (ddInfo.currentOverlapped != null)
				MsgService.INSTANCE.switchPseudoClassActivation(
					ddInfo.currentOverlapped, BhParams.CSS.PSEUDO_OVERLAPPED, false);

			//子ノード -> ワークスペース
			if ((model.getState() == BhNode.State.ROOT_DANGLING) && ddInfo.currentOverlapped == null) {
				toWorkspace(model.getWorkspace());
			}
			// (ワークスペース or 子ノード) -> 子ノード
			else if (ddInfo.currentOverlapped != null) {
				toChildNode(ddInfo.currentOverlapped);
			}
			//同一ワークスペース上で移動
			else {
				toSameWorkspace();
			}

			view.getPositionManager().toFront(false);
			deleteUnnecessaryNodes(mouseEvent);
			SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(ddInfo.userOpeCmd);
			SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(ddInfo.userOpeCmd);
			BunnyHop.INSTANCE.pushUserOpeCmd(ddInfo.userOpeCmd);
			ddInfo.reset();
			view.setMouseTransparent(false);	// 処理が終わったので、元に戻しておく。
			TrashboxService.INSTANCE.openCloseTrashbox(false);
			mouseEvent.consume();
		}
		finally {
			ModelExclusiveControl.INSTANCE.unlockForModification();
		}
	}

	/**
	 * (ワークスペース or 子ノード) から 子ノード に移動する
	 * @param oldChildNode 入れ替え対象の古い子ノード
     **/
	private void toChildNode(BhNode oldChildNode) {

		boolean fromWS = model.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS;
		ConnectiveNode parentNode = oldChildNode.findParentNode();
		Connector parentCnctr = oldChildNode.getParentConnector();

		//ワークスペースから移動する場合
		if (fromWS)
			ddInfo.userOpeCmd.pushCmdOfSetPosOnWorkspace(ddInfo.posOnWorkspace.x, ddInfo.posOnWorkspace.y, model);

		ConnectiveNode oldParentOfReplaced = oldChildNode.findParentNode();	//入れ替えられるノードの親ノード
		BhNode oldRootOfReplaced = oldChildNode.findRootNode();	//入れ替えられるノードのルートノード
		BhNodeHandler.INSTANCE.replaceChild(oldChildNode, model, ddInfo.userOpeCmd);	//重なっているノードをこのノードと入れ替え
		//接続変更時のスクリプト実行
		model.getEventDispatcher().dispatchOnMovedToChild(
			ddInfo.latestParent, ddInfo.latestRoot, oldChildNode, ddInfo.userOpeCmd);

		Vec2D posOnWS = MsgService.INSTANCE.getPosOnWS(oldChildNode);
		double newXPosInWs = posOnWS.x + BhParams.LnF.REPLACED_NODE_SHIFT;
		double newYPosInWs = posOnWS.y + BhParams.LnF.REPLACED_NODE_SHIFT;
		//重なっているノードをWSに移動
		BhNodeHandler.INSTANCE.moveToWS(
			oldChildNode.getWorkspace(), oldChildNode, newXPosInWs, newYPosInWs, ddInfo.userOpeCmd);
		//接続変更時のスクリプト実行
		oldChildNode.getEventDispatcher().dispatchOnMovedFromChildToWS(
			oldParentOfReplaced, oldRootOfReplaced, model, false, ddInfo.userOpeCmd);

		// 子ノード入れ替え時のスクリプト実行
		parentNode.execScriptOnChildReplaced(oldChildNode, model, parentCnctr, ddInfo.userOpeCmd);
	}

	/**
	 * 子ノードからワークスペースに移動する
	 * @param ws 移動先のワークスペース
	 **/
	private void toWorkspace(Workspace ws) {

		Vec2D absPosInWS = view.getPositionManager().getPosOnWorkspace();
		BhNodeHandler.INSTANCE.moveToWS(ws, model, absPosInWS.x, absPosInWS.y, ddInfo.userOpeCmd);
		model.getEventDispatcher().dispatchOnMovedFromChildToWS(
			ddInfo.latestParent,
			ddInfo.latestRoot,
			model.getLastReplaced(),
			true,
			ddInfo.userOpeCmd);	//接続変更時のスクリプト実行
		view.getAppearanceManager().arrangeAndResize();
	}

	/**
	 * 同一ワークスペースへの移動処理
	 */
	private void toSameWorkspace() {

		if (ddInfo.dragging && (model.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS))
			ddInfo.userOpeCmd.pushCmdOfSetPosOnWorkspace(ddInfo.posOnWorkspace.x, ddInfo.posOnWorkspace.y, model);
		view.getAppearanceManager().arrangeAndResize();
	}

	/**
	 * 不要になったノードを削除する
	 */
	private void deleteUnnecessaryNodes(MouseEvent mouseEvent) {

		DelayedDeleter.INSTANCE.deleteAll(ddInfo.userOpeCmd);

		//ゴミ箱に重なっていた場合, 削除
		if (model.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS &&
			TrashboxService.INSTANCE.isInTrashboxArea(mouseEvent.getSceneX(), mouseEvent.getSceneY())) {
			model.getEventDispatcher().dispatchOnDeletionRequested(
				new ArrayList<BhNode>() {{add(model);}}, CauseOfDeletion.TRASH_BOX, ddInfo.userOpeCmd);
			BhNodeHandler.INSTANCE.deleteNode(model, ddInfo.userOpeCmd);
		}
	}

	/**
	 * viewと重なっているBhNodeViewを強調する
	 */
	private void highlightOverlappedNode() {

		if (ddInfo.currentOverlapped != null) {
			//前回重なっていたものをライトオフ
			MsgService.INSTANCE.switchPseudoClassActivation(
				ddInfo.currentOverlapped, BhParams.CSS.PSEUDO_OVERLAPPED, false);
		}
		ddInfo.currentOverlapped = null;

		List<BhNode> overlappedList = view.getRegionManager().searchForOverlappedModels();
		for (BhNode overlapped : overlappedList) {
			if (overlapped.canBeReplacedWith(model)) {	//このノードと入れ替え可能
				//今回重なっているものをライトオン
				MsgService.INSTANCE.switchPseudoClassActivation(
					overlapped, BhParams.CSS.PSEUDO_OVERLAPPED, true);
				ddInfo.currentOverlapped = overlapped;
				break;
			}
		}
	}

	/**
	 * ノードの選択処理を行う.
	 * @param isShiftDown シフトボタンが押されている場合 true
	 */
	private void selectNode(boolean isShiftDown) {

		if (isShiftDown) {
			if (model.isSelected()) {
				model.getWorkspace().removeSelectedNode(model, ddInfo.userOpeCmd);
			}
			else {
				model.getWorkspace().addSelectedNode(model, ddInfo.userOpeCmd);
			}
		}
		else {
			if (model.isSelected()) {
				// 末尾ノードまで一気に選択
				BhNode outerNode = model.findOuterNode(-1);
				while(true) {
					if (outerNode == model)
						break;
					if (!outerNode.isSelected() && outerNode.isMovable())
						model.getWorkspace().addSelectedNode(outerNode, ddInfo.userOpeCmd);
					outerNode = outerNode.findParentNode();
				}
			}
			else {
				model.getWorkspace().setSelectedNode(model, ddInfo.userOpeCmd);
			}
		}
	}

	/**
	 * ワークスペース上でノードを動かす.
	 * @param distanceX x移動量
	 * @param distanceY y移動量
	 * */
	private void moveNodeOnWorkspace(double distanceX, double distanceY) {

		view.getPositionManager().move(distanceX, distanceY);
		if (model.getWorkspace() != null)
			MsgService.INSTANCE.updateMultiNodeShifter(model, model.getWorkspace());
	}

	@Override
	public MsgData processMsg(BhMsg msg, MsgData data) {
		return msgProcessor.processMsg(msg, data);
	}

	/**
	 * D&D 操作で使用する一連のイベントハンドラがアクセスするデータをまとめたクラス.
	 */
	private class DragAndDropEventInfo {
		Vec2D mousePressedPos = null;
		Vec2D posOnWorkspace = null;
		BhNode currentOverlapped = null;	//現在重なっているView
		boolean propagateEvent = false;	//!< イベントを親ノードに伝播する場合true
		boolean dragging = false;	//!< ドラッグ中ならtrue
		ConnectiveNode latestParent = null;	//!< 最後につながっていた親ノード
		BhNode latestRoot = null;	//!< 最後に子孫であったルートノード
		UserOperationCommand userOpeCmd;	//!< D&D操作のundo用コマンド

		/**
		 * D&Dイベント情報を初期化する
		 */
		public void reset() {
			mousePressedPos = null;
			posOnWorkspace = null;
			currentOverlapped = null;
			propagateEvent = false;
			dragging = false;
			latestParent = null;
			latestRoot = null;
			userOpeCmd = null;
		}
	}

	/**
	 * BhNode 宛てに送られたメッセージを処理するクラス.
	 */
	private class MsgProcessor {

		/**
		 * 受信したメッセージを処理する
		 * @param msg メッセージの種類
		 * @param data メッセージの種類に応じて処理するデータ
		 * @return メッセージを処理した結果返すデータ
		 * */
		public MsgData processMsg(BhMsg msg, MsgData data) {

			switch (msg) {

				case ADD_ROOT_NODE: // model がWorkSpace のルートノードとして登録された
					return  new MsgData(model, view);

				case REMOVE_ROOT_NODE:
					return  new MsgData(model, view);

				case ADD_QT_RECTANGLE:
					return new MsgData(view);

				case REMOVE_QT_RECTANGLE:
					view.getRegionManager().removeQtRectable();
					break;

				case GET_POS_ON_WORKSPACE:
					var pos = view.getPositionManager().getPosOnWorkspace();
					return new MsgData(pos);

				case SET_POS_ON_WORKSPACE:
					setPosOnWorkspace(data.vec2d);
					break;

				case MOVE_NODE_ON_WORKSPACE:
					moveNodeOnWorkspace(data.vec2d.x, data.vec2d.y);
					break;

				case GET_VIEW_SIZE_INCLUDING_OUTER:
					Vec2D size = view.getRegionManager().getNodeSizeIncludingOuter(data.bool);
					return new MsgData(size);

				case UPDATE_ABS_POS:
					Vec2D posOnWs = view.getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
					view.getPositionManager().setPosOnWorkspace(posOnWs.x, posOnWs.y);
					break;

				case REPLACE_NODE_VIEW:
					view.getTreeManager().replace(data.nodeView);	//新しいノードビューに入れ替え
					break;

				case SWITCH_PSEUDO_CLASS_ACTIVATION:
					view.getAppearanceManager().switchPseudoClassActivation(data.bool, data.text);
					break;

				case GET_VIEW:
					return new MsgData(view);

				case SET_USER_OPE_CMD:
					ddInfo.userOpeCmd = data.userOpeCmd;
					break;

				case REMOVE_FROM_GUI_TREE:
					view.getTreeManager().removeFromGUITree();
					break;

				case SET_VISIBLE:
					view.getAppearanceManager().setVisible(data.bool);
					data.userOpeCmd.pushCmdOfSetVisible(view, data.bool);
					break;

				case SET_SYNTAX_ERRPR_INDICATOR:
					data.userOpeCmd.pushCmdOfSetSyntaxError(view, data.bool, view.getAppearanceManager().isSyntaxErrorVisible());
					view.getAppearanceManager().setSytaxErrorVisibility(data.bool);
					break;

				case SELECT_NODE_VIEW:
					view.getAppearanceManager().select(data.bool);
					break;

				case IS_TEMPLATE_NODE:
					return new MsgData(false);

				default:
					throw new IllegalStateException("receive an unknown msg " + msg);
			}

			return null;
		}

		/**
		 * ワークスペース上での位置を設定する.
		 * @param posOnWs 設定するワークスペース上での位置
		 */
		private void setPosOnWorkspace(Vec2D posOnWs) {

			view.getPositionManager().setPosOnWorkspace(posOnWs.x, posOnWs.y);
			if (model.getWorkspace() != null)
				MsgService.INSTANCE.updateMultiNodeShifter(model, model.getWorkspace());
		}
	}
}










