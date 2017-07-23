package pflab.bunnyHop.modelProcessor;

import java.util.ArrayList;
import java.util.List;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;

/**
 * スコープ外のノードを探して集めるクラス
 * @author K.Koike
 * */
public class UnscopedNodeCollector implements BhModelProcessor {

	private final List<Imitatable> unscopedNodeList = new ArrayList<>();	//!< オリジナルノードと同じスコープに居ないイミテーションノードのリスト
	
	/**
	 * オリジナルノードと同じスコープに居ないイミテーションノードのリストを返す
	 * @return オリジナルノードと同じスコープに居ないイミテーションノードのリスト
	 */
	public List<Imitatable> getUnscopedNodeList() {
		return unscopedNodeList;
	}
	
	@Override
	public void visit(ConnectiveNode node) {		
		
		node.introduceSectionsTo(this);
		node.getImitationInfo().getImitationList().forEach(
			imitNode -> {
				if (imitNode.isUnscoped())
					unscopedNodeList.add(imitNode);
		});
		
		if (node.isUnscoped())
			unscopedNodeList.add(node);
	}
	
	@Override
	public void visit(TextNode node) {
		
		node.getImitationInfo().getImitationList().forEach(
			imitNode -> {
				if (imitNode.isUnscoped())
					unscopedNodeList.add(imitNode);
		});
			
		if (node.isUnscoped())
			unscopedNodeList.add(node);
	}
}
