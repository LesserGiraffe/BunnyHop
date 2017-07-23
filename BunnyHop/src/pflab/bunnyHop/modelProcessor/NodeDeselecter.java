package pflab.bunnyHop.modelProcessor;

import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * ノードツリーの全ノードを非選択にする
 * @author K.Koike
 */
public class NodeDeselecter implements BhModelProcessor {

	private final UserOperationCommand userOpeCmd;
	
	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public NodeDeselecter(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}
	
	@Override
	public void visit(ConnectiveNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
		node.introduceSectionsTo(this);
	}
	
	@Override
	public void visit(VoidNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
	}
	
	@Override
	public void visit(TextNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
	}
}
