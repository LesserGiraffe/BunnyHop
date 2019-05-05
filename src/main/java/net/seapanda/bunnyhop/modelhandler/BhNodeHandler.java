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
package net.seapanda.bunnyhop.modelhandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.State;
import net.seapanda.bunnyhop.modelprocessor.NodeDeselector;
import net.seapanda.bunnyhop.modelprocessor.WorkspaceRegisterer;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * BhNodeの追加, 移動, 入れ替え, 削除用関数を提供するクラス
 * @author K.Koike
 */
public class BhNodeHandler {

	public static final BhNodeHandler INSTANCE = new BhNodeHandler();	//!< シングルトンインスタンス

	private BhNodeHandler(){}

	/**
	 * Workspace へのBhNodeの新規追加と4分木空間への登録を行う
	 * @param ws BhNodeを追加したいワークスペース
	 * @param node WS直下に追加したいノード.
	 * @param x ワークスペース上での位置
	 * @param y ワークスペース上での位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void addRootNode(Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {

		Vec2D curPos = MsgService.INSTANCE.getPosOnWS(node);
		WorkspaceRegisterer.register(node, ws, userOpeCmd);	//ツリーの各ノードへのWSの登録
		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);		//ワークスペース直下に追加
		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_QT_RECTANGLE, node, ws);	//4分木ノード登録(重複登録はされない)
		MsgService.INSTANCE.setPosOnWS(node, x, y);	//ワークスペース内での位置登録

		userOpeCmd.pushCmdOfAddRootNode(node, ws);
		userOpeCmd.pushCmdOfAddQtRectangle(node, ws);
		userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
		UnscopedNodeManager.INSTANCE.collect(node, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
	}

	/**
	 * 引数で指定したBhNodeを削除する
	 * @param node WSから取り除きたいノード.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 削除したノードと入れ替わる子ノードが作成された場合, そのノードを返す
	 * */
	public Optional<BhNode> deleteNode(BhNode node, UserOperationCommand userOpeCmd) {

		Optional<BhNode> newNode = Optional.empty();
		if (DelayedDeleter.INSTANCE.containsInCandidateList(node)) {
			DelayedDeleter.INSTANCE.deleteCandidate(node, userOpeCmd);
			return newNode;
		}

		//undo時に削除前の状態のBhNodeを選択ノードとして MultiNodeShifterController に通知するためここで非選択にする
		NodeDeselector.deselect(node, userOpeCmd);

		Workspace ws = node.getWorkspace();
		BhNode.State nodeState = node.getState();
		switch(nodeState) {
			case CHILD:
				newNode = Optional.of(removeChild(node, userOpeCmd));
				MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除
				break;

			case ROOT_DANGLING:
				MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除
				break;

			case ROOT_DIRECTLY_UNDER_WS:
				MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_ROOT_NODE, new MsgData(false), node, ws);	 //WS直下から削除
				userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
				break;

			case DELETED:
				return newNode;

			default:
				throw new AssertionError("invalid node state " + nodeState);
		}

		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);		 //4分木空間からの削除
		userOpeCmd.pushCmdOfRemoveQtRectangle(node, ws);
		WorkspaceRegisterer.deregister(node, userOpeCmd);
		node.delete(userOpeCmd);
		return newNode;
	}

	/**
	 * 引数で指定したBhNodeを削除する <br>
	 * BhNodeの親子関係, ワークスペースへの登録情報, 4分木空間への登録情報は消える.<br>
	 * 引数で指定したノードは遅延削除リストに入る.
	 * @param node 仮削除するノード
	 * @param saveModelRels 親子関係を除くモデル間の関係(イミテーション -オリジナルの関係等) を保存する場合true
	 * @param saveGuiTreeRels GUIツリーの親子関係を保存する場合true
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 削除したノードと入れ替わる子ノードが作成された場合, そのノードを返す
	 */
	public Optional<BhNode> deleteNodeIncompletely(
		BhNode node,
		boolean saveModelRels,
		boolean saveGuiTreeRels,
		UserOperationCommand userOpeCmd) {

		Optional<BhNode> newNode = Optional.empty();
		if (DelayedDeleter.INSTANCE.containsInCandidateList(node.findRootNode())) {
			return newNode;
		}

		//undo時に削除前の状態のBhNodeを選択ノードとして MultiNodeShifterController に通知するためここで非選択にする
		NodeDeselector.deselect(node, userOpeCmd);

		Workspace ws = node.getWorkspace();
		BhNode.State nodeState = node.getState();
		switch(nodeState) {
			case CHILD:
				newNode = Optional.of(removeChild(node, userOpeCmd));
				if (!saveGuiTreeRels)
					MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除
				break;

			case ROOT_DANGLING:
				if (!saveGuiTreeRels)
					MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除
				break;

			case ROOT_DIRECTLY_UNDER_WS:
				MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_ROOT_NODE, new MsgData(saveGuiTreeRels), node, ws);	 //WS直下から削除
				userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
				break;

			case DELETED:
				return newNode;

			default:
				throw new AssertionError("invalid node state " + nodeState);
		}

		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);		 //4分木空間からの削除
		userOpeCmd.pushCmdOfRemoveQtRectangle(node, ws);
		WorkspaceRegisterer.deregister(node, userOpeCmd); //ノードに対して登録されたWSを削除

		if (!saveModelRels)
			node.delete(userOpeCmd);
		DelayedDeleter.INSTANCE.addDeletionCandidate(node, saveModelRels, saveGuiTreeRels);
		return newNode;
	}

	/**
	 * 引数で指定したノードを全て削除する
	 * @param nodeListToDelete 削除するノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 削除したノードと入れ替わる子ノードが作成された場合, 削除された古いノードと新しく作成されたノードのペアのリストを返す
	 */
	public List<Pair<BhNode, BhNode>> deleteNodes(Collection<? extends BhNode> nodeListToDelete, UserOperationCommand userOpeCmd) {

		List<Pair<BhNode, BhNode>> oldAndNewNodeList = new ArrayList<>();
		List<BhNode> deleteList = new ArrayList<>();
		for (BhNode candidateForDeletion : nodeListToDelete) {
			boolean canDelete = true;
			for (BhNode compared : nodeListToDelete) {
				//自分自身とは比較しない
				if (candidateForDeletion == compared)
					continue;

				//削除候補が別の削除候補の子孫ノードである -> 削除候補の先祖だけ削除する.
				if (candidateForDeletion.isDescendantOf(compared)) {
					canDelete = false;
					break;
				}

				//削除候補のオリジナルノードが別の削除候補子孫ノードである -> イミテーションは直接削除せず, オリジナルの先祖だけ削除する.
				BhNode orgNode = candidateForDeletion.getOriginalNode();
				if (orgNode != null) {
					if (orgNode.isDescendantOf(compared)) {
						canDelete = false;
						break;
					}
				}
			}
			if (canDelete)
				deleteList.add(candidateForDeletion);
		}

		deleteList.forEach(node -> {
			deleteNode(node, userOpeCmd).ifPresent(newNode -> oldAndNewNodeList.add(new Pair<>(node, newNode)));
		});
		return oldAndNewNodeList;
	}

	/**
	 * 引数で指定したBhNodeを Workspace に移動する (4分木空間への登録は行わないが、4分木空間上の位置は更新する)
	 * @param ws BhNodeを追加したいワークスペース
	 * @param node WS直下に追加したいノード.
	 * @param x ワークスペース上での位置
	 * @param y ワークスペース上での位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void moveToWS(Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {

		if (node.getState() == State.ROOT_DIRECTLY_UNDER_WS)
			removeFromWS(node, userOpeCmd);
		else if (node.getState() == State.CHILD)
			removeChild(node, userOpeCmd);

		Vec2D curPos = MsgService.INSTANCE.getPosOnWS(node);
		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);		//ワークスペースに移動
		MsgService.INSTANCE.setPosOnWS(node, x, y);	//ワークスペース内での位置登録
		userOpeCmd.pushCmdOfAddRootNode(node, ws);
		userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
		UnscopedNodeManager.INSTANCE.collect(node, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
	}

	/**
	 * 引数で指定したBhNodeを Workspace から移動する (4分木空間からの消去は行わない)
	 * @param node WS直下から移動させるノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeFromWS(BhNode node, UserOperationCommand userOpeCmd) {

		Vec2D curPos = MsgService.INSTANCE.getPosOnWS(node);
		Workspace ws = node.getWorkspace();
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_ROOT_NODE, new MsgData(false), node, ws);
		userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
		userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
		UnscopedNodeManager.INSTANCE.collect(node, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
	}

	/**
	 * 子ノードを取り除く (GUIツリー上からは取り除かない)
	 * @param childToRemove 取り除く子ノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 取り除いた子ノードの代わりに作成されたノード
	 */
	public BhNode removeChild(BhNode childToRemove, UserOperationCommand userOpeCmd) {

		Workspace ws = childToRemove.getWorkspace();
		BhNode newNode = childToRemove.remove(userOpeCmd);
		//子ノードを取り除いた結果, 新しくできたノードを4分木空間に登録し, ビューツリーにつなぐ
		WorkspaceRegisterer.register(newNode, ws, userOpeCmd);	//ツリーの各ノードへのWSの登録

		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_QT_RECTANGLE, newNode, ws);
		BhNodeView newNodeView = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), childToRemove);	//ここで4分木空間上での位置も更新される
		userOpeCmd.pushCmdOfAddQtRectangle(newNode, ws);
		userOpeCmd.pushCmdOfReplaceNodeView(childToRemove, newNode);
		UnscopedNodeManager.INSTANCE.collect(childToRemove, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
		return newNode;
	}

	/**
	 * 子ノードを入れ替える
	 * @param oldChildNode 入れ替え対象の古いノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
	 * @param newNode 入れ替え対象の新しいノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void replaceChild(BhNode oldChildNode, BhNode newNode, UserOperationCommand userOpeCmd) {

		if (newNode.getState() == State.ROOT_DIRECTLY_UNDER_WS)
			removeFromWS(newNode, userOpeCmd);
		else if (newNode.getState() == State.CHILD)
			removeChild(newNode, userOpeCmd);

		//新しいノードをビューツリーにつないで, 4分木空間内の位置を更新する
		BhNodeView newNodeView = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), oldChildNode);
		userOpeCmd.pushCmdOfReplaceNodeView(oldChildNode, newNode);

		oldChildNode.replacedWith(newNode, userOpeCmd);	//イミテーションの自動追加は, ビューツリーにつないだ後でなければならないので, モデルの変更はここで行う
		UnscopedNodeManager.INSTANCE.collect(oldChildNode, userOpeCmd);
		UnscopedNodeManager.INSTANCE.collect(newNode, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
	}

	/**
	 * 新しく作られた子ノードを古いノードと入れ替える
	 * @param oldChildNode 入れ替え対象の古いノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
	 * @param newNode 入れ替え対象の新しいノード (新規作成されたノード)
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void replaceChildNewlyCreated(BhNode oldChildNode, BhNode newNode, UserOperationCommand userOpeCmd) {

		//新しいノードを4分木空間に登録し, ビューツリーにつなぐ
		Workspace ws = oldChildNode.getWorkspace();
		WorkspaceRegisterer.register(newNode, ws, userOpeCmd);	//ツリーの各ノードへのWSの登録
		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_QT_RECTANGLE, newNode, ws);
		BhNodeView newNodeView = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), oldChildNode);	//ここで4分木空間上での位置も更新される
		userOpeCmd.pushCmdOfAddQtRectangle(newNode, ws);
		userOpeCmd.pushCmdOfReplaceNodeView(oldChildNode, newNode);

		oldChildNode.replacedWith(newNode, userOpeCmd);	//イミテーションの自動追加は, ビューツリーにつないだ後でなければならないので, モデルの変更はここで行う
		UnscopedNodeManager.INSTANCE.collect(newNode, userOpeCmd);
		UnscopedNodeManager.INSTANCE.collect(oldChildNode, userOpeCmd);
		UnscopedNodeManager.INSTANCE.updateUnscopedNodeWarning(userOpeCmd);
	}

	/**
	 * 2つのノードを入れ替える
	 * @param nodeA 入れ替えたいノード (ダングリング状態のノードはエラー)
	 * @param nodeB 入れ替えたいノード (ダングリング状態のノードはエラー)
	 * */
	public void exchangeNodes(BhNode nodeA, BhNode nodeB, UserOperationCommand userOpeCmd) {

		if (nodeA.getState() == BhNode.State.DELETED ||
			nodeA.getState() == BhNode.State.ROOT_DANGLING ||
			nodeB.getState() == BhNode.State.DELETED ||
			nodeB.getState() == BhNode.State.ROOT_DANGLING) {
			throw new AssertionError("try to exchange dangling/deleted nodes.  "
					+ nodeA.getSymbolName() + "(" + nodeA.getState() + ")    "
					+ nodeB.getSymbolName() + "(" + nodeB.getState() + ")");
		}
		else if (nodeA.isDescendantOf(nodeB) || nodeB.isDescendantOf(nodeA)) {
			throw new AssertionError("try to exchange parent-child relationship nodes.  "
					+ nodeA.getSymbolName() + "    " + nodeB.getSymbolName());
		}

		if (nodeA.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS && nodeB.getState() == BhNode.State.CHILD) {
			//swap
			BhNode tmp = nodeA;
			nodeA = nodeB;
			nodeB = tmp;
		}

		Vec2D posA = MsgService.INSTANCE.getPosOnWS(nodeA);
		Vec2D posB = MsgService.INSTANCE.getPosOnWS(nodeB);
		Workspace wsA = nodeB.getWorkspace();
		Workspace wsB = nodeB.getWorkspace();

		if (nodeA.getState() == BhNode.State.CHILD) {

			// (child, child)
			if (nodeB.getState() == BhNode.State.CHILD) {
				BhNode newNodeA = removeChild(nodeA, userOpeCmd);
				BhNode newNodeB = removeChild(nodeB, userOpeCmd);
				replaceChild(newNodeA, nodeB, userOpeCmd);
				replaceChild(newNodeB, nodeA, userOpeCmd);
				deleteNode(newNodeA, userOpeCmd);
				deleteNode(newNodeB, userOpeCmd);
			}
			// (child, workspace)
			else {
				replaceChild(nodeA, nodeB, userOpeCmd);
				moveToWS(wsB, nodeA, posB.x, posB.y, userOpeCmd);
			}
		}
		//(workspace, workspace)
		else {
			moveToWS(wsA, nodeB, posA.x, posA.y, userOpeCmd);
			moveToWS(wsB, nodeA, posB.x, posB.y, userOpeCmd);
		}
	}
}

















