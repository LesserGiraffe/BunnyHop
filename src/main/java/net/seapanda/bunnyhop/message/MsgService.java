/**
 * Copyright 2018 K.Koike
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
package net.seapanda.bunnyhop.message;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * メッセージ送信を伴う処理のサービスクラス
 * @author K.Koike
 * */
public class MsgService {

	public static final MsgService INSTANCE = new MsgService();	//!< シングルトンインスタンス

	private MsgService() {}


	/**
	 * 引数で指定したノードのワークスペース上での位置を取得する
	 * */
	public Vec2D getPosOnWS(BhNode node) {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node).vec2d;
	}

	/**
	 * 引数で指定したノードのワークスペース上での位置を更新する.<br>
	 * 4分木空間上の位置も更新する
	 * @param node ワークスペース上での位置を更新するノード. (ルートノードを指定すること)
	 * @param x ワークスペース上でのx位置
	 * @param y ワークスペース上でのy位置
	 * */
	public void setPosOnWS(BhNode node, double x, double y) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_POS_ON_WORKSPACE, new MsgData(new Vec2D(x, y)), node);
	}

	/**
	 * ノードの可視性を変更する
	 * @param node このノードの可視性を変更する
	 * @param visible 可視状態にする場合true, 不可視にする場合false
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setNodeVisibility(BhNode node, boolean visible, UserOperationCommand userOpeCmd) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_VISIBLE, new MsgData(visible, userOpeCmd), node);
	}

	/**
	 * undo/redo スタックを解放する
	 * @param wss ワークスペースセット
	 * */
	public void deleteUndoRedoCommand(WorkspaceSet wss) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.DELETE_USER_OPE_CMD, wss);
	}

	/**
	 * 複数ノード移動用マルチノードシフタとリンクを更新する
	 * @param node マルチノードシフタ更新の原因を作ったノード
	 * @param ws 更新するマルチノードシフタを含むワークスペース
	 * */
	public void updateMultiNodeShifter(BhNode node, Workspace ws) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.UPDATE_MULTI_NODE_SHIFTER, new MsgData(node), ws);
	}

	/**
	 * ノードボディのワークスペース上での範囲を取得する
	 * @param node このノードのワークスペース上での範囲を取得する
	 * */
	public Pair<Vec2D, Vec2D> getNodeBodyRange(BhNode node) {

		BhNodeView nodeView = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW, node).nodeView;
		QuadTreeRectangle bodyRange = nodeView.getRegionManager().getRegions()._1;
		return new Pair<Vec2D, Vec2D>(bodyRange.getUpperLeftPos(), bodyRange.getLowerRightPos());
	}

	/**
	 * 外部ノードを含んだノードのサイズを取得する
	 * @param node サイズを取得したいノード
	 * @return 外部ノードを含んだノードのサイズ
	 * */
	public Vec2D getViewSizeIncludingOuter(BhNode node) {
		MsgData msgData = MsgTransporter.INSTANCE.sendMessage(
			BhMsg.GET_VIEW_SIZE_INCLUDING_OUTER,
			new MsgData(true),
			node);
		return msgData.vec2d;
	}

	/**
	 * ワークスペース上のノードを動かす
	 * @param node 動かすノード
	 * @param distance 移動距離
	 * */
	public void setMoveNodeOnWS(BhNode node, Vec2D distance) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.MOVE_NODE_ON_WORKSPACE, new MsgData(distance), node);
	}
}







