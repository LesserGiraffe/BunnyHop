package pflab.bunnyHop.model;

import java.io.Serializable;
import java.util.List;
import pflab.bunnyHop.ModelProcessor.BhModelProcessor;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * コネクタに何も繋がっていないことを表す終端BhNode
 * @author K.Koike
 * */
public class VoidNode extends BhNode implements Serializable {

	/**
	 * コンストラクタ<br>
	 * ノード内部・外部とは描画位置のこと
	 * @param bhID ノードID (\<Node\> タグの bhID)
	 * @param symbolName  終端, 非終端記号名
	 * */
	public VoidNode(
		String bhID,
		String symbolName) {
		super(
			bhID, 
			symbolName,
			BhParams.BhModelDef.attrValueVoid,
			"",
			"");
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private VoidNode(VoidNode org) {
		super(org);
	}
	
	@Override
	public VoidNode copy(UserOperationCommand userOpeCmd) {
		return new VoidNode(this);
	}

	@Override 
	public Imitatable getOriginalNode() {
		return null;
	}	

	/**
	 * BhModelProcessor に自オブジェクトを渡す
	 * @param processor 自オブジェクトを渡す BhModelProcessorオブジェクト
	 * */
	@Override
	public void accept(BhModelProcessor processor) {
		processor.visit(this);
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		String lastReplacedHash = "";
		if (getLastReplaced() != null)
			lastReplacedHash =  getLastReplaced().hashCode() + "";
		
		String parentHashCode = null;
		if (parentConnector != null)
			parentHashCode = parentConnector.hashCode() + "";
		
		MsgPrinter.instance.MsgForDebug(indent(depth) + "<voidNode" + "  bhID=" + getID() + "  parent="+ parentHashCode + "> " + this.hashCode());
		MsgPrinter.instance.MsgForDebug(indent(depth+1) + "<" + "last replaced " + lastReplacedHash + "> ");
	}
	
	@Override
	public boolean isRemovable() {		
		return false;
	}
	
	@Override
	public boolean canBeReplacedWith(BhNode node) {
		
		if (getState() != BhNode.State.CHILD)
			return false;
		
		if (findRootNode().getState() == BhNode.State.ROOT_DANGLING)
			return false;

		if (node.isDescendantOf(this) || this.isDescendantOf(node))	//同じtree に含まれている場合置き換え不可
			return false;
		
		do {
			if (!(node instanceof Imitatable))
				break;
			
			Imitatable imitNode = (Imitatable)node;
			if (!imitNode.isImitationNode())
				break;
			
			if (imitNode.getImitationInfo().scopeName.isEmpty())
				break;
			
			SyntaxSymbol scope = imitNode.getOriginalNode().findSymbolInAncestors(imitNode.getImitationInfo().scopeName, 1, true);
			if (scope == null)	//可視範囲制限なし
				break;
			
			if (!this.isDescendantOf(scope))
				return false;
		}
		while(false);
		
		return parentConnector.canConnectedNodeBeReplacedWith(node);
	}
	
	@Override
	public BhNode findOuterEndNode() {
		return this;
	}
	
	@Override
	public void findSymbolInDescendants(String symbolName, int hierarchyLevel, boolean toBottom, List<SyntaxSymbol> foundSymbolList) {
		
		if (hierarchyLevel == 0)
			if (Util.equals(getSymbolName(), symbolName))
				foundSymbolList.add(this);
	}
}






