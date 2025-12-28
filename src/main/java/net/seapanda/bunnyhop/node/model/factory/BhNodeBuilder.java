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

package net.seapanda.bunnyhop.node.model.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.factory.XmlBhNodeRepository.DerivationCorrespondence;
import net.seapanda.bunnyhop.node.model.factory.XmlBhNodeRepository.ModelArchive;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeType;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.section.ConnectorSection;
import net.seapanda.bunnyhop.node.model.section.Section;
import net.seapanda.bunnyhop.node.model.section.Subsection;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.script.BhScriptRepository;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link BhNode} が定義された xml の Node エレメント以下の情報から {@link BhNode} を作成する.
 *
 * @author K.Koike
 */
class BhNodeBuilder {

  private final ModelArchive archive;
  private final BhScriptRepository repository;
  private final ModelGenerator generator;
  private final TextDatabase textDb;

  /**
   * コンストラクタ.
   *
   * @param archive このオブジェクトに作成したノードやコネクタを格納する
   * @param repository {@link BhNode} の定義ファイルに書かれた外部スクリプトを保持するオブジェクト
   * @param generator このオブジェクトが作成する {@link BhNode} に渡すオブジェクト
   */
  public BhNodeBuilder(
      ModelArchive archive,
      BhScriptRepository repository,
      ModelGenerator generator,
      TextDatabase textDb) {
    this.archive = archive;
    this.repository = repository;
    this.generator = generator;
    this.textDb = textDb;
  }

  /**
   * {@code doc} 以下の xml 要素からノードツリーを作成する.
   *
   * @param doc このオブジェクトからノードツリーを作成する
   * @return 作成したノードツリーのルート {@link BhNode} オブジェクト
   */
  public Optional<? extends BhNode> build(Document doc) {
    if (!doc.getFirstChild().getNodeName().equals(BhConstants.BhModelDef.ELEM_NODE)) {
      LogManager.logger().error(String.format("""
          Invalid BhNode definition. (%s)
          A BhNode definition must have a '%s' root element.\n%s
          """,
          doc.getFirstChild().getNodeName(),
          BhConstants.BhModelDef.ELEM_NODE,
          doc.getBaseURI()));
      return Optional.empty();
    }
    return build(doc.getDocumentElement());
  }

  /**
   * {@code elem} 以下の xml 要素からノードツリーを作成する.
   *
   * @param elem Node エレメント
   * @param archive 作成したノードやコネクタの格納先
   * @return 作成したノードツリーのルート {@link BhNode} オブジェクト
   */
  public Optional<? extends BhNode> build(Element elem) {
    Optional<? extends BhNode> templateNode = Optional.empty();
    String typeName = elem.getAttribute(BhConstants.BhModelDef.ATTR_TYPE);
    BhNodeType type = BhNodeType.toType(typeName);
    switch (type) {
      //<Node type="connective">
      case CONNECTIVE:
        templateNode = genConnectiveNode(elem);
        break;

      //<Node type="text">
      case TEXT:
        templateNode = genTextNode(elem);
        break;

      default:
        LogManager.logger().error(
            "Unknown BhNode type.  (%s)\n%s".formatted(type, elem.getBaseURI()));
        break;
    }
    return templateNode;
  }

  /**
   * Derivation エレメントの情報を取得する.
   *
   * @param elem 派生先情報が書いてあるエレメント
   * @param orgNodeId 派生ノードを持つノードのID
   * @return 派生先 ID と派生ノード ID のマップ
   */
  private Optional<Map<DerivationId, BhNodeId>> genDerivationAndDerivative(
      Element elem, BhNodeId orgNodeId) {

    boolean success = true;
    Map<DerivationId, BhNodeId> derivationToDerivative = new HashMap<>();
    List<Element> derivationElems =
        getElementsByTagNameFromChild(elem, BhConstants.BhModelDef.ELEM_DERIVATION);
    for (Element derivationElem : derivationElems) {
      DerivationId derivationId =
          DerivationId.of(derivationElem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATION_ID));
      if (derivationId.equals(DerivationId.NONE)) {
        LogManager.logger().error(String.format(
            "A '%s' element must have a '%s' attribute.\n%s",
            BhConstants.BhModelDef.ELEM_DERIVATION,
            BhConstants.BhModelDef.ATTR_DERIVATION_ID,
            elem.getBaseURI()));
        success &= false;
        continue;
      }

      BhNodeId derivativeId =
          BhNodeId.of(derivationElem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_ID));
      if (derivativeId.equals(BhNodeId.NONE)) {
        LogManager.logger().error(String.format(
            "A '%s' element must have a '%s' attribute.\n%s",
            BhConstants.BhModelDef.ELEM_DERIVATION,
            BhConstants.BhModelDef.ATTR_DERIVATIVE_ID,
            elem.getBaseURI()));
        success &= false;
        continue;
      }
      derivationToDerivative.put(derivationId, derivativeId);
      archive.putDerivationCorrespondence(
          new DerivationCorrespondence(orgNodeId, derivativeId));
    }

    if (!success) {
      return Optional.empty();
    }
    return Optional.of(derivationToDerivative);
  }

  /**
   * {@link ConnectiveNode} を構築する.
   *
   * @param elem Node エレメント
   * @return {@link ConnectiveNode} オブジェクト
   */
  private Optional<ConnectiveNode> genConnectiveNode(Element elem) {
    BhNodeAttributes nodeAttrs = BhNodeAttributes.create(elem, textDb);

    if (nodeAttrs.bhNodeId().equals(BhNodeId.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_BH_NODE_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }

    if (nodeAttrs.version().equals(BhNodeVersion.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_VERSION,
          elem.getBaseURI()));
      return Optional.empty();
    }
    Optional<ArrayList<Section>> childSection = genSectionList(elem);
    if (childSection.isEmpty()) {
      return Optional.empty();
    }

    if (childSection.get().size() != 1) {
      LogManager.logger().error(String.format(
          ("A '%s' element whose '%s' is '%s' must have a child '%s' element "
          + "or a child '%s' element.\n%s"),
          BhConstants.BhModelDef.ELEM_SECTION,
          BhConstants.BhModelDef.ATTR_TYPE,
          BhConstants.BhModelDef.ATTR_VAL_CONNECTIVE,
          BhConstants.BhModelDef.ELEM_SECTION,
          BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION,
          elem.getBaseURI()));
      return Optional.empty();
    }

    //実行時スクリプト存在チェック
    boolean allScriptsFound = repository.allExistIgnoringEmptyWithHandler(
        scriptName -> outputScriptNotFoundMsg(scriptName, elem.getBaseURI()),
        nodeAttrs.onMovedFromChildToWs(),
        nodeAttrs.onMovedFromWsToChild(),
        nodeAttrs.onChildReplaced(),
        nodeAttrs.onDeletionRequested(),
        nodeAttrs.onCutRequested(),
        nodeAttrs.onCopyRequested(),
        nodeAttrs.onCompileErrorChecking(),
        nodeAttrs.onCompanionNodesCreating(),
        nodeAttrs.onCreatedAsTemplate(),
        nodeAttrs.onUiEventReceived(),
        nodeAttrs.onAliasAsked(),
        nodeAttrs.onUserDefinedNameAsked(),
        nodeAttrs.onRelatedNodesRequired());
    if (!allScriptsFound) {
      return Optional.empty();
    }

    return genDerivationAndDerivative(elem, nodeAttrs.bhNodeId())
        .map(derivationToDerivative -> generator.newConnectiveNode(
            childSection.get().get(0),
            derivationToDerivative, nodeAttrs));
  }

  /** {@link TextNode} を構築する. */
  private Optional<TextNode> genTextNode(Element elem) {
    BhNodeAttributes nodeAttrs = BhNodeAttributes.create(elem, textDb);

    if (nodeAttrs.bhNodeId().equals(BhNodeId.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_BH_NODE_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }

    if (nodeAttrs.version().equals(BhNodeVersion.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' element must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_VERSION,
          elem.getBaseURI()));
      return Optional.empty();
    }

    //実行時スクリプト存在チェック
    boolean allScriptsFound = repository.allExistIgnoringEmptyWithHandler(
        scriptName -> outputScriptNotFoundMsg(scriptName, elem.getBaseURI()),
        nodeAttrs.onMovedFromChildToWs(),
        nodeAttrs.onMovedFromWsToChild(),
        nodeAttrs.onTextChecking(),
        nodeAttrs.onTextFormatting(),
        nodeAttrs.onDeletionRequested(),
        nodeAttrs.onCutRequested(),
        nodeAttrs.onCopyRequested(),
        nodeAttrs.onCompileErrorChecking(),
        nodeAttrs.onCompanionNodesCreating(),
        nodeAttrs.onTextOptionsCreating(),
        nodeAttrs.onCreatedAsTemplate(),
        nodeAttrs.onUiEventReceived(),
        nodeAttrs.onAliasAsked(),
        nodeAttrs.onUserDefinedNameAsked(),
        nodeAttrs.onRelatedNodesRequired());
    if (!allScriptsFound) {
      return Optional.empty();
    }

    return genDerivationAndDerivative(elem, nodeAttrs.bhNodeId())
        .map(derivationToDerivative -> generator.newTextNode(derivationToDerivative, nodeAttrs));
  }

  /**
   * {@code parentTag} で指定したエレメントより下の {@link Section} リストを作成する.
   *
   * @param elem Section エレメント or ConnectorSection エレメントを子に持つエレメント
   * @return {@code elem} より下の {@link Section} リスト
   */
  private Optional<ArrayList<Section>> genSectionList(Element elem) {
    if (elem == null) {
      return Optional.of(new ArrayList<>());
    }
    ArrayList<Optional<? extends Section>> sectionListTmp = new ArrayList<>();
    NodeList sections = elem.getChildNodes();
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
   * @param elem Section エレメント
   * @return Section エレメントの内容を反映した {@link Subsection} オブジェクト
   */
  private Optional<Subsection> genSection(Element elem) {
    String groupName = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    return genSectionList(elem).map(sectionList -> new Subsection(groupName, sectionList));
  }

  /**
   * ConnectorSection エレメントから {@link ConnectorSection} オブジェクトを作成する.
   *
   * @param elem ConnectorSection エレメント
   * @return ConnectorSection エレメント の内容を反映した {@link ConnectorSection} オブジェクト
   */
  private Optional<ConnectorSection> genConnectorSection(Element elem) {
    final String sectionName = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    Collection<Element> connectorTags = 
        getElementsByTagNameFromChild(elem, BhConstants.BhModelDef.ELEM_CONNECTOR);
    List<Connector> cnctrList = new ArrayList<>();
  
    if (connectorTags.isEmpty()) {
      LogManager.logger().error(String.format(
          "A '%s' element must have at least one child '%s' element.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR_SECTION,
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          elem.getBaseURI()));
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
   * @param elem Connector エレメント
   * @return プライベートコネクタとコネクタインスタンス化時のパラメータのペア
   */
  private Optional<Connector> genConnector(Element elem) {
    List<Element> privateNodeTagList =
        getElementsByTagNameFromChild(elem, BhConstants.BhModelDef.ELEM_NODE);
    if (privateNodeTagList.size() >= 2) {
      LogManager.logger().error(String.format(
          "A '%s' element cannot have more than two child '%s' elements.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ELEM_NODE,
          elem.getBaseURI()));
      return Optional.empty();
    }
    // プライベートノードがある
    if (privateNodeTagList.size() == 1) {
      Optional<? extends BhNode> privateNode = genPirvateNode(privateNodeTagList.get(0));
      if (privateNode.isPresent()
          && !elem.hasAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID)) {
        elem.setAttribute(
            BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
            privateNode.get().getId().toString());
      }
    }
    // コネクタ作成
    return new ConnectorBuilder(archive, repository, generator)
        .build(elem)
        .map(cnctr -> archive.putConnector(cnctr.getId(), cnctr) ? cnctr : null);
  }

  /**
   * プライベートノードの定義 (= Connector エレメントの下に定義されたノード) から {@link BhNode} を作成する.
   *
   * @param elem Node エレメント
   * @return プライベートノードオブジェクト
   */
  private Optional<? extends BhNode> genPirvateNode(Element elem) {
    return build(elem)
        .map(node -> archive.putNode(node.getId(), node) ? node : null);
  }

  /**
   * 指定した名前を持つ子タグを探す.
   *
   * @param elem 子タグを探すタグ
   * @param childTagName 探したいタグ名
   * @return {@code childTagName} で指定した名前を持つタグリスト
   */
  private static List<Element> getElementsByTagNameFromChild(Element elem, String childTagName) {
    ArrayList<Element> selectedElementList = new ArrayList<>();
    for (int i = 0; i < elem.getChildNodes().getLength(); ++i) {
      Node node = elem.getChildNodes().item(i);
      if (node instanceof Element) {
        Element tag = (Element) node;
        if (tag.getTagName().equals(childTagName)) {
          selectedElementList.add(tag);
        }
      }
    }
    return selectedElementList;
  }

  /**
   * 外部スクリプトが見つからなかったときのエラーメッセージを出力する.
   *
   * @param scriptName 見つからなかった外部スクリプト名
   * @param fileName {@code scriptName} が書いてあったファイルの名前
   */
  private static void outputScriptNotFoundMsg(String scriptName, String fileName) {
    LogManager.logger().error(
        "Cannot find '%s'.  file: %s".formatted(scriptName, fileName));
  }
}
