package pflab.bunnyHop.undo;

import java.util.Deque;
import java.util.LinkedList;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgReceiver;
import pflab.bunnyHop.message.MsgSender;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.model.ImitationInfo;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.view.BhNodeView;

/**
 * undo/redo 用コマンドクラス
 * @author Koike
 */
public class UserOperationCommand {
	
	public UserOperationCommand(){}
	
	private Deque<SubOperation> subOpeList = new LinkedList<>();	//!< このオブジェクトが表す操作を構成するサブ操作のリスト
	
	/**
	 * このコマンドの逆の操作を行う (例えば, ノード追加ならノード削除を行う)
	 * @return このコマンドの逆の操作を表す UserOperationCommand オブジェクトを返す. <br>
	 *          つまり, 戻りオブジェクトの doInverseOperation はこのコマンドの元になった操作を行う
	 */
	UserOperationCommand doInverseOperation() {
		UserOperationCommand inverseCmd = new UserOperationCommand();
		while (!subOpeList.isEmpty()) {
			subOpeList.removeLast().doInverseOperation(inverseCmd);
		}
		return inverseCmd;
	}
	
	/**
	 * サブ操作の数を返す
	 * @return サブ操作の数
	 */
	public int getNumSubOpe() {
		return subOpeList.size();
	}
	
	//for debug
	public void printSubOpeList() {
		for (SubOperation subope : subOpeList) {
			MsgPrinter.instance.MsgForDebug("subope  " + subope);
		}
		MsgPrinter.instance.MsgForDebug("");
	}
	
	/**
	 * メッセージの送り手と受け手の登録をコマンド化してサブ操作リストに加える
	 * @param sender 登録されたメッセージ送信者オブジェクト
	 */
	public void pushCmdOfSetSenderAndReceiver(MsgSender sender) {
		subOpeList.addLast(new SetSenderAndReceiverCmd(sender));
	}

	/**
	 * メッセージの送り手と受け手の削除をコマンド化してサブ操作リストに加える
	 * @param sender 削除されたメッセージ送信者オブジェクト
	 * @param receiver 削除されたメッセージ受信者オブジェクト
	 */	
	public void pushCmdOfDeleteSenderAndReceiver(MsgSender sender, MsgReceiver receiver) {
		subOpeList.addLast(new DeleteSenderAndReceiverCmd(sender, receiver));
	}
	
	/**
	 * イミテーションノードリストへの追加をコマンド化してサブ操作リストに加える
	 * @param <T> ImitationCreator を継承しているクラスの型パラメータ
	 * @param imitInfo イミテーションノードを追加したイミテーションノードリストを持つオブジェクト
	 * @param imit 追加したイミテーションノード
	 */
	public <T extends Imitatable> void pushCmdOfAddImitation(ImitationInfo<T> imitInfo, T imit) {
		subOpeList.addLast(new AddImitationCmd(imitInfo, imit));
	}
	
	/**
	 * イミテーションノードリストからの削除をコマンド化してサブ操作リストに加える
	 * @param <T> ImitationCreator を継承しているクラスの型パラメータ
	 * @param imitInfo イミテーションノードを削除したイミテーションノードリストを持つオブジェクト
	 * @param imit 削除したイミテーションノード
	 */
	public <T extends Imitatable> void pushCmdOfRemoveImitation(ImitationInfo<T> imitInfo, T imit) {
		subOpeList.addLast(new RemoveImitationCmd(imitInfo, imit));
	}
	
	/**
	 * イミテーションのオリジナルノード登録操作をコマンド化してサブ操作リストに加える
	 * @param <T> ImitationCreator を継承しているクラスの型パラメータ
	 * @param imitInfo オリジナルノードを登録するイミテーションノードが持つ ImitationInfo オブジェクト
	 * @param original 元々登録されていたオリジナルノード
	 */		
	public <T extends Imitatable> void pushCmdSetOriginal(ImitationInfo<T> imitInfo, T original) {
		subOpeList.addLast(new SetOriginalCmd(imitInfo, original));
	}
	
	/**
	 * ワークスペース直下へのルートノードの追加をコマンド化してサブ操作リストに加える
	 * @param node ワークスペース直下に追加したルートノード
	 * @param ws ルートノードを追加したワークスペース
	 */
	public void pushCmdOfAddRootNode(BhNode node, Workspace ws) {
		subOpeList.addLast(new AddRootNodeCmd(node, ws));
	}

	/**
	 * ワークスペース直下からのルートノードの削除をコマンド化してサブ操作リストに加える
	 * @param node ワークスペース直下から削除したルートノード
	 * @param ws ルートノードを削除したワークスペース
	 */
	public void pushCmdOfRemoveRootNode(BhNode node, Workspace ws) {
		subOpeList.addLast(new RemoveRootNodeCmd(node, ws));
	}
	
	/**
	 * ワークスペース上での位置指定をコマンド化してサブ操作リストに加える
	 * @param x ワークスペース上での元の位置 x
	 * @param y ワークスペース上での元の位置 y
	 * @param node 位置指定をしたノード
	 */
	public void pushCmdOfSetPosOnWorkspace(double x, double y, BhNode node) {
		subOpeList.addLast(new SetPosOnWorkspaceCmd(x, y, node));
	}
	
	/**
	 * 4分木ノードの4分木空間への登録をコマンド化してサブ操作リストに加える
	 * @param node 4分木ノードを登録するBhNode
	 * @param ws 追加した4分木ノードがあった4分木空間に対応するワークスペース
	 */
	public void pushCmdOfAddQtRectangle(BhNode node, Workspace ws) {
		subOpeList.addLast(new AddQtRectangleCmd(node, ws));
	}
	
	/**
	 * 4分木ノードの4分木空間からの削除をコマンド化してサブ操作リストに加える
	 * @param node 4分木ノードを削除したBhNode
	 * @param ws 削除した4分木ノードがあった4分木空間に対応するワークスペース
	 */
	public void pushCmdOfRemoveQtRectangle(BhNode node, Workspace ws) {
		subOpeList.addLast(new RemoveQtRectangleCmd(node, ws));
	}
	
	/**
	 BhNodeView の入れ替えをコマンド化してサブ操作リストに加える
	 * @param oldNode 入れ替え前の古いView に対応するBhNode
	 * @param newNode 入れ替え後の新しいView に対応するBhNode
	 */
	public void pushCmdOfReplaceNodeView(BhNode oldNode, BhNode newNode) {
		subOpeList.addLast(new ReplaceNodeViewCmd(oldNode, newNode));
	}
	
	/**
	 * BhNode の繋ぎ換えをコマンド化してサブ操作リストに加える
	 * @param oldNode 繋ぎ替え前のBhNode
	 * @param connector ノードのつなぎ替えを行うコネクタ
	 */
	public void pushCmdOfConnectNode(BhNode oldNode, Connector connector) {
		subOpeList.addLast(new ConnectNodeCmd(oldNode, connector));
	}
	
	/**
	 * 入れ替わりノードの登録をコマンド化してサブ操作リストに加える
	 * @param oldNode 元々セットされていたノード
	 * @param nodeRegisteredWith 入れ替わったノードを登録するノード
	 */
	public void pushCmdOfSetLastReplaced(BhNode oldNode, BhNode nodeRegisteredWith) {
		subOpeList.addLast(new SetLastReplacedCmd(oldNode, nodeRegisteredWith));
	}
	
	/**
	 * ノードへのワークスペースの登録をコマンド化してサブ操作リストに加える
	 * @param oldWS 元々セットされていたワークスペース
	 * @param node ワークスペースのセットを行うノード
	 */
	public void pushCmdOfSetWorkspace(Workspace oldWS, BhNode node) {
		subOpeList.addLast(new SetWorkspaceCmd(oldWS, node));
	}

	/**
	 * 選択ノードリストへのノードの追加をコマンド化してサブ操作リストに加える
	 * @param ws 選択ノードリストを持つワークスペース
	 * @param node 選択ノードリストに追加するノード
	 */
	public void pushCmdOfAddSelectedNode(Workspace ws, BhNode node) {
		subOpeList.addLast(new AddSelectedNodeCmd(ws, node));
	}

	/**
	 * 選択ノードリストからのノードの削除をコマンド化してサブ操作リストに加える
	 * @param ws 選択ノードリストを持つワークスペース
	 * @param node 選択ノードリストから削除するノード
	 */
	public void pushCmdOfRemoveSelectedNode(Workspace ws, BhNode node) {
		subOpeList.addLast(new RemoveSelectedNodeCmd(ws, node));
	}
	
	/**
	 * 親のUserOperationCommandを構成するサブ操作
	 */
	interface SubOperation {
		/**
		 * このSubOperation の逆の操作を行う
		 * @param inverseCmd このサブ操作の逆の操作を作るための UserOperationCommand オブジェクト
		 */
		public void doInverseOperation(UserOperationCommand inverseCmd);
	}
	
	/**
	 * メッセージの送り手と受け手の登録を表すコマンド
	 */
	class SetSenderAndReceiverCmd implements SubOperation {
		
		private final MsgSender sender;
		
		public SetSenderAndReceiverCmd(MsgSender sender) {
			this.sender = sender;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			MsgTransporter.instance().deleteSenderAndReceiver(sender, inverseCmd);	//送信者, 受信者登録の反対は削除
		}
	}
	
	/**
	 * メッセージの送り手と受け手の削除を表すコマンド
	 */
	class DeleteSenderAndReceiverCmd implements SubOperation {
		
		private final MsgSender sender;
		private final MsgReceiver receiver;
		
		public DeleteSenderAndReceiverCmd(MsgSender sender, MsgReceiver receiver) {
			this.sender = sender;
			this.receiver = receiver;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			MsgTransporter.instance().setSenderAndReceiver(sender, receiver, inverseCmd); //送信者, 受信者削除の反対は登録
		}
	}

	/**
	 * イミテーションノードリストへの追加を表すコマンド
	 */
	class AddImitationCmd<T extends Imitatable> implements SubOperation {
		
		private final ImitationInfo<T> imitInfo;	//!< イミテーションノードを追加したイミテーションノードリストを持つオブジェクト
		private final T imit;	//!< リストに追加されたイミテーション
		
		public AddImitationCmd(ImitationInfo<T> imitInfo, T imit) {
			this.imitInfo = imitInfo;
			this.imit = imit;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			imitInfo.removeImitation(imit, inverseCmd);
		}
	}
	
	/**
	 * イミテーションノードリストからの削除を表すコマンド
	 */
	class RemoveImitationCmd<T extends Imitatable> implements SubOperation {
		
		private final ImitationInfo<T> imitInfo;	//!< イミテーションノードを削除したイミテーションノードリストを持つオブジェクト
		private final T imit;	//!< リストから削除されたイミテーション
		
		public RemoveImitationCmd(ImitationInfo<T> imitInfo, T imit) {
			this.imitInfo = imitInfo;
			this.imit = imit;
		}

		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			imitInfo.addImitation(imit, inverseCmd);
		}
	}
	
	/**
	 * イミテーションノードにそのオリジナルノードを登録する操作を表すコマンド
	 */
	class SetOriginalCmd<T extends Imitatable> implements SubOperation {

		private final ImitationInfo<T> imitInfo;	//!< オリジナルノードを登録するイミテーションノードが持つ ImitationInfo オブジェクト
		private final T original;	//!< 元々登録されていたオリジナルノード
		
		public SetOriginalCmd(ImitationInfo<T> imitInfo, T original) {
			this.imitInfo = imitInfo;
			this.original = original;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			imitInfo.setOriginal(original, inverseCmd);	//元々登録されていたオリジナルノードをセットする
		}
	}
	
	/**
	 * ワークスペースへのノードの追加を表すコマンド
	 */
	class AddRootNodeCmd implements SubOperation {
		
		private final BhNode node;	//!< ワークスペース直下に追加したノード
		private final Workspace ws;	//!< ノードを追加したワークスペース
		
		public AddRootNodeCmd(BhNode node, Workspace ws) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			MsgTransporter.instance().sendMessage(BhMsg.REMOVE_ROOT_NODE, node, ws);
			inverseCmd.pushCmdOfRemoveRootNode(node, ws);
		}
	}
	
	/**
	 * ワークスペースからのノードの削除を表すコマンド
	 */
	class RemoveRootNodeCmd implements SubOperation {
		
		private final BhNode node;	//!< ワークスペース直下から削除したノード
		private final Workspace ws;	//!< ノードを削除したワークスペース
	
		public RemoveRootNodeCmd(BhNode node, Workspace ws) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			MsgTransporter.instance().sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);
			inverseCmd.pushCmdOfAddRootNode(node, ws);
		}
	}
	
	/**
	 * ワークスペース上の位置指定を表すコマンド
	 */
	class SetPosOnWorkspaceCmd implements SubOperation {
		
		private final double x;	//!< 指定前のワークスペース上での位置X
		private final double y;	//!< 指定前のワークスペース上での位置y
		private final BhNode node;	//!< 位置を指定したノード
		
		public SetPosOnWorkspaceCmd(double x, double y, BhNode node) {
			this.x = x;
			this.y = y;
			this.node = node;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			
			MsgData curPos = MsgTransporter.instance().sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node);
			inverseCmd.pushCmdOfSetPosOnWorkspace(curPos.doublePair._1, curPos.doublePair._2, node);
			MsgTransporter.instance().sendMessage(BhMsg.SET_POS_ON_WORKSPACE, new MsgData(x, y), node);
			MsgTransporter.instance().sendMessage(BhMsg.UPDATE_ABS_POS, node);		//4分木空間での位置確定
		}
	}
	
	/**
	 * 4分木空間への4分木ノード登録を表すコマンド
	 */
	class AddQtRectangleCmd implements SubOperation {
		
		private final BhNode node;	//!< 4分木ノードを登録したBhNode
		private final Workspace ws;	//!< 追加した4分木ノードがあった4分木空間に対応するワークスペース

		public AddQtRectangleCmd(BhNode node, Workspace ws) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			
			MsgTransporter.instance().sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);
			inverseCmd.pushCmdOfRemoveQtRectangle(node, ws);
		}		
	}
	
	/**
	 * 4分木空間からの4分木ノード削除を表すコマンド
	 */
	class RemoveQtRectangleCmd implements SubOperation {
		
		private final BhNode node;	//!< 4分木ノードから削除したBhNode
		private final Workspace ws;	//!< 削除した4分木ノードがあった4分木空間に対応するワークスペース
		
		public RemoveQtRectangleCmd(BhNode node, Workspace ws) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {

			MsgTransporter.instance().sendMessage(BhMsg.ADD_QT_RECTANGLE, node, ws);
			MsgTransporter.instance().sendMessage(BhMsg.UPDATE_ABS_POS, node);		//4分木空間での位置確定
			inverseCmd.pushCmdOfAddQtRectangle(node, ws);
		}		
	}	
	
	/**
	 * BhNodeView の入れ替えを表すコマンド
	 */
	class ReplaceNodeViewCmd implements SubOperation {
	
		private final BhNode oldNode;	//!< 入れ替え前の古いView に対応するBhNode
		private final BhNode newNode;	//!< 入れ替え後の新しいView に対応するBhNode
		
		public ReplaceNodeViewCmd(BhNode oldNode, BhNode newNode) {
			this.oldNode = oldNode;
			this.newNode = newNode;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {

			BhNodeView oldNodeView = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW, oldNode).nodeView;
			MsgTransporter.instance().sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(oldNodeView), newNode);	//元々付いていた古いViewに付け替える
			MsgTransporter.instance().sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, newNode);			
			inverseCmd.pushCmdOfReplaceNodeView(newNode, oldNode);	//逆操作なので, 入れ替え後のノードがoldNode となる
		}		
	}
	
	/**
	 * ノードの接続を表すコマンド
	 */
	class ConnectNodeCmd implements SubOperation {
		
		private final BhNode oldNode;	//!< 繋ぎ替え前のBhNode
		private final Connector connector;	//!< 繋ぎ替えを行うコネクタ

		public ConnectNodeCmd(BhNode oldNode, Connector connector) {
			this.oldNode = oldNode;
			this.connector = connector;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			connector.connectNode(oldNode, inverseCmd);
		}	
	}
	
	/**
	 * 最後に入れ替わったノードをセットする操作を表すコマンド
	 */
	class SetLastReplacedCmd implements SubOperation {
		
		private final BhNode oldNode;	//!< 元々セットされていたノード
		private final BhNode nodeRegisteredWith;	//!< 入れ替わったノードを登録するノード
		
		public SetLastReplacedCmd(BhNode oldNode, BhNode nodeRegisteredWith) {
			this.oldNode = oldNode;
			this.nodeRegisteredWith = nodeRegisteredWith;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			nodeRegisteredWith.setLastReplaced(oldNode, inverseCmd);
		}
	}
	
	/**
	 * ノードに対してワークスペースの登録を行う操作を表すコマンド
	 */
	class SetWorkspaceCmd implements SubOperation {
		
		private final BhNode node;	//!< WSを登録するノード
		private final Workspace oldWS;	//!< WS登録前に登録されていたWS
		
		public SetWorkspaceCmd(Workspace oldWS, BhNode node) {
			this.node = node;
			this.oldWS = oldWS;
		}
		
		@Override 
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			node.setWorkspace(oldWS, inverseCmd);
		}
	}
	
	/**
	 * 選択ノードリストへのBhNode の追加を表すコマンド
	 */
	class AddSelectedNodeCmd implements SubOperation {
		
		private final Workspace ws;	//!< 選択ノードリストを持つワークスペース
		private final BhNode node;	//!< 選択リストに追加するノード
		
		public AddSelectedNodeCmd(Workspace ws, BhNode node) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			ws.removeSelectedNode(node, inverseCmd);
		}
	}
	
	/**
	 * 選択ノードリストへのBhNode の追加を表すコマンド
	 */
	class RemoveSelectedNodeCmd implements SubOperation {
		
		private final Workspace ws;	//!< 選択ノードリストを持つワークスペース
		private final BhNode node;	//!< 選択リストから削除するノード
		
		public RemoveSelectedNodeCmd(Workspace ws, BhNode node) {
			this.node = node;
			this.ws = ws;
		}
		
		@Override
		public void doInverseOperation(UserOperationCommand inverseCmd) {
			ws.addSelectedNode(node, inverseCmd);
		}
	}
}
