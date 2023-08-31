/**
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.seapanda.bunnyhop.model.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeID;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeViewType;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorID;
import net.seapanda.bunnyhop.model.node.connective.ConnectorSection;
import net.seapanda.bunnyhop.model.node.connective.Section;
import net.seapanda.bunnyhop.model.node.connective.Subsection;
import net.seapanda.bunnyhop.model.node.imitation.ImitationConnectionPos;
import net.seapanda.bunnyhop.model.node.imitation.ImitationID;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;

/**
 * \<Node\> タグ以下の情報からBhNodeを作成する
 * @author K.Koike
 */
public class NodeConstructor {

	private final BiConsumer<BhNodeID, BhNode> registerNodeTemplate;	//!< ノードテンプレート登録用関数
	private final BiConsumer<ConnectorID, Connector> registerCnctrTemplate;	//!< コネクタテンプレート登録用関数
	private final BiConsumer<BhNodeID, BhNodeID> registerOrgNodeIdAndImitNodeID;	//!< オリジナル & イミテーションノード格納用関数
	private final Function<ConnectorID, Optional<Connector>> getCnctrTemplate;	//!< コネクタテンプレート取得用関数

	public NodeConstructor(
		BiConsumer<BhNodeID, BhNode> registerNodeTemplate,
		BiConsumer<ConnectorID, Connector> registerCnctrTemplate,
		BiConsumer<BhNodeID, BhNodeID> registerOrgNodeIdAndImitNodeID,
		Function<ConnectorID, Optional<Connector>> getCnctrTemplate) {

		this.registerNodeTemplate = registerNodeTemplate;
		this.registerCnctrTemplate = registerCnctrTemplate;
		this.registerOrgNodeIdAndImitNodeID = registerOrgNodeIdAndImitNodeID;
		this.getCnctrTemplate = getCnctrTemplate;
	}

	/**
	 * ノードテンプレートを作成する
	 * @param doc ノードテンプレートを作成するxml の Document オブジェクト
	 * @return 作成したBhNodeオブジェクト
	 */
	public Optional<? extends BhNode> genTemplate(Document doc) {

		if (!doc.getFirstChild().getNodeName().equals(BhParams.BhModelDef.ELEM_NAME_NODE)) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"ノード定義のルート要素は " + BhParams.BhModelDef.ELEM_NAME_NODE + " で始めてください.  " + doc.getBaseURI());
			return Optional.empty();
		}

		Element rootElement = doc.getDocumentElement();
		return genTemplate(rootElement);
	}

	/**
	 * ノードテンプレートを作成する
	 * @param nodeRoot \<Node\> タグを表す要素
	 * @return 作成したBhNodeオブジェクト
	 */
	public Optional<? extends BhNode> genTemplate(Element nodeRoot) {

		Optional<? extends BhNode> templateNode = Optional.empty();
		String typeName = nodeRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_TYPE);
		BhNodeViewType type = BhNodeViewType.toType(typeName);

		switch (type) {
			//<Node type="connective">
			case CONNECTIVE:
				templateNode = genConnectiveNode(nodeRoot);
				break;

			//<Node type="textField">
			//<Node type="comboBox">
			//<Node type="label">
			case TEXT_FIELD:
			case COMBO_BOX:
			case LABEL:
			case TEXT_AREA:
				templateNode = genTextNode(nodeRoot, type, true);
				break;
			//<Node type="noView">
			//<Node type="noContent">
			case NO_VIEW:
			case NO_CONTENT:
				templateNode = genTextNode(nodeRoot, type, false);
				break;

			default:
				MsgPrinter.INSTANCE.errMsgForDebug(
					BhParams.BhModelDef.ATTR_NAME_TYPE + "=" + type + " はサポートされていません.\n" + nodeRoot.getBaseURI() + "\n");
				break;
		}
		return templateNode;
	}

	/**
	 * \<Imitation\> タグの情報を取得する
	 * @param node イミテーションノードに関する情報が書いてあるxmlタグをあらわすノード
	 * @param orgNodeID イミテーションを持つノードのID
	 * @param canCreateImitManually イミテーションを手動作成できる場合true
	 * @return イミテーションIDとイミテーションノードIDのマップ
	 */
	private Optional<Map<ImitationID, BhNodeID>> genImitIDAndNodePair(Element node, BhNodeID orgNodeID, boolean canCreateImitManually) {

		boolean success = true;
		Map<ImitationID, BhNodeID> imitIdToImitNodeId = new HashMap<>();
		List<Element> imitTagList = BhNodeTemplates.getElementsByTagNameFromChild(node, BhParams.BhModelDef.ELEM_NAME_IMITATION);

		for (Element imitTag : imitTagList) {
			ImitationID imitationID = ImitationID.create(imitTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_ID));
			if (imitationID.equals(ImitationID.NONE)) {
				MsgPrinter.INSTANCE.errMsgForDebug(
					BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグには, " +
					BhParams.BhModelDef.ATTR_NAME_IMITATION_ID + " 属性を記述してください. " + node.getBaseURI());
				success &= false;
				continue;
			}

			BhNodeID imitNodeID = BhNodeID.create(imitTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_NODE_ID));
			if (imitNodeID.equals(BhNodeID.NONE)) {
				MsgPrinter.INSTANCE.errMsgForDebug(
					BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグには, " +
					BhParams.BhModelDef.ATTR_NAME_IMITATION_NODE_ID + " 属性を記述してください. " + node.getBaseURI());
				success &= false;
				continue;
			}
			imitIdToImitNodeId.put(imitationID, imitNodeID);
			registerOrgNodeIdAndImitNodeID.accept(orgNodeID, imitNodeID);
		}

		if (canCreateImitManually && (imitIdToImitNodeId.get(ImitationID.MANUAL) == null)) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				BhParams.BhModelDef.ATTR_NAME_CAN_CREATE_IMIT_MANUALLY + " 属性が " + 
				BhParams.BhModelDef.ATTR_VALUE_TRUE + " のとき " +
				BhParams.BhModelDef.ATTR_NAME_IMITATION_ID + " 属性に " +
				BhParams.BhModelDef.ATTR_VALUE_IMIT_ID_MANUAL + " を指定した" +
				BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグを作る必要があります. " + node.getBaseURI());
			success &= false;
		}
		if (!success) {
			return Optional.empty();
		}

		return Optional.of(imitIdToImitNodeId);
	}

	/**
	 * コネクティブノードを構築する
	 * @param node \<Node\> タグを表すオブジェクト
	 * @return ConnectiveNodeオブジェクト(Option)
	 */
	private Optional<ConnectiveNode> genConnectiveNode(Element node) {

		Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
		if (!nodeAttrs.isPresent()) {
			return Optional.empty();
		}

		Optional<ArrayList<Section>> childSection = genSectionList(node);
		if (!childSection.isPresent()) {
			return Optional.empty();
		}

		if (childSection.get().size() != 1) {
			MsgPrinter.INSTANCE.errMsgForDebug(BhParams.BhModelDef.ATTR_NAME_TYPE + " が "
				+ BhParams.BhModelDef.ATTR_VALUE_CONNECTIVE + " の "
				+ BhParams.BhModelDef.ELEM_NAME_NODE + " タグは, "
				+ BhParams.BhModelDef.ELEM_NAME_SECTION + " または " + BhParams.BhModelDef.ELEM_NAME_CONNECTOR_SECTION + " 子タグを1つ持たなければなりません. " + node.getBaseURI());
			return Optional.empty();
		}

		//実行時スクリプトチェック
		boolean allScriptsFound = BhNodeTemplates.allScriptsExist(
			node.getBaseURI(),
			nodeAttrs.get().getOnMovedFromChildToWS(),
			nodeAttrs.get().getOnMovedToChild(),
			nodeAttrs.get().getOnChildReplaced(),
			nodeAttrs.get().getOnDeletionRequested(),
			nodeAttrs.get().getOnCutRequested(),
			nodeAttrs.get().getOnCopyRequested(),
			nodeAttrs.get().getSyntaxErrorChecker(),
			nodeAttrs.get().getOnPrivateTemplateCreating());
		if (!allScriptsFound)
			return Optional.empty();

		BhNodeID orgNodeID = nodeAttrs.get().getBhNodeID();
		Optional<Map<ImitationID, BhNodeID>> imitIdToImitNodeID =
			genImitIDAndNodePair(node, orgNodeID, nodeAttrs.get().getCanCreateImitManually());
		if (!imitIdToImitNodeID.isPresent())
			return Optional.empty();

		return Optional.of(new ConnectiveNode(childSection.get().get(0), imitIdToImitNodeID.get(), nodeAttrs.get()));
	}

	/**
	 * テキストフィールドを持つノードを構築する
	 * @param node \<Node\> タグを表すオブジェクト
	 * @param type 関連する BhNodeView の種類
	 * @param checkViewComponent GUI部品の有無をチェックする場合true
	 * @return TextFieldオブジェクト (Option)
	 */
	private Optional<TextNode> genTextNode(Element node, BhNodeViewType type, boolean checkViewComponent) {

		Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
		if (!nodeAttrs.isPresent()) {
			return Optional.empty();
		}

		//実行時スクリプトチェック
		boolean allScriptsFound = BhNodeTemplates.allScriptsExist(node.getBaseURI(),
			nodeAttrs.get().getOnMovedFromChildToWS(),
			nodeAttrs.get().getOnMovedToChild(),
			nodeAttrs.get().getTextAcceptabilityChecker(),
			nodeAttrs.get().getOnDeletionRequested(),
			nodeAttrs.get().getOnCutRequested(),
			nodeAttrs.get().getOnCopyRequested(),
			nodeAttrs.get().getTextFormatter(),
			nodeAttrs.get().getSyntaxErrorChecker(),
			nodeAttrs.get().getOnPrivateTemplateCreating());
		if (!allScriptsFound) {
			return Optional.empty();
		}

		if (checkViewComponent && nodeAttrs.get().getNodeInputControlFileName().isEmpty()) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				BhParams.BhModelDef.ATTR_NAME_TYPE + " 属性が " + type + " の " +
				"<" + BhParams.BhModelDef.ELEM_NAME_NODE + "> タグは " +
				BhParams.BhModelDef.ATTR_NAME_NODE_INPUT_CONTROL + 
				" 属性でGUI入力部品のfxmlファイルを指定しなければなりません.\n" +
				node.getBaseURI() + "\n");
			return Optional.empty();
		}
		else {
			BhNodeViewStyle.nodeIdToInputControlFileName.put(
				nodeAttrs.get().getBhNodeID(), nodeAttrs.get().getNodeInputControlFileName());
		}

		Optional<Map<ImitationID, BhNodeID>> imitIdToImitNodeId =
			genImitIDAndNodePair(node, nodeAttrs.get().getBhNodeID(), nodeAttrs.get().getCanCreateImitManually());
		if (!imitIdToImitNodeId.isPresent())
			return Optional.empty();

		return Optional.of(new TextNode(type, imitIdToImitNodeId.get(), nodeAttrs.get()));
	}

	/**
	 * parentTag で指定したタグより下の Section リストを作成する
	 * @param parentTag \<Section\> or \<ConnectorSection\> タグを子に持つタグ
	 * @return parentTag より下の Section リスト<br>
	 */
	private Optional<ArrayList<Section>> genSectionList(Element parentTag) {

		if (parentTag == null) {
			return Optional.of(new ArrayList<>());
		}

		ArrayList<Optional<? extends Section>> sectionListTmp = new ArrayList<>();

		NodeList sections = parentTag.getChildNodes();
		for (int i = 0; i < sections.getLength(); ++i) {

			Node childNode = sections.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) //子タグ以外処理しない
			{
				continue;
			}

			if (childNode.getNodeName().equals(BhParams.BhModelDef.ELEM_NAME_CONNECTOR_SECTION)) {	//parentTag の子ノードの名前が ConnectorSection
				Optional<ConnectorSection> connectorGroup = genConnectorSection((Element) childNode);
				sectionListTmp.add(connectorGroup);
			} else if (childNode.getNodeName().equals(BhParams.BhModelDef.ELEM_NAME_SECTION)) {	//parentTag の子ノードの名前が Section
				Optional<Subsection> subsection = genSection((Element) childNode);
				sectionListTmp.add(subsection);
			}
		}
		if (sectionListTmp.contains(Optional.empty())) {//section (<Group> or <ConnectorGroup>) より下でエラーがあった
			return Optional.empty();
		}

		ArrayList<Section> sectionList
			= sectionListTmp.stream()
				.map(section -> section.get()) //中身取り出し (Optional<Section> -> Section)
				.collect(Collectors.toCollection(ArrayList<Section>::new));	//ArrayList への変換
		return Optional.of(sectionList);
	}

	/**
	 * \<Section\> タグから Subsectionオブジェクトを作成する
	 * @param section \<Section\> タグを表すElement オブジェクト
	 * @return \<Section\> タグ の内容を反映した Subsection オブジェクト
	 */
	private Optional<Subsection> genSection(Element section) {

		String groupName = section.getAttribute(BhParams.BhModelDef.ATTR_NAME_NAME);
		Optional<ArrayList<Section>> childSectionListOpt = genSectionList(section);	//このSubsection オブジェクトが持つセクションリスト
		return childSectionListOpt.map(sectionList -> new Subsection(groupName, sectionList));
	}

	/**
	 * \<ConnectorSection\> タグからConnectorSectionオブジェクトを作成する
	 * @param connectorSection \<ConnectorSection\> タグを表すElement オブジェクト
	 * @return \<ConnectorSection\> タグ の内容を反映したConnectorSection オブジェクト
	 */
	private Optional<ConnectorSection> genConnectorSection(Element connectorSection) {

		String sectionName = connectorSection.getAttribute(BhParams.BhModelDef.ATTR_NAME_NAME);
		Collection<Element> connectorTags = BhNodeTemplates.getElementsByTagNameFromChild(connectorSection, BhParams.BhModelDef.ELEM_NAME_CONNECTOR);
		Collection<Element> privateCnctrTags = BhNodeTemplates.getElementsByTagNameFromChild(connectorSection, BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR);
		List<Connector> cnctrList = new ArrayList<>();
		List<ConnectorSection.CnctrInstantiationParams> cnctrInstantiationParamsList = new ArrayList<>();

		if (connectorTags.isEmpty() && privateCnctrTags.isEmpty()) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR_SECTION + ">" + " タグは最低一つ " +
				"<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + "> タグか" +
				"<" + BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグを" +
					"持たなければなりません.  " + connectorSection.getBaseURI());
			return Optional.empty();
		}

		for (Element connectorTag : connectorTags) {
			Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> cnctr_instParams = getConnector(connectorTag);
			if (!cnctr_instParams.isPresent()) {
				return Optional.empty();
			}
			cnctrList.add(cnctr_instParams.get()._1);
			cnctrInstantiationParamsList.add(cnctr_instParams.get()._2);
		}

		for (Element connectorTag : privateCnctrTags) {
			Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> cnctrAndInstParams = genPrivateConnector(connectorTag);
			if (!cnctrAndInstParams.isPresent()) {
				return Optional.empty();
			}
			cnctrList.add(cnctrAndInstParams.get()._1);
			cnctrInstantiationParamsList.add(cnctrAndInstParams.get()._2);
		}

		return Optional.of(new ConnectorSection(sectionName, cnctrList, cnctrInstantiationParamsList));
	}

	/**
	 * \<Connector\> タグからコネクタのテンプレートを取得する
	 * @param connector \<Connector\> タグを表すElement オブジェクト
	 * @return コネクタとそれのインスタンス化の際のパラメータのタプル
	 */
	private Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> getConnector(Element connectorTag) {

		ConnectorID connectorID = ConnectorID.createCnctrID(connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID));
		if (connectorID.equals(ConnectorID.NONE)) {
			MsgPrinter.INSTANCE.errMsgForDebug("<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + " タグには "
												   + BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID + " 属性を記述してください.  " + connectorTag.getBaseURI());
			return Optional.empty();
		}

		Optional<Connector> connectorOpt = getCnctrTemplate.apply(connectorID);
		if (!connectorOpt.isPresent())
			MsgPrinter.INSTANCE.errMsgForDebug(connectorID + " に対応するコネクタ定義が見つかりません.  " + connectorTag.getBaseURI());

		return connectorOpt.map(connector -> new Pair<>(connector, genConnectorInstParams(connectorTag)));
	}

	/**
	 * \<PrivateConnector\> タグからコネクタのテンプレートを取得する <br>
	 * プライベートコネクタの下にBhNodeがある場合, そのテンプレートも作成する.
	 * @return プライベートコネクタとコネクタインスタンス化時のパラメータのペア
	 */
	private Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> genPrivateConnector(Element connectorTag) {

		List<Element> privateNodeTagList = BhNodeTemplates.getElementsByTagNameFromChild(connectorTag, BhParams.BhModelDef.ELEM_NAME_NODE);
		if (privateNodeTagList.size() >= 2) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + "タグの下に2つ以上" +
				" <" + BhParams.BhModelDef.ELEM_NAME_NODE +"> タグを定義できません.\n" + connectorTag.getBaseURI());
			return Optional.empty();
		}

		//プライベートノードがある
		if (privateNodeTagList.size() == 1) {
			Optional<? extends BhNode> privateNode = genPirvateNode(privateNodeTagList.get(0));
			if (privateNode.isPresent() &&
				!connectorTag.hasAttribute(BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID))
				connectorTag.setAttribute(
					BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID, privateNode.get().getID().toString());
		}

		Optional<Connector> privateCnctrOpt = new ConnectorConstructor().genTemplate(connectorTag);
		//コネクタテンプレートリストに登録
		privateCnctrOpt.ifPresent(
			privateCnctr -> registerCnctrTemplate.accept(privateCnctr.getID(), privateCnctr));
		return privateCnctrOpt.map(
			privateCnctr -> new Pair<>(privateCnctr, genConnectorInstParams(connectorTag)));
	}

	/**
	 * コネクタオブジェクトをインスタンス化する際のパラメータオブジェクトを作成する
	 * @param connectorTag \<Connector\> or \<PrivateConnector\> タグを表すElement オブジェクト
	 * @return コネクタオブジェクトをインスタンス化する際のパラメータ
	 */
	private ConnectorSection.CnctrInstantiationParams genConnectorInstParams(Element connectorTag) {

		String imitationID = connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_ID);
		String imitCnctPoint = connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMIT_CNCT_POS);
		String name = connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_NAME);
		return new ConnectorSection.CnctrInstantiationParams(
			name,
			ImitationID.create(imitationID),
			ImitationConnectionPos.createImitCnctPoint(imitCnctPoint));
	}

	/**
	 * プライベートノード (\<PrivateConnector\> タグの下に定義されたタグ)
	 * @param nodeTag \<Node\> タグを表すオブジェクト
	 * @return プライベートノードオブジェクト
	 */
	private Optional<? extends BhNode> genPirvateNode(Element nodeTag) {

		NodeConstructor constructor =
			new NodeConstructor(
				registerNodeTemplate,
				registerCnctrTemplate,
				registerOrgNodeIdAndImitNodeID,
				getCnctrTemplate);

		Optional<? extends BhNode> privateNodeOpt = constructor.genTemplate(nodeTag);
		if (!privateNodeOpt.isPresent())
			MsgPrinter.INSTANCE.errMsgForDebug(
				"プライベートノード(<" +  BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグの下に定義されたノード) エラー.\n"
				+ nodeTag.getBaseURI());

		privateNodeOpt.ifPresent(privateNode -> registerNodeTemplate.accept(privateNode.getID(), privateNode));
		return privateNodeOpt;
	}
}
