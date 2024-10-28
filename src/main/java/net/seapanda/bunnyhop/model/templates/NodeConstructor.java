/*
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
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeType;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorId;
import net.seapanda.bunnyhop.model.node.connective.ConnectorInstantiationParams;
import net.seapanda.bunnyhop.model.node.connective.ConnectorSection;
import net.seapanda.bunnyhop.model.node.connective.Section;
import net.seapanda.bunnyhop.model.node.connective.Subsection;
import net.seapanda.bunnyhop.model.node.imitation.ImitCnctPosId;
import net.seapanda.bunnyhop.model.node.imitation.ImitationId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link BhNode } が定義された xml の Node タグ以下の情報から {@link BhNode} を作成する.
 *
 * @author K.Koike
 */
public class NodeConstructor {

  /** ノードテンプレート登録用関数. */
  private final BiConsumer<BhNodeId, BhNode> registerNodeTemplate;
  /** コネクタテンプレート登録用関数. */
  private final BiConsumer<ConnectorId, Connector> registerCnctrTemplate;
  /** オリジナル & イミテーションノード格納用関数. */
  private final BiConsumer<BhNodeId, BhNodeId> registerOrgNodeIdAndImitNodeId;
  /** コネクタテンプレート取得用関数. */
  private final Function<ConnectorId, Optional<Connector>> getCnctrTemplate;

  /** コンストラクタ. */
  public NodeConstructor(
      BiConsumer<BhNodeId, BhNode> registerNodeTemplate,
      BiConsumer<ConnectorId, Connector> registerCnctrTemplate,
      BiConsumer<BhNodeId, BhNodeId> registerOrgNodeIdAndImitNodeId,
      Function<ConnectorId, Optional<Connector>> getCnctrTemplate) {
    this.registerNodeTemplate = registerNodeTemplate;
    this.registerCnctrTemplate = registerCnctrTemplate;
    this.registerOrgNodeIdAndImitNodeId = registerOrgNodeIdAndImitNodeId;
    this.getCnctrTemplate = getCnctrTemplate;
  }

  /**
   * ノードテンプレートを作成する.
   *
   * @param doc ノードテンプレートを作成する xml の Document オブジェクト
   * @return 作成した {@link BhNode} オブジェクト
   */
  public Optional<? extends BhNode> genTemplate(Document doc) {
    if (!doc.getFirstChild().getNodeName().equals(BhConstants.BhModelDef.ELEM_NODE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "ノード定義のルート要素は " + BhConstants.BhModelDef.ELEM_NODE + " で始めてください.  "
          + doc.getBaseURI());
      return Optional.empty();
    }
    return genTemplate(doc.getDocumentElement());
  }

  /**
   * ノードテンプレートを作成する.
   *
   * @param nodeRoot Node タグを表す要素
   * @return 作成した {@link BhNode} オブジェクト
   */
  public Optional<? extends BhNode> genTemplate(Element nodeRoot) {
    Optional<? extends BhNode> templateNode = Optional.empty();
    String typeName = nodeRoot.getAttribute(BhConstants.BhModelDef.ATTR_TYPE);
    BhNodeType type = BhNodeType.toType(typeName);
    switch (type) {
      //<Node type="connective">
      case CONNECTIVE:
        templateNode = genConnectiveNode(nodeRoot);
        break;

      //<Node type="text">
      case TEXT:
        templateNode = genTextNode(nodeRoot);
        break;

      default:
        MsgPrinter.INSTANCE.errMsgForDebug(
            BhConstants.BhModelDef.ATTR_TYPE + "=" + type + " はサポートされていません.\n" 
            + nodeRoot.getBaseURI() + "\n");
        break;
    }
    return templateNode;
  }

  /**
   * Imitation タグの情報を取得する.
   *
   * @param node イミテーションノードに関する情報が書いてあるxmlタグをあらわすノード
   * @param orgNodeId イミテーションを持つノードのID
   * @return イミテーション ID とイミテーションノード ID のマップ
   */
  private Optional<Map<ImitationId, BhNodeId>> genImitIdAndNodePair(
        Element node, BhNodeId orgNodeId) {

    boolean success = true;
    Map<ImitationId, BhNodeId> imitIdToImitNodeId = new HashMap<>();
    List<Element> imitTagList =
        BhNodeTemplates.getElementsByTagNameFromChild(node, BhConstants.BhModelDef.ELEM_IMITATION);
    for (Element imitTag : imitTagList) {
      ImitationId imitationId =
          ImitationId.create(imitTag.getAttribute(BhConstants.BhModelDef.ATTR_IMITATION_ID));
      if (imitationId.equals(ImitationId.NONE)) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            BhConstants.BhModelDef.ELEM_IMITATION + " タグには, "
            + BhConstants.BhModelDef.ATTR_IMITATION_ID + " 属性を記述してください. " + node.getBaseURI());
        success &= false;
        continue;
      }

      BhNodeId imitNodeId =
          BhNodeId.create(imitTag.getAttribute(BhConstants.BhModelDef.ATTR_IMITATION_NODE_ID));
      if (imitNodeId.equals(BhNodeId.NONE)) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            BhConstants.BhModelDef.ELEM_IMITATION + " タグには, "
            + BhConstants.BhModelDef.ATTR_IMITATION_NODE_ID + " 属性を記述してください. " + node.getBaseURI());
        success &= false;
        continue;
      }
      imitIdToImitNodeId.put(imitationId, imitNodeId);
      registerOrgNodeIdAndImitNodeId.accept(orgNodeId, imitNodeId);
    }

    if (!success) {
      return Optional.empty();
    }
    return Optional.of(imitIdToImitNodeId);
  }

  /**
   * {@link ConnectiveNode} を構築する.
   *
   * @param node Node タグを表すオブジェクト
   * @return {@link ConnectiveNode} オブジェクト
   */
  private Optional<ConnectiveNode> genConnectiveNode(Element node) {
    Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
    if (nodeAttrs.isEmpty()) {
      return Optional.empty();
    }

    Optional<ArrayList<Section>> childSection = genSectionList(node);
    if (childSection.isEmpty()) {
      return Optional.empty();
    }

    if (childSection.get().size() != 1) {
      MsgPrinter.INSTANCE.errMsgForDebug(BhConstants.BhModelDef.ATTR_TYPE + " が "
          + BhConstants.BhModelDef.ATTR_VAL_CONNECTIVE + " の "
          + BhConstants.BhModelDef.ELEM_NODE + " タグは, "
          + BhConstants.BhModelDef.ELEM_SECTION + " または "
          + BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION + " 子タグを1つ持たなければなりません. "
          + node.getBaseURI());
      return Optional.empty();
    }

    //実行時スクリプト存在チェック
    boolean allScriptsFound = BhNodeTemplates.allScriptsExist(
        node.getBaseURI(),
        nodeAttrs.get().getOnMovedFromChildToWs(),
        nodeAttrs.get().getOnMovedToChild(),
        nodeAttrs.get().getOnChildReplaced(),
        nodeAttrs.get().getOnDeletionRequested(),
        nodeAttrs.get().getOnCutRequested(),
        nodeAttrs.get().getOnCopyRequested(),
        nodeAttrs.get().getOnSyntaxChecking(),
        nodeAttrs.get().getOnPrivateTemplateCreating());
    if (!allScriptsFound) {
      return Optional.empty();
    }
    BhNodeId orgNodeId = nodeAttrs.get().getBhNodeId();
    Optional<Map<ImitationId, BhNodeId>> imitIdToImitNodeId = genImitIdAndNodePair(node, orgNodeId);
    if (imitIdToImitNodeId.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new ConnectiveNode(childSection.get().get(0), imitIdToImitNodeId.get(), nodeAttrs.get()));
  }

  /**
   * {@link TextNode} を構築する.
   *
   * @param node Node タグを表すオブジェクト
   * @param type 関連する BhNodeView の種類
   * @param checkViewComponent GUI部品の有無をチェックする場合true
   * @return {@link TextNode} オブジェクト
   */
  private Optional<TextNode> genTextNode(Element node) {
    Optional<BhNodeAttributes> nodeAttrs = BhNodeAttributes.readBhNodeAttriButes(node);
    if (nodeAttrs.isEmpty()) {
      return Optional.empty();
    }

    //実行時スクリプト存在チェック
    boolean allScriptsFound = BhNodeTemplates.allScriptsExist(
        node.getBaseURI(),
        nodeAttrs.get().getOnMovedFromChildToWs(),
        nodeAttrs.get().getOnMovedToChild(),
        nodeAttrs.get().getOnTextChecking(),
        nodeAttrs.get().getOnDeletionRequested(),
        nodeAttrs.get().getOnCutRequested(),
        nodeAttrs.get().getOnCopyRequested(),
        nodeAttrs.get().getOnTextFormatting(),
        nodeAttrs.get().getOnSyntaxChecking(),
        nodeAttrs.get().getOnPrivateTemplateCreating(),
        nodeAttrs.get().getOnTextOptionsCreating());
    if (!allScriptsFound) {
      return Optional.empty();
    }

    Optional<Map<ImitationId, BhNodeId>> imitIdToImitNodeId =
        genImitIdAndNodePair(node, nodeAttrs.get().getBhNodeId());
    if (imitIdToImitNodeId.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new TextNode(imitIdToImitNodeId.get(), nodeAttrs.get()));
  }

  /**
   * {@code parentTag} で指定したタグより下の {@link Section} リストを作成する.
   *
   * @param parentTag Section タグ or ConnectorSection タグを子に持つタグ
   * @return parentTag より下の {@link Section} リスト
   */
  private Optional<ArrayList<Section>> genSectionList(Element parentTag) {
    if (parentTag == null) {
      return Optional.of(new ArrayList<>());
    }
    ArrayList<Optional<? extends Section>> sectionListTmp = new ArrayList<>();
    NodeList sections = parentTag.getChildNodes();
    for (int i = 0; i < sections.getLength(); ++i) {
      Node childNode = sections.item(i);
      // 子タグ以外処理しない
      if (childNode.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      // parentTag の子ノードの名前が ConnectorSection/
      if (childNode.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION)) {
        Optional<ConnectorSection> connectorGroup = genConnectorSection((Element) childNode);
        sectionListTmp.add(connectorGroup);

      // parentTag の子ノードの名前が Section
      } else if (childNode.getNodeName().equals(BhConstants.BhModelDef.ELEM_SECTION)) {
        Optional<Subsection> subsection = genSection((Element) childNode);
        sectionListTmp.add(subsection);
      }
    }
    // section (<Group> or <ConnectorGroup>) より下でエラーがあった
    if (sectionListTmp.contains(Optional.empty())) {
      return Optional.empty();
    }
    ArrayList<Section> sectionList = sectionListTmp.stream()
        .map(section -> section.get())
        .collect(Collectors.toCollection(ArrayList<Section>::new));
    return Optional.of(sectionList);
  }

  /**
   * Section タグから {@link Subsection} オブジェクトを作成する.
   *
   * @param section Section タグを表す Element オブジェクト
   * @return Section タグ の内容を反映した {@link Subsection} オブジェクト
   */
  private Optional<Subsection> genSection(Element section) {
    String groupName = section.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    return genSectionList(section).map(sectionList -> new Subsection(groupName, sectionList));
  }

  /**
   * ConnectorSection タグから {@link ConnectorSection} オブジェクトを作成する.
   *
   * @param connectorSection ConnectorSection タグを表すElement オブジェクト
   * @return ConnectorSection タグ の内容を反映した {@link ConnectorSection} オブジェクト
   */
  private Optional<ConnectorSection> genConnectorSection(Element connectorSection) {
    final String sectionName = connectorSection.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    Collection<Element> connectorTags = BhNodeTemplates.getElementsByTagNameFromChild(
        connectorSection, BhConstants.BhModelDef.ELEM_CONNECTOR);
    Collection<Element> privateCnctrTags = BhNodeTemplates.getElementsByTagNameFromChild(
        connectorSection, BhConstants.BhModelDef.ELEM_PRIVATE_CONNECTOR);
    List<Connector> cnctrList = new ArrayList<>();
    List<ConnectorInstantiationParams> cnctrInstantiationParamsList =
        new ArrayList<>();

    if (connectorTags.isEmpty() && privateCnctrTags.isEmpty()) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION + ">" + " タグは最低一つ "
          + "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + "> タグか"
          + "<" + BhConstants.BhModelDef.ELEM_PRIVATE_CONNECTOR + "> タグを"
          + "持たなければなりません.  " + connectorSection.getBaseURI());
      return Optional.empty();
    }

    for (Element connectorTag : connectorTags) {
      Optional<Pair<Connector, ConnectorInstantiationParams>> cnctrAndParams =
          getConnector(connectorTag);
      if (cnctrAndParams.isEmpty()) {
        return Optional.empty();
      }
      cnctrList.add(cnctrAndParams.get().v1);
      cnctrInstantiationParamsList.add(cnctrAndParams.get().v2);
    }

    for (Element connectorTag : privateCnctrTags) {
      Optional<Pair<Connector, ConnectorInstantiationParams>> cnctrAndParams =
          genPrivateConnector(connectorTag);
      if (cnctrAndParams.isEmpty()) {
        return Optional.empty();
      }
      cnctrList.add(cnctrAndParams.get().v1);
      cnctrInstantiationParamsList.add(cnctrAndParams.get().v2);
    }

    return Optional.of(new ConnectorSection(sectionName, cnctrList, cnctrInstantiationParamsList));
  }

  /**
   * Connector タグからコネクタのテンプレートを取得する.
   *
   * @param connector Connector タグを表す Element オブジェクト
   * @return コネクタとそれのインスタンス化の際のパラメータのタプル
   */
  private Optional<Pair<Connector, ConnectorInstantiationParams>> getConnector(
      Element connectorTag) {
    ConnectorId connectorId = ConnectorId.createCnctrId(
        connectorTag.getAttribute(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID));
    if (connectorId.equals(ConnectorId.NONE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + ">" + " タグには "
          + BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID + " 属性を記述してください.  "
          + connectorTag.getBaseURI());
      return Optional.empty();
    }

    Optional<Connector> connectorOpt = getCnctrTemplate.apply(connectorId);
    if (connectorOpt.isEmpty()) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          connectorId + " に対応するコネクタ定義が見つかりません.  " + connectorTag.getBaseURI());
    }
    return connectorOpt.map(
        connector -> new Pair<>(connector, genConnectorInstParams(connectorTag)));
  }

  /**
   * PrivateConnector タグからコネクタのテンプレートを取得する.
   * プライベートコネクタの下にBhNodeがある場合, そのテンプレートも作成する.
   *
   * @return プライベートコネクタとコネクタインスタンス化時のパラメータのペア
   */
  private Optional<Pair<Connector, ConnectorInstantiationParams>>
      genPrivateConnector(Element connectorTag) {

    List<Element> privateNodeTagList = BhNodeTemplates.getElementsByTagNameFromChild(
        connectorTag, BhConstants.BhModelDef.ELEM_NODE);
    if (privateNodeTagList.size() >= 2) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + ">" + "タグの下に2つ以上"
          + " <" + BhConstants.BhModelDef.ELEM_NODE + "> タグを定義できません.\n"
          + connectorTag.getBaseURI());
      return Optional.empty();
    }

    //プライベートノードがある
    if (privateNodeTagList.size() == 1) {
      Optional<? extends BhNode> privateNode = genPirvateNode(privateNodeTagList.get(0));
      if (privateNode.isPresent()
          && !connectorTag.hasAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID)) {
        connectorTag.setAttribute(
            BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
            privateNode.get().getId().toString());
      }
    }

    Optional<Connector> privateCnctr = new ConnectorConstructor().genTemplate(connectorTag);
    //コネクタテンプレートリストに登録
    privateCnctr.ifPresent(cnctr -> registerCnctrTemplate.accept(cnctr.getId(), cnctr));
    return privateCnctr.map(
        cnctr -> new Pair<>(cnctr, genConnectorInstParams(connectorTag)));
  }

  /**
   * コネクタオブジェクトをインスタンス化する際のパラメータオブジェクトを作成する.
   *
   * @param connectorTag Connector or PrivateConnector タグを表す Element オブジェクト
   * @return コネクタオブジェクトをインスタンス化する際のパラメータ
   */
  private ConnectorInstantiationParams genConnectorInstParams(Element connectorTag) {
    String imitationId =
        connectorTag.getAttribute(BhConstants.BhModelDef.ATTR_IMITATION_ID);
    String imitCnctPoint = connectorTag.getAttribute(BhConstants.BhModelDef.ATTR_IMIT_CNCT_POS);
    String name = connectorTag.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    String fixedStr = connectorTag.getAttribute(BhConstants.BhModelDef.ATTR_FIXED);
    Boolean fixed =
        fixedStr.isEmpty() ? null : fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE);
    return new ConnectorInstantiationParams(
      name,
      fixed,
      ImitationId.create(imitationId),
      ImitCnctPosId.create(imitCnctPoint));
  }

  /**
   * プライベートノードの定義 (= PrivateConnector タグの下に定義されたノード) から {@link BhNode} を作成する.
   *
   * @param nodeTag Node タグを表すオブジェクト
   * @return プライベートノードオブジェクト
   */
  private Optional<? extends BhNode> genPirvateNode(Element nodeTag) {
    NodeConstructor constructor = new NodeConstructor(
        registerNodeTemplate,
        registerCnctrTemplate,
        registerOrgNodeIdAndImitNodeId,
        getCnctrTemplate);

    Optional<? extends BhNode> privateNodeOpt = constructor.genTemplate(nodeTag);
    if (privateNodeOpt.isEmpty()) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "プライベートノード(<" +  BhConstants.BhModelDef.ELEM_PRIVATE_CONNECTOR 
          + "> タグの下に定義されたノード) エラー.\n"
          + nodeTag.getBaseURI());
    }
    privateNodeOpt.ifPresent(
        privateNode -> registerNodeTemplate.accept(privateNode.getId(), privateNode));
    return privateNodeOpt;
  }
}
