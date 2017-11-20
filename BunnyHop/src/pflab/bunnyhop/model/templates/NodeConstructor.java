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
package pflab.bunnyhop.model.templates;

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
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Pair;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.BhNodeID;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.model.VoidNode;
import pflab.bunnyhop.model.connective.ConnectiveNode;
import pflab.bunnyhop.model.connective.Connector;
import pflab.bunnyhop.model.connective.ConnectorID;
import pflab.bunnyhop.model.connective.ConnectorSection;
import pflab.bunnyhop.model.connective.Section;
import pflab.bunnyhop.model.connective.Subsection;
import pflab.bunnyhop.model.imitation.ImitationConnectionPos;
import pflab.bunnyhop.model.imitation.ImitationID;
import pflab.bunnyhop.view.BhNodeViewStyle;

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
			MsgPrinter.instance.ErrMsgForDebug("ノード定義のルート要素は " + BhParams.BhModelDef.ELEM_NAME_NODE + " で始めてください.  " + doc.getBaseURI());
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
		String type = nodeRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_TYPE);

		if (type.equals(BhParams.BhModelDef.ATTR_VALUE_CONNECTIVE)) { //<Node type="connective">
			templateNode = genConnectiveNode(nodeRoot);
		}
		else if (type.equals(BhParams.BhModelDef.ATTR_VALUE_VOID)) { //<Node type="void">
			templateNode = genVoidNode(nodeRoot);
		}
		else if (type.equals(BhParams.BhModelDef.ATTR_NAME_TEXT_FIELD)	|| //<Node type="textField">
				 type.equals(BhParams.BhModelDef.ATTR_NAME_COMBO_BOX) || //<Node type="comboBox">
				 type.equals(BhParams.BhModelDef.ATTR_NAME_LABEL)) {		//<Node type="label">
			templateNode = genTextNode(nodeRoot, type);
		} else {
			MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ATTR_NAME_TYPE + "=" + type + " はサポートされていません.\n" + nodeRoot.getBaseURI() + "\n");
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
		Map<ImitationID, BhNodeID> imitID_imitNodeID = new HashMap<>();
		List<Element> imitTagList = BhNodeTemplates.getElementsByTagNameFromChild(node, BhParams.BhModelDef.ELEM_NAME_IMITATION);

		for (Element imitTag : imitTagList) {

			ImitationID imitationID = ImitationID.createImitID(imitTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_ID));
			if (imitationID.equals(ImitationID.NONE)) {
				MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグには, "
					+ BhParams.BhModelDef.ATTR_NAME_IMITATION_ID + " 属性を記述してください. " + node.getBaseURI());
				success &= false;
				continue;
			}

			BhNodeID imitNodeID = BhNodeID.createBhNodeID(imitTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_NODE_ID));
			if (imitNodeID.equals(BhNodeID.NONE)) {
				MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグには, "
					+ BhParams.BhModelDef.ATTR_NAME_IMITATION_NODE_ID + " 属性を記述してください. " + node.getBaseURI());
				success &= false;
				continue;
			}
			imitID_imitNodeID.put(imitationID, imitNodeID);
			registerOrgNodeIdAndImitNodeID.accept(orgNodeID, imitNodeID);
		}

		if (canCreateImitManually && (imitID_imitNodeID.get(ImitationID.MANUAL) == null)) {
			MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ATTR_NAME_CAN_CREATE_IMIT_MANUALLY + " 属性が " + BhParams.BhModelDef.ATTR_VALUE_TRUE + " のとき "
				+ BhParams.BhModelDef.ATTR_NAME_IMITATION_ID + " 属性に "
				+ BhParams.BhModelDef.ATTR_VALUE_IMIT_ID_MANUAL + " を指定した"
				+ BhParams.BhModelDef.ELEM_NAME_IMITATION + " タグを作る必要があります. " + node.getBaseURI());
			success &= false;
		}
		if (!success) {
			return Optional.empty();
		}

		return Optional.of(imitID_imitNodeID);
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
			MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ATTR_NAME_TYPE + " が "
				+ BhParams.BhModelDef.ATTR_VALUE_CONNECTIVE + " の "
				+ BhParams.BhModelDef.ELEM_NAME_NODE + " タグは, "
				+ BhParams.BhModelDef.ELEM_NAME_SECTION + " または " + BhParams.BhModelDef.ELEM_NAME_CONNECTOR_SECTION + " 子タグを1つ持たなければなりません. " + node.getBaseURI());
			return Optional.empty();
		}

		//実行時スクリプトチェック
		boolean allScriptsFound = 
			BhNodeTemplates.allScriptsExist(
				node.getBaseURI(),
				nodeAttrs.get().onMovedFromChildToWS, 
				nodeAttrs.get().onMovedToChild);
		if (!allScriptsFound) {
			return Optional.empty();
		}
		
		BhNodeID orgNodeID = nodeAttrs.get().bhNodeID;
		Optional<Map<ImitationID, BhNodeID>> imitID_imitNodeID = genImitIDAndNodePair(node, orgNodeID, nodeAttrs.get().canCreateImitManually);
		if (!imitID_imitNodeID.isPresent()) {
			return Optional.empty();
		}

		return Optional.of(new ConnectiveNode(
			nodeAttrs.get().bhNodeID,
			nodeAttrs.get().name,
			childSection.get().get(0),
			nodeAttrs.get().imitScopeName,
			nodeAttrs.get().onMovedFromChildToWS,
			nodeAttrs.get().onMovedToChild,
			imitID_imitNodeID.get(),
			nodeAttrs.get().canCreateImitManually));
	}

	/**
	 * voidノード(何も繋がっていないことを表すノード)を構築する
	 * @param node \<Node\> タグを表すオブジェクト
	 * @return VoidNodeオブジェクト(Option)
	 */
	private Optional<VoidNode> genVoidNode(Element node) {

		Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
		if (!nodeAttrs.isPresent()) {
			return Optional.empty();
		}
		return Optional.of(new VoidNode(nodeAttrs.get().bhNodeID, nodeAttrs.get().name));
	}

	/**
	 * テキストフィールドを持つノードを構築する
	 *
	 * @param node \<Node\> タグを表すオブジェクト
	 * @param type テキストノードに関連するGUIの種類
	 * @return TextFieldオブジェクト (Option)
		 *
	 */
	private Optional<TextNode> genTextNode(Element node, String type) {

		Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
		if (!nodeAttrs.isPresent()) {
			return Optional.empty();
		}
		
		//実行時スクリプトチェック
		boolean allScriptsFound = BhNodeTemplates.allScriptsExist(node.getBaseURI(),
			nodeAttrs.get().onMovedFromChildToWS,
			nodeAttrs.get().onMovedToChild,
			nodeAttrs.get().onTextInput);
		if (!allScriptsFound) {
			return Optional.empty();
		}
		
		if (nodeAttrs.get().nodeInputControlFileName.isEmpty()) {
			MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.ATTR_NAME_TYPE + " 属性が " + type + " の "
				+ "<" + BhParams.BhModelDef.ELEM_NAME_NODE + "> タグは " 
				+ BhParams.BhModelDef.ATTR_NAME_NODE_INPUT_CONTROL + " 属性でGUI入力部品のfxmlファイルを指定しなければなりません.\n" 
				+ node.getBaseURI() + "\n");
			return Optional.empty();
		} 
		else {
			BhNodeViewStyle.nodeID_inputControlFileName.put(nodeAttrs.get().bhNodeID, nodeAttrs.get().nodeInputControlFileName);
		}

		Optional<Map<ImitationID, BhNodeID>> imitID_imitNodeID = genImitIDAndNodePair(node, nodeAttrs.get().bhNodeID, nodeAttrs.get().canCreateImitManually);
		if (!imitID_imitNodeID.isPresent()) {
			return Optional.empty();
		}			

		return Optional.of(new TextNode(
			nodeAttrs.get().bhNodeID,
			nodeAttrs.get().name,
			type,
			nodeAttrs.get().initString,
			nodeAttrs.get().imitScopeName,
			nodeAttrs.get().onTextInput,
			nodeAttrs.get().onMovedFromChildToWS,
			nodeAttrs.get().onMovedToChild,
			imitID_imitNodeID.get(),
			nodeAttrs.get().canCreateImitManually));
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
			MsgPrinter.instance.ErrMsgForDebug(
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
			Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> cnctr_instParams = genPrivateConnector(connectorTag);
			if (!cnctr_instParams.isPresent()) {
				return Optional.empty();
			}
			cnctrList.add(cnctr_instParams.get()._1);
			cnctrInstantiationParamsList.add(cnctr_instParams.get()._2);
		}		

		return Optional.of(
			new ConnectorSection(
				sectionName,
				cnctrList,
				cnctrInstantiationParamsList));
	}

	/**
	 * \<Connector\> タグからコネクタのテンプレートを取得する
	 * @param connector \<Connector\> タグを表すElement オブジェクト
	 * @return コネクタとそれのインスタンス化の際のパラメータのタプル
	 */
	private Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> getConnector(Element connectorTag) {
		
		ConnectorID connectorID = ConnectorID.createCnctrID(connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID));
		if (connectorID.equals(ConnectorID.NONE)) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + " タグには " 
												   + BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID + " 属性を記述してください.  " + connectorTag.getBaseURI());
			return Optional.empty();
		}
		
		Optional<Connector> connector = getCnctrTemplate.apply(connectorID);
		if (!connector.isPresent()) {
			MsgPrinter.instance.ErrMsgForDebug(connectorID + " に対応するコネクタ定義が見つかりません.  " + connectorTag.getBaseURI());
			return Optional.empty();
		}
		
		ConnectorSection.CnctrInstantiationParams cnctrInstParams = genConnectorInstParams(connectorTag);
		return Optional.of(new Pair<>(connector.get(), cnctrInstParams));
	}

	/**
	 * \<PrivateConnector\> タグからコネクタのテンプレートを取得する <br>
	 * プライベートコネクタの下にBhNodeがある場合, そのテンプレートも作成する.
	 * @return プライベートコネクタオブジェクト
	 */
	private Optional<Pair<Connector, ConnectorSection.CnctrInstantiationParams>> genPrivateConnector(Element connectorTag) {
	
		List<Element> privateNodeTagList = BhNodeTemplates.getElementsByTagNameFromChild(connectorTag, BhParams.BhModelDef.ELEM_NAME_NODE);
		if (privateNodeTagList.size() >= 2) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + "タグの下に2つ以上" 
											 +" <" + BhParams.BhModelDef.ELEM_NAME_NODE +"> タグを定義できません.\n" + connectorTag.getBaseURI());
			return Optional.empty();
		}
		
		ConnectorID cnctrID = ConnectorID.createCnctrID(connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID));
		if (!cnctrID.equals(ConnectorID.NONE)) {
			MsgPrinter.instance.ErrMsgForDebug("<" +  BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグには"
											 + BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID + " を付けられません.\n" + connectorTag.getBaseURI());
			return Optional.empty();
		}
		
		Optional<? extends BhNode> privateNodeOpt = Optional.empty();
		if (privateNodeTagList.size() == 1) {
			privateNodeOpt = genPirvateNode(privateNodeTagList.get(0));	//プライベートノード作成
			if (!privateNodeOpt.isPresent())	//プライベートノード作成失敗
				return Optional.empty();
		}
						
		// プライベートコネクタ作成
		ConnectorID privateCnctrID = ConnectorID.createCnctrID("private_" + Util.genSerialID());	//プライベートコネクタのIDはプログラム内部でつける
		connectorTag.setAttribute(BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID, privateCnctrID.toString());	//cnctrID登録
		privateNodeOpt.ifPresent(privateNode -> connectorTag.setAttribute(BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID, privateNode.getID().toString()));	//initNode登録
		ConnectorConstructor constructor = new ConnectorConstructor();
		Optional<Connector> privateCnctr = constructor.genTemplate(connectorTag);	
		if (!privateCnctr.isPresent()) {
			MsgPrinter.instance.ErrMsgForDebug("プライベートコネクタエラー.  " + connectorTag.getBaseURI());
			return Optional.empty();
		}
		privateCnctrID = privateCnctr.get().getID();
		registerCnctrTemplate.accept(privateCnctrID, privateCnctr.get()); //コネクタテンプレートリストに登録
		ConnectorSection.CnctrInstantiationParams cnctrInstParams = genConnectorInstParams(connectorTag);
		return Optional.of(new Pair<>(privateCnctr.get(), cnctrInstParams));
	}
	
	/**
	 * コネクタオブジェクトをインスタンス化する際のパラメータオブジェクトを作成する
	 * @param connectorTag \<Connector\> or \<PrivateConnector\> タグを表すElement オブジェクト
	 * @return コネクタオブジェクトをインスタンス化する際のパラメータ
	 */
	private ConnectorSection.CnctrInstantiationParams genConnectorInstParams(Element connectorTag) {
		
		String imitationID = connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMITATION_ID);
		String imitCnctPoint = connectorTag.getAttribute(BhParams.BhModelDef.ARRT_NAME_IMIT_CNCT_POS);
		String name = connectorTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_NAME);
		return new ConnectorSection.CnctrInstantiationParams(
			name, 
			ImitationID.createImitID(imitationID),
			ImitationConnectionPos.createImitCnctPoint(imitCnctPoint));		
	}
	
	/**
	 * プライベートノード (\<PrivateConnector\> タグの下に定義されたタグ)
	 * @param nodeTag \<Node\> タグを表すオブジェクト
	 * @return プライベートノードオブジェクト
	 */
	private Optional<? extends BhNode> genPirvateNode(Element nodeTag) {
		
		String nodeID = nodeTag.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHNODE_ID);
		if (!nodeID.isEmpty()) {
			MsgPrinter.instance.ErrMsgForDebug("プライベートノード (<" +  BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグの下に定義されたノード) "
											 + "には, " + BhParams.BhModelDef.ATTR_NAME_BHNODE_ID + " を付けられません.\n" + nodeTag.getBaseURI());
			return Optional.empty();
		}
	
		String bhNodeID = "private_" + Util.genSerialID();
		nodeTag.setAttribute(BhParams.BhModelDef.ATTR_NAME_BHNODE_ID, bhNodeID);	//プライベートノードのIDは内部で付ける
		NodeConstructor constructor = 
			new NodeConstructor(
				registerNodeTemplate, 
				registerCnctrTemplate,
				registerOrgNodeIdAndImitNodeID,
				getCnctrTemplate);
		
		Optional<? extends BhNode> privateNode = constructor.genTemplate(nodeTag);	
		if (!privateNode.isPresent()) {
			MsgPrinter.instance.ErrMsgForDebug("プライベートノード(<" +  BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグの下に定義されたノード) エラー.\n" 
												+ nodeTag.getBaseURI());
			return Optional.empty();
		}
		registerNodeTemplate.accept(privateNode.get().getID(), privateNode.get());
		return privateNode;
	}
}
