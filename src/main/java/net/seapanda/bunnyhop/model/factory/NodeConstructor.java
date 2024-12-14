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

package net.seapanda.bunnyhop.model.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeType;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorAttributes;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorParamSetId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.section.ConnectorSection;
import net.seapanda.bunnyhop.model.node.section.Section;
import net.seapanda.bunnyhop.model.node.section.Subsection;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link BhNode } が定義された xml の Node エレメント以下の情報から {@link BhNode} を作成する.
 *
 * @author K.Koike
 */
public class NodeConstructor {

  /** ノードテンプレート登録用関数. */
  private final BiFunction<BhNodeId, BhNode, Boolean> fnRegisterNodeTemplate;
  /** コネクタテンプレート登録用関数. */
  private final BiFunction<ConnectorId, Connector, Boolean> fnRegisterCnctrTemplate;
  /** オリジナルと派生ノードの ID を登録するための関数. */
  private final BiConsumer<BhNodeId, BhNodeId> fnRegisterOrgAndDervNodeId;
  /** コネクタテンプレート取得用関数. */
  private final Function<ConnectorId, Optional<Connector>> fnGetCnctrTemplate;
  /** コネクタアトリビュート取得用関数. */
  private final Function<ConnectorParamSetId, Optional<ConnectorAttributes>> fnGetCnctrAttr;
  /** {@link BhNode} の定義ファイルに書かれた JavaScript ファイルを保持する {@link BhScriptManager} オブジェクト. */
  private final BhScriptManager scriptManager;

  /**
   * コンストラクタ.
   *
   * @param fnRegisterNodeTemplate ノードテンプレート登録用関数
   * @param fnRegisterCnctrTemplate コネクタテンプレート登録用関数.
   * @param fnRegisterOrgAndDervId オリジナルと派生ノードの ID を登録するための関数.
   * @param fnGetCnctrTemplate コネクタテンプレート取得用関数
   * @param fnGetCnctrAttr コネクタアトリビュート取得用関数
   * @param scriptManager {@link BhNode} の定義ファイルに書かれた
   *                      JavaScript ファイルを保持する {@link BhScriptManager} オブジェクト
   */
  public NodeConstructor(
      BiFunction<BhNodeId, BhNode, Boolean> fnRegisterNodeTemplate,
      BiFunction<ConnectorId, Connector, Boolean> fnRegisterCnctrTemplate,
      BiConsumer<BhNodeId, BhNodeId> fnRegisterOrgAndDervId,
      Function<ConnectorId, Optional<Connector>> fnGetCnctrTemplate,
      Function<ConnectorParamSetId, Optional<ConnectorAttributes>> fnGetCnctrAttr,
      BhScriptManager scriptManager) {
    this.fnRegisterNodeTemplate = fnRegisterNodeTemplate;
    this.fnRegisterCnctrTemplate = fnRegisterCnctrTemplate;
    this.fnRegisterOrgAndDervNodeId = fnRegisterOrgAndDervId;
    this.fnGetCnctrTemplate = fnGetCnctrTemplate;
    this.fnGetCnctrAttr = fnGetCnctrAttr;
    this.scriptManager = scriptManager;
  }

  /**
   * ノードテンプレートを作成する.
   *
   * @param doc ノードテンプレートを作成する xml の Document オブジェクト
   * @return 作成した {@link BhNode} オブジェクト
   */
  public Optional<? extends BhNode> genTemplate(Document doc) {
    if (!doc.getFirstChild().getNodeName().equals(BhConstants.BhModelDef.ELEM_NODE)) {
      BhService.msgPrinter().errForDebug(String.format("""
          Invalid BhNode definition. (%s)
          A BhNode definition must have a '%s' root element.\n%s
          """,
          doc.getFirstChild().getNodeName(),
          BhConstants.BhModelDef.ELEM_NODE,
          doc.getBaseURI()));
      return Optional.empty();
    }
    return genTemplate(doc.getDocumentElement());
  }

  /**
   * ノードテンプレートを作成する.
   *
   * @param nodeRoot Node エレメントを表す要素
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
        BhService.msgPrinter().errForDebug(
            "Unknown BhNode type.  (%s)\n%s".formatted(type, nodeRoot.getBaseURI()));
        break;
    }
    return templateNode;
  }

  /**
   * Derivation エレメントの情報を取得する.
   *
   * @param node 派生先情報が書いてあるエレメントを表すノード
   * @param orgNodeId 派生ノードを持つノードのID
   * @return 派生先 ID と派生ノード ID のマップ
   */
  private Optional<Map<DerivationId, BhNodeId>> genDerivationAndDerivative(
      Element node, BhNodeId orgNodeId) {

    boolean success = true;
    Map<DerivationId, BhNodeId> derivationToDerivative = new HashMap<>();
    List<Element> derivationElems =
        BhNodeFactory.getElementsByTagNameFromChild(node, BhConstants.BhModelDef.ELEM_DERIVATION);
    for (Element derivationElem : derivationElems) {
      DerivationId derivationId =
          DerivationId.of(derivationElem.getAttribute(BhConstants.BhModelDef.ATTR_DETIVATION_ID));
      if (derivationId.equals(DerivationId.NONE)) {
        BhService.msgPrinter().errForDebug(String.format(
            "A '%s' element must have a '%s' attribute.\n%s",
            BhConstants.BhModelDef.ELEM_DERIVATION,
            BhConstants.BhModelDef.ATTR_DETIVATION_ID,
            node.getBaseURI()));
        success &= false;
        continue;
      }

      BhNodeId derivativeId =
          BhNodeId.of(derivationElem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_ID));
      if (derivativeId.equals(BhNodeId.NONE)) {
        BhService.msgPrinter().errForDebug(String.format(
            "A '%s' element must have a '%s' attribute.\n%s",
            BhConstants.BhModelDef.ELEM_DERIVATION,
            BhConstants.BhModelDef.ATTR_DERIVATIVE_ID,
            node.getBaseURI()));
        success &= false;
        continue;
      }
      derivationToDerivative.put(derivationId, derivativeId);
      fnRegisterOrgAndDervNodeId.accept(orgNodeId, derivativeId);
    }

    if (!success) {
      return Optional.empty();
    }
    return Optional.of(derivationToDerivative);
  }

  /**
   * {@link ConnectiveNode} を構築する.
   *
   * @param node Node エレメントを表すオブジェクト
   * @return {@link ConnectiveNode} オブジェクト
   */
  private Optional<ConnectiveNode> genConnectiveNode(Element node) {
    BhNodeAttributes nodeAttrs = BhNodeAttributes.of(node);

    if (nodeAttrs.bhNodeId().equals(BhNodeId.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_BH_NODE_ID,
          node.getBaseURI()));
      return Optional.empty();
    }

    if (nodeAttrs.version().equals(BhNodeVersion.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_VERSION,
          node.getBaseURI()));
      return Optional.empty();
    }

    if (!nodeAttrs.nodeStyleId().isEmpty()) {
      BhNodeViewStyle.registerNodeIdAndStyleId(nodeAttrs.bhNodeId(), nodeAttrs.nodeStyleId());
    }

    Optional<ArrayList<Section>> childSection = genSectionList(node);
    if (childSection.isEmpty()) {
      return Optional.empty();
    }

    if (childSection.get().size() != 1) {
      BhService.msgPrinter().errForDebug(String.format(
          ("A '%s' element whose '%s' is '%s' must have a child '%s' element "
          + "or a child '%s' element.\n%s"),
          BhConstants.BhModelDef.ELEM_SECTION,
          BhConstants.BhModelDef.ATTR_TYPE,
          BhConstants.BhModelDef.ATTR_VAL_CONNECTIVE,
          BhConstants.BhModelDef.ELEM_SECTION,
          BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION,
          node.getBaseURI()));
      return Optional.empty();
    }

    //実行時スクリプト存在チェック
    boolean allScriptsFound = scriptManager.allExistIgnoringEmpty(
        node.getBaseURI(),
        nodeAttrs.onMovedFromChildToWs(),
        nodeAttrs.onMovedToChild(),
        nodeAttrs.onChildReplaced(),
        nodeAttrs.onDeletionRequested(),
        nodeAttrs.onCutRequested(),
        nodeAttrs.onCopyRequested(),
        nodeAttrs.onCompileErrorChecking(),
        nodeAttrs.onPrivateTemplateCreating(),
        nodeAttrs.onTemplateCreated(),
        nodeAttrs.onDragStarted());
    if (!allScriptsFound) {
      return Optional.empty();
    }
    Optional<Map<DerivationId, BhNodeId>> derivationToDerivative =
        genDerivationAndDerivative(node, nodeAttrs.bhNodeId());
    if (derivationToDerivative.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new ConnectiveNode(childSection.get().get(0), derivationToDerivative.get(), nodeAttrs));
  }

  /**
   * {@link TextNode} を構築する.
   *
   * @param node Node エレメントを表すオブジェクト
   * @param type 関連する BhNodeView の種類
   * @param checkViewComponent GUI部品の有無をチェックする場合true
   * @return {@link TextNode} オブジェクト
   */
  private Optional<TextNode> genTextNode(Element node) {
    BhNodeAttributes nodeAttrs = BhNodeAttributes.of(node);

    if (nodeAttrs.bhNodeId().equals(BhNodeId.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_BH_NODE_ID,
          node.getBaseURI()));
      return Optional.empty();
    }

    if (nodeAttrs.version().equals(BhNodeVersion.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_VERSION,
          node.getBaseURI()));
      return Optional.empty();
    }

    if (!nodeAttrs.nodeStyleId().isEmpty()) {
      BhNodeViewStyle.registerNodeIdAndStyleId(nodeAttrs.bhNodeId(), nodeAttrs.nodeStyleId());
    }
    //実行時スクリプト存在チェック
    boolean allScriptsFound = scriptManager.allExistIgnoringEmpty(
        node.getBaseURI(),
        nodeAttrs.onMovedFromChildToWs(),
        nodeAttrs.onMovedToChild(),
        nodeAttrs.onTextChecking(),
        nodeAttrs.onTextFormatting(),
        nodeAttrs.onDeletionRequested(),
        nodeAttrs.onCutRequested(),
        nodeAttrs.onCopyRequested(),
        nodeAttrs.onCompileErrorChecking(),
        nodeAttrs.onPrivateTemplateCreating(),
        nodeAttrs.onTextOptionsCreating(),
        nodeAttrs.onTemplateCreated(),
        nodeAttrs.onDragStarted());
    if (!allScriptsFound) {
      return Optional.empty();
    }

    Optional<Map<DerivationId, BhNodeId>> derivationToDerivative =
        genDerivationAndDerivative(node, nodeAttrs.bhNodeId());
    if (derivationToDerivative.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new TextNode(derivationToDerivative.get(), nodeAttrs));
  }

  /**
   * {@code parentTag} で指定したエレメントより下の {@link Section} リストを作成する.
   *
   * @param parentTag Section エレメント or ConnectorSection エレメントを子に持つエレメント
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
      // 子エレメント以外処理しない
      if (childNode.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      // parentTag の子ノードの名前が ConnectorSection
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
   * Section エレメントから {@link Subsection} オブジェクトを作成する.
   *
   * @param section Section エレメントを表す Element オブジェクト
   * @return Section エレメント の内容を反映した {@link Subsection} オブジェクト
   */
  private Optional<Subsection> genSection(Element section) {
    String groupName = section.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    return genSectionList(section).map(sectionList -> new Subsection(groupName, sectionList));
  }

  /**
   * ConnectorSection エレメントから {@link ConnectorSection} オブジェクトを作成する.
   *
   * @param connectorSection ConnectorSection エレメントを表す Element オブジェクト
   * @return ConnectorSection エレメント の内容を反映した {@link ConnectorSection} オブジェクト
   */
  private Optional<ConnectorSection> genConnectorSection(Element connectorSection) {
    final String sectionName = connectorSection.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    Collection<Element> connectorTags = BhNodeFactory.getElementsByTagNameFromChild(
        connectorSection, BhConstants.BhModelDef.ELEM_CONNECTOR);
    List<Connector> cnctrList = new ArrayList<>();
  
    if (connectorTags.isEmpty()) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element must have at least one child '%s' element.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION,
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          connectorSection.getBaseURI()));
      return Optional.empty();
    }

    for (Element connectorTag : connectorTags) {
      Optional<Connector> connector = genConnector(connectorTag);
      if (connector.isEmpty()) {
        return Optional.empty();
      }
      cnctrList.add(connector.get());
    }
    return Optional.of(new ConnectorSection(sectionName, cnctrList));
  }

  /**
   * Connector エレメントからコネクタのテンプレートを取得する.
   * プライベートコネクタの下に BhNode がある場合, そのテンプレートも作成する.
   *
   * @return プライベートコネクタとコネクタインスタンス化時のパラメータのペア
   */
  private Optional<Connector> genConnector(Element cnctrElem) {
    List<Element> privateNodeTagList = BhNodeFactory.getElementsByTagNameFromChild(
        cnctrElem, BhConstants.BhModelDef.ELEM_NODE);
    if (privateNodeTagList.size() >= 2) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' element cannot have more than two child '%s' elements.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ELEM_NODE,
          cnctrElem.getBaseURI()));
      return Optional.empty();
    }
    // プライベートノードがある
    if (privateNodeTagList.size() == 1) {
      Optional<? extends BhNode> privateNode = genPirvateNode(privateNodeTagList.get(0));
      if (privateNode.isPresent()
          && !cnctrElem.hasAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID)) {
        cnctrElem.setAttribute(
            BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
            privateNode.get().getId().toString());
      }
    }
    // コネクタ作成
    Optional<Connector> connector =
        new ConnectorConstructor(cnctrElem, fnGetCnctrAttr, scriptManager).genConnector();
    boolean success =
        connector.map(cnctr -> fnRegisterCnctrTemplate.apply(cnctr.getId(), cnctr)).orElse(false);
    
    return success ? connector : Optional.empty();
  }

  /**
   * プライベートノードの定義 (= Connector エレメントの下に定義されたノード) から {@link BhNode} を作成する.
   *
   * @param nodeElem Node エレメントを表すオブジェクト
   * @return プライベートノードオブジェクト
   */
  private Optional<? extends BhNode> genPirvateNode(Element nodeElem) {
    NodeConstructor constructor = new NodeConstructor(
        fnRegisterNodeTemplate,
        fnRegisterCnctrTemplate,
        fnRegisterOrgAndDervNodeId,
        fnGetCnctrTemplate,
        fnGetCnctrAttr,
        scriptManager);

    Optional<? extends BhNode> privateNode = constructor.genTemplate(nodeElem);
    boolean success = privateNode
        .map(node -> fnRegisterNodeTemplate.apply(node.getId(), node)).orElse(false);

    return success ? privateNode : Optional.empty();
  }
}
