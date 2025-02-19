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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.ModelConstructionException;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorParamSetId;
import net.seapanda.bunnyhop.service.BhScriptRepository;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * {@link BhNode} の生成に使う元データを保持するクラス.
 *
 * @author K.Koike
 */
public class XmlBhNodeRepository implements BhNodeRepository {

  private final ModelArchive archive;
  private final BhScriptRepository repository;

  /**
   * コンストラクタ.
   *
   * @param repository {@link BhNode} の定義ファイルに書かれた外部スクリプトを保持するオブジェクト
   */
  public XmlBhNodeRepository(BhScriptRepository repository) {
    Objects.requireNonNull(repository);
    this.repository = repository;
    this.archive = new ModelArchive();
  }

  /**
   * 引数で指定したディレクトリ以下の xml ファイルから {@link BhNode} を作成し保持する.
   *
   * @param nodeDirPath このディレクトリ以下から {@link BhNode} の定義ファイルを探す.
   * @param cnctrDirPath このディレクトリ以下から {@link Connector} のパラメータの定義ファイルを探す.
   * @param generator ノードやコネクタの生成に使用するオブジェクト
   * @param textDb ノードが参照するテキストデータを保持したオブジェクト
   * @return 全てのファイルから正しくテンプレートを作成できた場合 true
   * @throws ModelConstructionException {@link BhNode} の作成時に参照するデータの構築に失敗したことを表す例外
   */
  public boolean collect(
      Path nodeDirPath,
      Path cnctrDirPath,
      ModelGenerator generator,
      TextDatabase textDb) throws ModelConstructionException {
    boolean success = true;
    var builder = new BhNodeBuilder(archive, repository, generator, textDb);
    success &= genCnctrParamSet(cnctrDirPath);
    success &= genNode(nodeDirPath, builder);
    String msg = "Failed to construct template nodes.";
    if (!success) {
      throw new ModelConstructionException(msg);
    }
    success &= checkIfAllDefaultNodesExist();
    if (!success) {
      throw new ModelConstructionException(msg);
    }
    return success;
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
      LogManager.logger().error("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }
    // コネクタ設定ファイル読み込み & 登録
    boolean success = true;
    for (Path file : files) {
      Element rootElem = toRootElem(file);
      if (rootElem == null) {
        success = false;
        continue;
      }
      if (!rootElem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET)) {
        continue;
      }
      success &= buildParamSet(rootElem)
          .map(params -> registerCnctrParamset(params, file))
          .orElse(false);
    }
    return success;
  }

  /** コネクタパラメータセットを作成する. */
  private Optional<ConnectorAttribute> buildParamSet(Element elem) {
    //ルートエレメントチェック
    if (!elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET)) {
      LogManager.logger().error(String.format("""
          Invalid connector parameter set definition (%s).
          A connector parameter set definition must have a '%s' root element.\n%s
          """,
          elem.getNodeName(),
          BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET,
          elem.getBaseURI()));
      return Optional.empty();
    }
    return Optional.of(ConnectorAttribute.of(elem));
  }


  private boolean registerCnctrParamset(ConnectorAttribute attrbutes, Path file) {
    if (attrbutes.paramSetId().equals(ConnectorParamSetId.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET,
          BhConstants.BhModelDef.ATTR_PARAM_SET_ID,
          file.toAbsolutePath()));
      return false;
    }
    if (archive.hasConnectorAttribute(attrbutes.paramSetId())) {
      LogManager.logger().error(String.format(
          "Duplicated '%s'. (%s)\n%s",
          BhConstants.BhModelDef.ATTR_PARAM_SET_ID,
          attrbutes.paramSetId(),
          file.toAbsolutePath()));
      return false;
    }
    archive.putConnectorAttributes(attrbutes.paramSetId(), attrbutes);
    return true;
  }

  private Element toRootElem(Path file) {
    try {
      DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbfactory.newDocumentBuilder();
      return builder.parse(file.toFile()).getDocumentElement();
    } catch (IOException | ParserConfigurationException | SAXException e) {
      LogManager.logger().error(e + "\n" + file.toAbsolutePath());
      return null;
    }
  }

  /**
   * ノードを作成し, ハッシュに格納する.
   *
   * @param dirPath このディレクトリ以下から {@link BhNode} の定義ファイルを探す
   * @param builder ノード生成用オブジェクト
   * @return 処理に成功した場合 true
   */
  private boolean genNode(Path dirPath, BhNodeBuilder builder) {
    // ノードファイルパスリスト取得
    Stream<Path> files;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"));
    } catch (IOException e) {
      LogManager.logger().error("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }
    // ノード設定ファイル読み込み
    boolean success = files
        .map(file -> {
          Optional<? extends BhNode> nodeOpt = genNodeFromFile(file, builder);
          nodeOpt.ifPresent(node -> archive.putNode(node.getId(), node));
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
   * @param builder ノード生成用オブジェクト
   * @return {@code file} から作成したノード
   */
  private Optional<? extends BhNode> genNodeFromFile(Path file, BhNodeBuilder builder) {
    try {
      DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfactory.newDocumentBuilder();
      Document doc = docBuilder.parse(file.toFile());
      return builder.build(doc);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      LogManager.logger().error(e + "\n" + file.toAbsolutePath());
      return Optional.empty();
    }
  }

  /**
   * コネクタに最初につながっているノードをテンプレートコネクタに登録する.
   *
   * @return 全てのコネクタに対し、ノードの登録が成功した場合 true を返す
   */
  private boolean checkIfAllDefaultNodesExist() {
    List<Connector> errCnctrs = archive.getConnectorIdToConnector().values().stream()
        .filter(cnctr -> !archive.hasNodeOf(cnctr.getDefaultNodeId()))
        .toList();
    
    for (Connector errCnctr : errCnctrs) {
      LogManager.logger().error(String.format(
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
    for (DerivationCorrespondence pair : archive.getDerivationCorrespondence()) {
      if (!archive.hasNodeOf(pair.derivative())) {
        LogManager.logger().error(String.format(
            "Cannot find '%s' with the '%s' matching the '%s' %s that is defined in %s.",
            BhConstants.BhModelDef.ELEM_NODE,
            BhConstants.BhModelDef.ATTR_BH_NODE_ID,
            BhConstants.BhModelDef.ATTR_DETIVATION_ID,
            pair.derivative(),
            pair.original()));
        allValid = false;
      }
      
      BhNode original = archive.getNode(pair.original());
      BhNode derivative = archive.getNode(pair.derivative());
      if (original.getClass() != derivative.getClass()) {
        LogManager.logger().error(
            """
            An original node and it's derivative node must have the same '%s' Attribute.
                original: %s    derivative: %s
            """.formatted(BhConstants.BhModelDef.ATTR_TYPE, pair.original(), pair.derivative()));
        allValid = false;
      }      
    }
    return allValid;
  }

  @Override
  public BhNode getNodeOf(BhNodeId id) {
    return archive.getNode(id);
  }

  @Override
  public Collection<BhNode> getAll() {
    return archive.getAllNodes();
  }

  @Override
  public boolean hasNodeOf(BhNodeId id) {
    return archive.hasNodeOf(id);
  }

  /** オリジナルノードの ID と, その派生ノードの ID のペア. */
  record DerivationCorrespondence(BhNodeId original, BhNodeId derivative) {}

  /**
   * {@link BhNode} や {@link Connector} とそれらに関連する情報を登録および取得する機能を提供するクラス.
   *
   * @author K.Koike
   */
  class ModelArchive {

    /** {@link BhNode} のテンプレートを格納するハッシュ.*/
    private final Map<BhNodeId, BhNode> nodeIdToNode = new HashMap<>();
    /** {@link Connector} のテンプレートを格納するハッシュ.*/
    private final Map<ConnectorId, Connector> cnctrIdToCnctr = new HashMap<>();
    /** オリジナルノードと, その派生ノードの ID を格納する. */
    private final Set<DerivationCorrespondence> derivationCorrespondenceSet = new HashSet<>();
    /** コネクタパラメータセットを格納するハッシュ.  */
    private final Map<ConnectorParamSetId, ConnectorAttribute> cnctrParamIdToCnctrAttributes
        = new HashMap<>();

    /**
     * ノード ID とノードを登録する.
     *
     * @param nodeId テンプレートとして登録する {@link BhNode} の ID
     * @param node 登録する {@link BhNode}
     * @return 登録に成功した場合 true
     */
    boolean putNode(BhNodeId nodeId, BhNode node) {
      if (nodeIdToNode.containsKey(nodeId)) {
        LogManager.logger().error(
            "Duplicated '%s'  (%s)".formatted(BhConstants.BhModelDef.ATTR_BH_NODE_ID, nodeId));
        return false;
      }
      nodeIdToNode.put(nodeId, node);
      return true;
    }

    /**
     * ノード ID からノードを取得する.
     *
     * @param nodeId このノード ID に対応する {@link BhNode} を取得する
     * @return {@code nodeId} に対応する {@link BhNode} オブジェクト.
     *         見つからない場合は null.
     */
    BhNode getNode(BhNodeId nodeId) {
      return nodeIdToNode.get(nodeId);
    }

    Collection<BhNode> getAllNodes() {
      return new ArrayList<>(nodeIdToNode.values());
    }

    /**
     * {@code id} で指定したノード ID に対応する {@link BhNode} オブジェクトが登録済みか調べる. */
    boolean hasNodeOf(BhNodeId id) {
      return nodeIdToNode.containsKey(id);
    }

    /**
     * コネクタ ID とコネクタを登録する.
     *
     * @param cnctrId 登録する {@link Connector} の ID
     * @param cnctr 登録する {@link Connector}
     * @return 登録に成功した場合 true
     */
    boolean putConnector(ConnectorId cnctrId, Connector cnctr) {
      if (this.cnctrIdToCnctr.containsKey(cnctrId)) {
        LogManager.logger().error(String.format(
            "Duplicated '%s'.  (%s)", BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID, cnctrId));
        return false;
      }
      cnctrIdToCnctr.put(cnctrId, cnctr);
      return true;
    }

    /** このオブジェクトに登録された コネクタ ID と, コネクタのマップを取得する. */
    Map<ConnectorId, Connector> getConnectorIdToConnector() {
      return new HashMap<>(cnctrIdToCnctr);
    }

    /**
     * オリジナルノードの ID と, その派生ノードの ID のペアを登録する.
     *
     * @param orgNodeId オリジナルノードの ID
     * @param dervNodeId 派生ノードの ID
     */
    void putDerivationCorrespondence(DerivationCorrespondence pair) {
      derivationCorrespondenceSet.add(pair);
    }

    /** このオブジェクトに登録された オリジナルノードの ID と, その派生ノードの ID のペアを取得する. */
    Set<DerivationCorrespondence> getDerivationCorrespondence() {
      return new HashSet<>(derivationCorrespondenceSet);
    }

    /** {@code id} に対応する {@link ConnectorAttribute} オブジェクトを取得する. */
    Optional<ConnectorAttribute> getConnectorAttribute(ConnectorParamSetId id) {
      return Optional.ofNullable(cnctrParamIdToCnctrAttributes.get(id));
    }

    /** {@code id} に対応する {@link ConnectorAttributes} オブジェクトを登録する. */
    Optional<ConnectorAttribute> putConnectorAttributes(
        ConnectorParamSetId id, ConnectorAttribute attr) {
      return Optional.ofNullable(cnctrParamIdToCnctrAttributes.put(id, attr));
    }

    /**
     * {@code id} で指定したコネクタパラメータ ID に対応する
     * {@link ConnectorAttributes} オブジェクトが登録済みか調べる.
     */
    boolean hasConnectorAttribute(ConnectorParamSetId id) {
      return cnctrParamIdToCnctrAttributes.containsKey(id);
    }
  }
}
