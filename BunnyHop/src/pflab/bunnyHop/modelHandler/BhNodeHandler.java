package pflab.bunnyHop.modelHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import pflab.bunnyHop.modelProcessor.NodeDeselecter;
import pflab.bunnyHop.modelProcessor.WorkspaceRegisterer;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.BhNodeView;

/**
 * BhNodeの追加, 移動, 入れ替え, 削除用関数を提供するクラス
 * @author K.Koike
 */
public class BhNodeHandler {
	
	public static final BhNodeHandler instance = new BhNodeHandler();	//!< シングルトンインスタンス
	
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

		MsgData curPos = MsgTransporter.instance().sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node);
		node.accept(new WorkspaceRegisterer(ws, userOpeCmd));							//ツリーの各ノードへのWSの登録
		MsgTransporter.instance().sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);		//ワークスペース直下に追加
		MsgTransporter.instance().sendMessage(BhMsg.ADD_QT_RECTANGLE, node, ws);	//4分木ノード登録(重複登録はされない)
		MsgTransporter.instance().sendMessage(BhMsg.SET_POS_ON_WORKSPACE, new MsgData(x, y), node);	//ワークスペース内での位置登録
		MsgTransporter.instance().sendMessage(BhMsg.UPDATE_ABS_POS, node);		//4分木空間での位置確定

		userOpeCmd.pushCmdOfAddRootNode(node, ws);
		userOpeCmd.pushCmdOfAddQtRectangle(node, ws);
		userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.doublePair._1, curPos.doublePair._2, node);
	}

	/**
	 * 引数で指定したBhNodeを削除する
	 * @param node WSから取り除きたいノード. 
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void deleteNode(BhNode node, UserOperationCommand userOpeCmd) {
		
		BhNode rootNode = node.findRootNode();
		if (DelayedDeleter.instance.containsInCandidateList(rootNode)) {
			DelayedDeleter.instance.deleteCandidate(rootNode, userOpeCmd);
			return;
		}
				
		Workspace ws = node.getWorkspace();
		BhNode.State nodeState = node.getState();
		switch(nodeState) {
			case CHILD:
				removeChild(node, userOpeCmd);
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除				
				break;
				
			case ROOT_DANGLING:
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除				
				break;
			
			case ROOT_DIRECTLY_UNDER_WS:
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_ROOT_NODE, node, ws);	 //WS直下から削除
				userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
				break;				
		}
		
		MsgTransporter.instance().sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);		 //4分木空間からの削除
		userOpeCmd.pushCmdOfRemoveQtRectangle(node, ws);
		node.accept(new NodeDeselecter(userOpeCmd));
		node.accept(new WorkspaceRegisterer(null, userOpeCmd));	//ノードの登録されたWSを削除
		node.delete(userOpeCmd);
	}
	
	/**
	 * 引数で指定したBhNodeをモデルのつながりだけ残して削除する <br>
	 * ワークスペース, GUI, 4分木空間上からは消える.<br>
	 * イミテーション -オリジナルの関係, メッセージ送信者 - 受信者の登録は残る
	 * @param node 仮削除するノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteNodeIncompletely(BhNode node, UserOperationCommand userOpeCmd) {
		
		if (DelayedDeleter.instance.containsInCandidateList(node.findRootNode())) {
			return;
		}
		
		Workspace ws = node.getWorkspace();
		BhNode.State nodeState = node.getState();
		switch(nodeState) {
			case CHILD:
				removeChild(node, userOpeCmd);
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除				
				break;
				
			case ROOT_DANGLING:
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);	//GUIツリー上から削除				
				break;
			
			case ROOT_DIRECTLY_UNDER_WS:
				MsgTransporter.instance().sendMessage(BhMsg.REMOVE_ROOT_NODE, node, ws);	 //WS直下から削除
				userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
				break;				
		}
		
		MsgTransporter.instance().sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);		 //4分木空間からの削除
		userOpeCmd.pushCmdOfRemoveQtRectangle(node, ws);
		node.accept(new NodeDeselecter(userOpeCmd));
		node.accept(new WorkspaceRegisterer(null, userOpeCmd));	//ノードに対して登録されたWSを削除
		DelayedDeleter.instance.addDeletionCandidate(node);
	}
	
	/**
	 * 引数で指定したノードを全て削除する
	 * @param deletedNodeList 削除するノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteNodes(Collection<? extends BhNode> deletedNodeList, UserOperationCommand userOpeCmd) {

		List<BhNode> deleteList = new ArrayList<>();		
		for (BhNode candidateForDeletion : deletedNodeList) {
			boolean canDelete = true;			
			for (BhNode compared : deletedNodeList) {
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
			BhNodeHandler.instance.deleteNode(node, userOpeCmd);
		});
	}
	
	/**
	 * 引数で指定したBhNodeを Workspace に移動する (4分木空間への登録は行わない)
	 * @param ws BhNodeを追加したいワークスペース
	 * @param node WS直下に追加したいノード.
	 * @param x ワークスペース上での位置
	 * @param y ワークスペース上での位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void moveToWS(Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {
		
		MsgData curPos = MsgTransporter.instance().sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node);
		MsgTransporter.instance().sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);		//ワークスペースに移動
		MsgTransporter.instance().sendMessage(BhMsg.SET_POS_ON_WORKSPACE, new MsgData(x, y), node);	//ワークスペース内での位置登録
		MsgTransporter.instance().sendMessage(BhMsg.UPDATE_ABS_POS, node);		//4分木空間での位置確定
		userOpeCmd.pushCmdOfAddRootNode(node, ws);
		userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.doublePair._1, curPos.doublePair._2, node);
	}
	
	/**
	 * 引数で指定したBhNodeを Workspace から移動する (4分木空間からの消去は行わない)
	 * @param node WS直下から移動させるノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になる.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeFromWS(BhNode node, UserOperationCommand userOpeCmd) {
		
		Workspace ws = node.getWorkspace();
		MsgTransporter.instance().sendMessage(BhMsg.REMOVE_ROOT_NODE, node, ws);
		userOpeCmd.pushCmdOfRemoveRootNode(node, ws);
	}

	/**
	 * 子ノードを取り除く (GUIイベント受信の都合上GUIツリー上からは取り除かない)
	 * @param removed 取り除く子ノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になる.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void removeChild(BhNode removed, UserOperationCommand userOpeCmd) {
		
		Workspace ws = removed.getWorkspace();
		BhNode newNode = removed.remove(userOpeCmd);
		//子ノードを取り除いた結果, 新しくできたノードを4分木空間に登録し, ビューツリーにつなぐ
		newNode.accept(new WorkspaceRegisterer(ws, userOpeCmd));					//ツリーの各ノードへのWSの登録
		MsgTransporter.instance().sendMessage(BhMsg.ADD_QT_RECTANGLE, newNode, ws);
		BhNodeView newNodeView = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.instance().sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), removed);	//ここで4分木空間上での位置も更新される
		userOpeCmd.pushCmdOfAddQtRectangle(newNode, ws);
		userOpeCmd.pushCmdOfReplaceNodeView(removed, newNode);
	}
	
	/**
	 * 子ノードを入れ替える
	 * @param oldNode 入れ替え対象の古いノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になる.
	 * @param newNode 入れ替え対象の新しいノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void replaceChild(BhNode oldNode, BhNode newNode, UserOperationCommand userOpeCmd) {
		
		//新しいノードをビューツリーにつないで, 4分木空間内の位置を更新する
		BhNodeView newNodeView = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.instance().sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), oldNode);
		userOpeCmd.pushCmdOfReplaceNodeView(oldNode, newNode);
		
		oldNode.replacedWith(newNode, userOpeCmd);	//イミテーションの自動追加は, ビューツリーにつないだ後でなければならないので, モデルの変更はここで行う
	}
	
	/**
	 * 新しく作られた子ノードを古いノードと入れ替える
	 * @param oldNode 入れ替え対象の古いノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になる.
	 * @param newNode 入れ替え対象の新しいノード (新規作成されたノード)
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void replaceChildNewlyCreated(BhNode oldNode, BhNode newNode, UserOperationCommand userOpeCmd) {
		
		//新しいノードを4分木空間に登録し, ビューツリーにつなぐ
		Workspace ws = oldNode.getWorkspace();
		newNode.accept(new WorkspaceRegisterer(ws, userOpeCmd));					//ツリーの各ノードへのWSの登録
		MsgTransporter.instance().sendMessage(BhMsg.ADD_QT_RECTANGLE, newNode, ws);
		BhNodeView newNodeView = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW, newNode).nodeView;
		MsgTransporter.instance().sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), oldNode);	//ここで4分木空間上での位置も更新される
		userOpeCmd.pushCmdOfAddQtRectangle(newNode, ws);
		userOpeCmd.pushCmdOfReplaceNodeView(oldNode, newNode);
		
		oldNode.replacedWith(newNode, userOpeCmd);	//イミテーションの自動追加は, ビューツリーにつないだ後でなければならないので, モデルの変更はここで行う
	}
}
