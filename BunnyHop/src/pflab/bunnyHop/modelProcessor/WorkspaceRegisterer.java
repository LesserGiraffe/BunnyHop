package pflab.bunnyHop.modelProcessor;

import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * ノードに対してワークスペースをセットする
 * @author K.Koike
 */
public class WorkspaceRegisterer implements BhModelProcessor{
	
	private final Workspace ws;	//!< 登録されるワークスペース
	private final UserOperationCommand userOpeCmd;
	
	/**
	 * @param ws 登録されるワークスペース<br> nullを指定するとノードと所属しているワークスペースの関連が無くなる
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public WorkspaceRegisterer(Workspace ws, UserOperationCommand userOpeCmd) {
		this.ws = ws;
		this.userOpeCmd = userOpeCmd;
	}
	
	@Override
	public void visit(ConnectiveNode node) {
		node.setWorkspace(ws, userOpeCmd);
		node.introduceSectionsTo(this);
	}
	
	@Override
	public void visit(VoidNode node) {
		node.setWorkspace(ws, userOpeCmd);
	}

	@Override
	public void visit(TextNode node) {
		node.setWorkspace(ws, userOpeCmd);
	}
}
