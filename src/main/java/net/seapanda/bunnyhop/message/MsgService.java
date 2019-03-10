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

import net.seapanda.bunnyhop.common.Point2D;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

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
	public Point2D getPosOnWS(BhNode node) {
		MsgData curPos = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node);
		return new Point2D(curPos.doublePair._1, curPos.doublePair._2);
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
	 * undo/redo
	 * @param wss ワークスペースセット
	 * */
	public void deleteUndoRedoCommand(WorkspaceSet wss) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.DELETE_USER_OPE_CMD, wss);
	}
}
