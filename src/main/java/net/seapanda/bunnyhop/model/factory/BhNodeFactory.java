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

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorAttributes;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorParamSetId;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * {@link BhNode} の生成に使う元データを保持するクラス.
 *
 * @author K.Koike
 */
public class BhNodeFactory {

  /** {@link BhNode} のテンプレートを格納するハッシュ.*/
  private final HashMap<BhNodeId, BhNode> nodeIdToNodeTemplate = new HashMap<>();
  /** {@link Connector} のテンプレートを格納するハッシュ.*/
  private final HashMap<ConnectorId, Connector> cnctrIdToCntrTemplate = new HashMap<>();
  /** オリジナルノードと, その派生ノードの ID を格納する. */
  private final Set<Pair<BhNodeId, BhNodeId>> orgAndDerivativeIdSet = new HashSet<>();
  /** コネクタパラメータセットを格納するハッシュ.  */
  private final Map<ConnectorParamSetId, ConnectorAttributes> cnctrParamIdToCnctrAttributes
      = new HashMap<>();
  private final BhScriptManager scriptManager;

  /**
   * 引数で指定したディレクトリ以下から xml ファイルを探して, {@link BhNode} の雛形を作成する.
   *
   * @param nodeDirPath このディレクトリ以下から {@link BhNode} の定義ファイルを探す.
   * @param cnctrDirPath このディレクトリ以下から {@link Connector} のパラメータの定義ファイルを探す.
   * @param scriptManager {@link BhNode} および {@link Connector} のパラメータの定義ファイルに書かれた
   *                      JavaScript ファイルを保持する {@link BhScriptManager} オブジェクト
   * @throws NodeTemplateConstructionException {@link BhNode} の作成時に参照するデータの構築に失敗したことを表す例外.
   */
  public BhNodeFactory(Path nodeDirPath, Path cnctrDirPath, BhScriptManager scriptManager)
      throws NodeTemplateConstructionException {
    boolean success = true;  //全てのファイルから正しくテンプレートを作成できた場合 true
    this.scriptManager = scriptManager;
    success &= genCnctrParamSet(cnctrDirPath);
    success &= genNodeTemplates(nodeDirPath);
    String msg = "Failed to construct template nodes.";
    if (!success) {
      throw new NodeTemplateConstructionException(msg);
    }
    success &= checkIfAllDefaultNodesExist();
    if (!success) {
      throw new NodeTemplateConstructionException(msg);
    }
  }

  /**
   * ノード ID から {@link BhNode} のテンプレートを取得する.
   *
   * @param id 取得したいノードのID
   * @return {@code id} で指定した {@link BhNode} のテンプレート.
   */
  private Optional<BhNode> getBhNodeTemplate(BhNodeId id) {
    return Optional.ofNullable(nodeIdToNodeTemplate.get(id));
  }

  /**
   * ノード ID から {@link BhNode} を新しく作る.
   *
   * @param id 取得したいノードの ID
   * @param userOpe undo 用コマンドオブジェクト
   * @return id で指定した {@link BhNode} のオブジェクト
   */
  public BhNode create(BhNodeId id, UserOperation userOpe) {
    BhNode newNode = nodeIdToNodeTemplate.get(id);
    if (newNode == null) {
      BhService.msgPrinter().errForDebug("Template not found.  (%s)".formatted(id));
    } else {
      newNode = newNode.copy(bhNode -> true, userOpe);
    }
    return newNode;
  }

  /** {@code id} に対応するコネクタパラメータセットを取得する. */
  public Optional<ConnectorAttributes> getConnectorAttributes(ConnectorParamSetId id) {
    return Optional.ofNullable(cnctrParamIdToCnctrAttributes.get(id));
  }

  /**
   * {@code id} に対応する {@link BhNode} が存在するかどうかを返す.
   *
   * @param id 存在を確認する {@link BhNode} の ID
   * @return  {@code id} に対応する {@link BhNode} が存在する場合 true
   * */
  public boolean bhNodeExists(BhNodeId id) {
    return nodeIdToNodeTemplate.containsKey(id);
  }

  /**
   * コネクタのパラメータセットを作成し, ハッシュに格納する.
   *
   * @param dirPath このディレクトリ以下から {@link Connector} のパラメータの定義ファイルを探す
   * @return コネクタのパラメータセットの作成に成功した場合 true
   */
  private boolean genCnctrParamSet(Path dirPath) {
    List<Path> files;  // 読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"))
          .toList();
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }
    // コネクタ設定ファイル読み込み & 登録
    boolean success = true;
    for (Path file : files) {
      Optional<ConnectorConstructor> constructor = toRootElem(file).map(
            elem -> new ConnectorConstructor(
                elem, id -> getConnectorAttributes(id), scriptManager));
      if (constructor.isEmpty()) {
        success = false;
      }
      if (!constructor.get().isParamSet()) {
        continue;
      }
      success &= constructor.get()
          .genParamSet()
          .map(attr -> registerCnctrParamset(attr, file))
          .orElse(false);
    }
    return success;
  }

  private boolean registerCnctrParamset(ConnectorAttributes attrbutes, Path file) {
    if (attrbutes.paramSetId().equals(ConnectorParamSetId.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET,
          BhConstants.BhModelDef.ATTR_PARAM_SET_ID,
          file.toAbsolutePath()));
      return false;
    }
    if (cnctrParamIdToCnctrAttributes.containsKey(attrbutes.paramSetId())) {
      BhService.msgPrinter().errForDebug(String.format(
          "Duplicated '%s'. (%s)\n%s",
          BhConstants.BhModelDef.ATTR_PARAM_SET_ID,
          attrbutes.paramSetId(),
          file.toAbsolutePath()));
      return false;
    }
    cnctrParamIdToCnctrAttributes.put(attrbutes.paramSetId(), attrbutes);
    return true;
  }

  private Optional<Element> toRootElem(Path file) {
    try {
      DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbfactory.newDocumentBuilder();
      return Optional.of(builder.parse(file.toFile()).getDocumentElement());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      BhService.msgPrinter().errForDebug(e + "\n" + file.toAbsolutePath());
      return Optional.empty();
    }
  }

  /**
   * ノードのテンプレートを作成し, ハッシュに格納する.
   *
   * @param dirPath このディレクトリ以下から {@link BhNode} の定義ファイルを探す
   * @return 処理に成功した場合 true
   */
  private boolean genNodeTemplates(Path dirPath) {
    // ノードファイルパスリスト取得
    Stream<Path> files;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"));
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }
    // ノード設定ファイル読み込み
    boolean success = files
        .map(file -> {
          Optional<? extends BhNode> nodeOpt = genNodeFromFile(file);
          nodeOpt.ifPresent(node -> nodeIdToNodeTemplate.put(node.getId(), node));
          return nodeOpt.isPresent();
        })
        .allMatch(Boolean::valueOf);
    success &= checkDerivativeConsistency();
    return success;
  }

  /**
   * ファイルからノードを作成する.
   *
   * @param file ノードが定義されたファイル
   * @return {@code file} から作成したノード
   */
  private Optional<? extends BhNode> genNodeFromFile(Path file) {
    try {
      DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbfactory.newDocumentBuilder();
      Document doc = builder.parse(file.toFile());
      Optional<? extends BhNode> templateNode = new NodeConstructor(
          this::registerNodeTemplate,
          this::registerCnctrTemplate,
          this::registerOrgAndDervId,
          this::getCnctrTemplate,
          this::getConnectorAttributes,
          scriptManager)
          .genTemplate(doc);
      return templateNode;
    } catch (IOException | ParserConfigurationException | SAXException e) {
      BhService.msgPrinter().errForDebug(e + "\n" + file.toAbsolutePath());
      return Optional.empty();
    }
  }

  /**
   * コネクタに最初につながっているノードをテンプレートコネクタに登録する.
   *
   * @return 全てのコネクタに対し、ノードの登録が成功した場合 true を返す
   */
  private boolean checkIfAllDefaultNodesExist() {
    List<Connector> errCnctrs = cnctrIdToCntrTemplate.values().stream()
        .filter(cnctr -> !this.nodeIdToNodeTemplate.containsKey(cnctr.getDefaultNodeId()))
        .toList();
    
    for (Connector errCnctr : errCnctrs) {
      BhService.msgPrinter().errForDebug(String.format(
          "Cannot find '%s' with the '%s' matching the '%s' %s.",
          BhConstants.BhModelDef.ELEM_NODE,
          BhConstants.BhModelDef.ATTR_BH_NODE_ID,
          BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
          errCnctr.getDefaultNodeId()));
    }
    return errCnctrs.isEmpty();
  }

  /**
   * オリジナルノードと派生ノード間の制約が満たされているか検査する.
   *
   * @return 全てのオリジナルノードと派生ノード間の制約が満たされていた場合 true
   */
  private boolean checkDerivativeConsistency() {
    var allValid = true; 
    for (Pair<BhNodeId, BhNodeId> orgIdAndDervId : orgAndDerivativeIdSet) {
      var orgId = orgIdAndDervId.v1;
      var dervId = orgIdAndDervId.v2;
      if (!bhNodeExists(dervId)) {
        BhService.msgPrinter().errForDebug(String.format(
            "Cannot find '%s' with the '%s' matching the '%s' %s that is defined in %s.",
            BhConstants.BhModelDef.ELEM_NODE,
            BhConstants.BhModelDef.ATTR_BH_NODE_ID,
            BhConstants.BhModelDef.ATTR_DETIVATION_ID,
            dervId,
            orgId));
        allValid = false;
      }
      
      BhNode original = getBhNodeTemplate(orgId).get();
      BhNode derivative = getBhNodeTemplate(dervId).get();
      if (original.getClass() != derivative.getClass()) {
        BhService.msgPrinter().errForDebug(
            """
            An original node and it's derivative node must have the same '%s' Attribute.
                original: %s    derivative: %s
            """.formatted(BhConstants.BhModelDef.ATTR_TYPE, orgId, dervId));
        allValid = false;
      }      
    }
    return allValid;
  }

  /**
   * ノード ID とノードテンプレートを登録する.
   *
   * @param nodeId テンプレートとして登録する {@link BhNode} の ID
   * @param nodeTemplate 登録する {@link BhNode} テンプレート
   */
  public boolean registerNodeTemplate(BhNodeId nodeId, BhNode nodeTemplate) {
    if (nodeIdToNodeTemplate.containsKey(nodeId)) {
      BhService.msgPrinter().errForDebug(
          "Duplicated '%s'  (%s)".formatted(BhConstants.BhModelDef.ATTR_BH_NODE_ID, nodeId));
      return false;
    }
    nodeIdToNodeTemplate.put(nodeId, nodeTemplate);
    return true;
  }

  /**
   * {@code nodeId} でしていしたノードテンプレートを {@code nodeTemplate} で上書きする.
   * {@code nodeId} のノードテンプレートが存在しない場合は {@code nodeTemplate} を登録する.
   *
   * @param nodeId この ID のノードテンプレートを上書きする.
   * @param nodeTemplate 新しいノードテンプレート.
   */
  public void overwriteNodeTemplate(BhNodeId nodeId, BhNode nodeTemplate) {
    nodeIdToNodeTemplate.put(nodeId, nodeTemplate);
  }

  /**
   * コネクタ ID とコネクタテンプレートを登録する.
   *
   * @param cnctrId テンプレートとして登録する {@link Connector} の ID
   * @param cnctrTemplate 登録する {@link Connector} テンプレート
   */
  private boolean registerCnctrTemplate(ConnectorId cnctrId, Connector cnctrTemplate) {
    if (this.cnctrIdToCntrTemplate.containsKey(cnctrId)) {
      BhService.msgPrinter().errForDebug(
          "Duplicated '%s'.  (%s)".formatted(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID, cnctrId));
      return false;
    }
    cnctrIdToCntrTemplate.put(cnctrId, cnctrTemplate);
    return true;
  }

  /**
   * オリジナルノードの ID と, その派生ノードの ID を登録する.
   *
   * @param orgNodeId オリジナルノードの ID
   * @param dervNodeId 派生ノードの ID
   */
  private void registerOrgAndDervId(BhNodeId orgNodeId, BhNodeId dervNodeId) {
    orgAndDerivativeIdSet.add(new Pair<>(orgNodeId, dervNodeId));
  }

  /**
   * コネクタテンプレートを取得する.
   *
   * @param cnctrId この ID を持つコネクタのテンプレートを取得する
   * @return コネクタテンプレート
   */
  private Optional<Connector> getCnctrTemplate(ConnectorId cnctrId) {
    return Optional.ofNullable(cnctrIdToCntrTemplate.get(cnctrId));
  }

  /**
   * 指定した名前を持つ子タグを探す.
   *
   * @param elem 子タグを探すタグ
   * @param childTagName 探したいタグ名
   * @return {@code childTagName} で指定した名前を持つタグリスト
   */
  public static List<Element> getElementsByTagNameFromChild(Element elem, String childTagName) {
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
}
