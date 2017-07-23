package pflab.bunnyHop.modelProcessor;

import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.model.connective.ConnectorSection;
import pflab.bunnyHop.model.connective.Subsection;

/**
 * SymtaxSymbolオブジェクトにユニークなIDを付与する
 * @author K.Koike
 */
public class SyntaxSymbolIDCreator implements BhModelProcessor {

	private long id = 1;
	
	public SyntaxSymbolIDCreator() {}
	
	@Override
	public void visit(ConnectiveNode node) {
		node.setSymbolID(Long.toHexString(id++));
		node.introduceSectionsTo(this);
	}

	@Override
	public void visit(VoidNode node) {
		node.setSymbolID(Long.toHexString(id++));
	}

	@Override
	public void visit(TextNode node) {
		node.setSymbolID(Long.toHexString(id++));
	}

	@Override
	public void visit(Subsection section) {
		section.setSymbolID(Long.toHexString(id++));
		section.introduceSubsectionTo(this);
	}

	@Override
	public void visit(ConnectorSection connectorGroup) {
		connectorGroup.setSymbolID(Long.toHexString(id++));
		connectorGroup.introduceConnectorsTo(this);
	}

	@Override
	public void visit(Connector connector) {
		connector.setSymbolID(Long.toHexString(id++));
		connector.introduceConnectedNodeTo(this);
	}
}
