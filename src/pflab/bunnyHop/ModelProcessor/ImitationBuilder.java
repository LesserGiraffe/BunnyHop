package pflab.bunnyHop.ModelProcessor;

import java.util.Deque;
import java.util.LinkedList;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.undo.UserOperationCommand;
	
/**
 * イミテーションノードツリーを作成する
 * @author Koike
 */
public class ImitationBuilder implements BhModelProcessor {
	
	final private Deque<Imitatable> parentImitStack = new LinkedList<>();	//!< 現在処理中のBhNode の親がトップにくるスタック
	UserOperationCommand userOpeCmd;	//!< undo用コマンドオブジェクト
	boolean isManualCreation = false;	//!< トップノードのイミテーションを手動作成する場合true
		
	public ImitationBuilder(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}
	
	public ImitationBuilder(UserOperationCommand userOpeCmd, boolean isManualCreation) {
		this.userOpeCmd = userOpeCmd;
		this.isManualCreation = isManualCreation;
	}	
	
	/**
	 * 作成したイミテーションノードツリーのトップノードを取得する
	 * @return 作成したイミテーションノードツリーのトップノード
	 */
	public Imitatable getTopImitation() {
		return parentImitStack.peekLast();
	}

	/**
	 * @param node イミテーションを作成して、入れ替えを行いたいオリジナルノード
	 */
	@Override
	public void visit(ConnectiveNode node) {
		
		String imitTag = null;
		if (isManualCreation) {
			imitTag = BhParams.BhModelDef.attrValueTagManual;
			isManualCreation = false;
		}
		else if (node.getParentConnector() != null) {
			imitTag = node.getParentConnector().getImitationTag();
		}
		
		if (!node.getImitationInfo().hasImitationID(imitTag))
			return;
		
		if (parentImitStack.isEmpty()) {
			ConnectiveNode newImit = node.createImitNode(userOpeCmd, imitTag);
			parentImitStack.addLast(newImit);
			node.introduceSectionsTo(this);
			newImit.accept(new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, userOpeCmd));
		}
		else {
			Imitatable parentImit = parentImitStack.peekLast();
			//接続先を探す
			ConnectiveChildFinder finder = new ConnectiveChildFinder(imitTag);
			parentImit.accept(finder);
			BhNode oldImit = finder.getFoundNode();
			if (oldImit != null) {
				ConnectiveNode newImit = node.createImitNode(userOpeCmd, imitTag);
				oldImit.replacedWith(newImit, userOpeCmd);
				parentImitStack.addLast(newImit);
				node.introduceSectionsTo(this);
				parentImitStack.removeLast();
			}
		}
	}
	
	@Override
	public void visit(TextNode node) {
		
		String imitTag = null;
		if (isManualCreation) {
			imitTag = BhParams.BhModelDef.attrValueTagManual;
			isManualCreation = false;
		}
		else if (node.getParentConnector() != null) {
			imitTag = node.getParentConnector().getImitationTag();
		}
		
		if (!node.getImitationInfo().hasImitationID(imitTag))
			return;
		
		if (parentImitStack.isEmpty()) {
			TextNode newImit = node.createImitNode(userOpeCmd, imitTag);
			parentImitStack.addLast(newImit);
			newImit.accept(new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, userOpeCmd));
		}
		else {
			Imitatable parentImit = parentImitStack.peekLast();
			//接続先を探す
			ConnectiveChildFinder finder = new ConnectiveChildFinder(imitTag);
			parentImit.accept(finder);
			BhNode oldImit = finder.getFoundNode();
			if (oldImit != null) {
				TextNode newImit = node.createImitNode(userOpeCmd, imitTag);
				oldImit.replacedWith(newImit, userOpeCmd);
			}			
		}
	}
}
