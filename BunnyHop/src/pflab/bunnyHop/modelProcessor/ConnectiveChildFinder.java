package pflab.bunnyHop.modelProcessor;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.model.connective.ConnectorSection;
import pflab.bunnyHop.model.connective.Subsection;

/**
 * イミテーションタグを指定し, 接続されているBhNode を見つける
 * @author K.Koike
 */
public class ConnectiveChildFinder implements BhModelProcessor {
	
	BhNode foundNode;	//!< 見つかったノード
	String imitTag;	//!< 接続されている探したい接続先のコネクタ名
	private boolean found = false;
	
	/**
	 * コンストラクタ
	 * @param imitTag このイミテーションタグを持つコネクタにつながったBhNodeを見つける
	 */
	public ConnectiveChildFinder(String imitTag){
		this.imitTag = imitTag;
	}
	
	@Override
	public void visit(ConnectiveNode node) {
		node.introduceSectionsTo(this);
	}

	@Override
	public void visit(Subsection section) {
		if (found)
			return;
		section.introduceSubsectionTo(this);
	}
	
	@Override
	public void visit(ConnectorSection connectorGroup) {
		if (found)
			return;
		connectorGroup.introduceConnectorsTo(this);
	}

	@Override
	public void visit(Connector connector) {
		if (found)
			return;
		
		if (connector.getImitationTag().equals(imitTag)) {
			foundNode = connector.getConnectedNode();
			found = true;
		}
	}
	
	/**
	 * コネクティブノードを捜査した結果見つかったBhNode を返す. <br>
	 * 何も見つからなかった場合は null を返す
	 * @return コネクティブノードを捜査した結果見つかったBhNode
	 */
	public BhNode getFoundNode() {
		return foundNode;
	}
}
