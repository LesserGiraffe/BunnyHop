package pflab.bunnyHop.modelProcessor;

import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * ノードツリーの削除を行うクラス
 * @author K.Koike
 */
public class NodeDeleter implements BhModelProcessor {
	
	UserOperationCommand userOpeCmd;	//!< undo用コマンドオブジェクト
	
	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public NodeDeleter(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}
	
	/**
	 * node の削除処理を行う
	 * @param node 削除するノード
	 * */
	@Override
	public void visit(ConnectiveNode node) {

		//このノードがイミテーションノードだった場合, オリジナルにイミテーションが消えたことを伝える
		if (node.isImitationNode())
			node.getOriginalNode().disconnectOrgImitRelation(node, userOpeCmd);

		node.introduceSectionsTo(this);		
		node.getImitationInfo().deleteAllImitations(userOpeCmd);	//オリジナルが消えた場合, イミテーションも消える
		MsgTransporter.instance().deleteSenderAndReceiver(node, userOpeCmd);
	}

	@Override
	public void visit(VoidNode node) {		
		MsgTransporter.instance().deleteSenderAndReceiver(node, userOpeCmd);
	}
	
	@Override
	public void visit(TextNode node) {

		//このノードがイミテーションノードだった場合, オリジナルにイミテーションが消えたことを伝える
		if (node.isImitationNode())
			node.getOriginalNode().disconnectOrgImitRelation(node, userOpeCmd);

		node.getImitationInfo().deleteAllImitations(userOpeCmd);	//オリジナルが消えた場合, イミテーションも消える
		MsgTransporter.instance().deleteSenderAndReceiver(node, userOpeCmd);
	}
}
