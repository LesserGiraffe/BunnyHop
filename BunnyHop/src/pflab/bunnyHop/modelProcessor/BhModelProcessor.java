package pflab.bunnyHop.modelProcessor;

import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.model.connective.ConnectorSection;
import pflab.bunnyHop.model.connective.Subsection;

/**
 * BhNode の各ノードに対して何かしらの処理を施すクラスのインタフェース
 * @author K.Koike
 * */
public interface BhModelProcessor {

	/**
	 *  ConnectiveNode が持つ onnerSection, outerSection に自オブジェクトを渡す
	 * @param node 自オブジェクトを渡してもらう ConnectiveNode オブジェクト
	 * */
	default public void visit(ConnectiveNode node) {
		node.introduceSectionsTo(this);
	}

	/**
	 * VoidNode を訪れた時の処理
	 * @param node BhModelProcessor が訪れる VoidNode
	 * */
	default public void visit(VoidNode node) {}

	/**
	 * TextNode を訪れた時の処理
	 * @param node BhModelProcessor が訪れる VoidNode
	 * */
	default public void visit(TextNode node) {}

	/**
	 * Section の下位Sectionに 自オブジェクトを渡す
	 * @param section 自オブジェクトを渡してもらう section オブジェクト
	 * */
	default public void visit(Subsection section) {
		section.introduceSubsectionTo(this);
	}

	/**
	 * ConnectorGroup が持つConnector に自オブジェクトを渡す
	 * @param connectorGroup 自オブジェクトを渡してもらう ConnectorGroup オブジェクト
	 * */
	default public void visit(ConnectorSection connectorGroup) {
		connectorGroup.introduceConnectorsTo(this);
	}

	/**
	 * Connector に接続された ノード に自オブジェクトを渡す
	 * @param connector 自オブジェクトを渡してもらう FixedConnector オブジェクト
	 * */
	default public void visit(Connector connector) {
		connector.introduceConnectedNodeTo(this);
	}
}
