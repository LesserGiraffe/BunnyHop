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

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorId;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * {@link BhNode} の生成に使うテンプレートを保持するクラス.
 *
 * @author K.Koike
 */
public class BhNodeTemplates {

  public static final BhNodeTemplates INSTANCE = new BhNodeTemplates(); //!< シングルトンインスタンス

  /** {@link BhNode} のテンプレートを格納するハッシュ. Node タグの bhID がキー. */
  private final HashMap<BhNodeId, BhNode> nodeIdToNodeTemplate = new HashMap<>();
  /** {@link Connector} のテンプレートを格納するハッシュ. Connector タグの bhID がキー. */
  private final HashMap<ConnectorId, Connector> cnctrIdToCntrTemplate = new HashMap<>();
  /** オリジナルノードと, そのイミテーションノードの ID を格納する. */
  private final List<Pair<BhNodeId, BhNodeId>> orgNodeIdToImitNodeId = new ArrayList<>();

  private BhNodeTemplates() {}

  /**
   * ノード ID から {@link BhNode} のテンプレートを取得する.
   *
   * @param id 取得したいノードのID
   * @return id で指定した {@link BhNode} のテンプレート.
   */
  private Optional<BhNode> getBhNodeTemplate(BhNodeId id) {
    return Optional.ofNullable(nodeIdToNodeTemplate.get(id));
  }

  /**
   * ノード ID から {@link BhNode} を新しく作る.
   *
   * @param id 取得したいノードの ID
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return id で指定したBhNodeのオブジェクト
   */
  public BhNode genBhNode(BhNodeId id, UserOperationCommand userOpeCmd) {
    BhNode newNode = nodeIdToNodeTemplate.get(id);
    if (newNode == null) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - template not found" + id);
    } else {
      newNode = newNode.copy(userOpeCmd, bhNode -> true);
    }
    return newNode;
  }

  /**
   * {@code id} に対応する {@link BhNode} が存在するかどうかを返す.
   *
   * @param id 存在を確認する {@link BhNode} の ID
   * @return  {@code id} に対応する {@link BhNode} が存在する場合 true
   * */
  public boolean bhNodeExists(BhNodeId id) {
    return nodeIdToNodeTemplate.get(id) != null;
  }

  /**
   * テンプレートを作成する.
   *
   * @return テンプレートノードの作成に成功した場合true
   */
  public boolean genTemplate() {
    boolean success = true;  //全てのファイルから正しくテンプレートを作成できた場合 true
    success &= genConnectorTemplate();
    success &= genNodeTemplate();
    if (!success) {
      return false;
    }
    success &= registerDefaultNodeWithConnector();
    success &= genCompoundNodes();
    return success;
  }

  /**
   * コネクタのテンプレートを作成し, ハッシュに格納する.
   *
   * @return テンプレートコネクタの作成に成功した場合 true
   */
  private boolean genConnectorTemplate() {
    //コネクタファイルパスリスト取得
    Path dirPath = Paths.get(
        Util.INSTANCE.execPath, BhParams.Path.BH_DEF_DIR, BhParams.Path.CONNECTOR_DEF_DIR);
    Stream<Path> files;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"));
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("connector directory not found " + dirPath);
      return false;
    }
    //コネクタ設定ファイル読み込み
    boolean success = files
        .map(file -> {
          Optional<? extends Connector> connectorOpt = genConnectorFromFile(file);
          connectorOpt.ifPresent(connector -> registerCnctrTemplate(connector.getId(), connector));
          return connectorOpt.isPresent();
        })
        .allMatch(Boolean::valueOf);

    files.close();
    return success;
  }

  /**
   * コネクタの定義ファイルを読んでコネクタを作成する.
   *
   * @param file コネクタが定義されたファイル
   * @return {@code file} から作成したコネクタ
   */
  private Optional<? extends Connector> genConnectorFromFile(Path file) {
    try {
      DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbfactory.newDocumentBuilder();
      Document doc = builder.parse(file.toFile());
      return new ConnectorConstructor().genTemplate(doc);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("ConnectorTemplates genTemplate \n" + e + "\n" +  file);
      return Optional.empty();
    }
  }

  /**
   * ノードのテンプレートを作成し, ハッシュに格納する.
   *
   * @return 処理に成功した場合 true
   */
  private boolean genNodeTemplate() {
    //ノードファイルパスリスト取得
    Path dirPath = Paths.get(
        Util.INSTANCE.execPath, BhParams.Path.BH_DEF_DIR, BhParams.Path.NODE_DEF_DIR);
    Stream<Path> files;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"));
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("node directory not found " + dirPath);
      return false;
    }
    //ノード設定ファイル読み込み
    boolean success = files
        .map(file -> {
          Optional<? extends BhNode> nodeOpt = genNodeFromFile(file);
          nodeOpt.ifPresent(node -> nodeIdToNodeTemplate.put(node.getId(), node));
          return nodeOpt.isPresent();
        })
        .allMatch(Boolean::valueOf);
    files.close();
    success &= checkImitationConsistency(orgNodeIdToImitNodeId);
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
          this::registerOrgNodeIdAndImitNodeId,
          this::getCnctrTemplate)
          .genTemplate(doc);
      return templateNode;
    } catch (IOException | ParserConfigurationException | SAXException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("NodeTemplates genTemplate \n" + e + "\n" +  file);
      return Optional.empty();
    }
  }

  /**
   * コネクタに最初につながっているノードをテンプレートコネクタに登録する.
   *
   * @return 全てのコネクタに対し、ノードの登録が成功した場合 true を返す
   */
  private boolean registerDefaultNodeWithConnector() {
    // 初期ノード (コネクタに最初につながっているノード) をコネクタに登録する
    var success = new MutableBoolean(true);
    for (Connector connector : cnctrIdToCntrTemplate.values()) {
      getDefaultNode(connector).ifPresentOrElse(
          initNode -> connector.connectNode(initNode, new UserOperationCommand()),
          () -> success.setFalse());
    }
    return success.getValue();
  }

  /** {@code connector} に設定された初期ノードを取得する. */
  private Optional<BhNode> getDefaultNode(Connector connector) {
    BhNodeId defNodeId = connector.defaultNodeId;
    Optional<BhNode> defNode = getBhNodeTemplate(defNodeId);
    BhNodeId initNodeId = connector.initNodeId;
    Optional<BhNode> initNode =
        initNodeId.equals(BhNodeId.NONE) ? defNode : getBhNodeTemplate(initNodeId);

    //ノードテンプレートが見つからない
    if (defNode.isEmpty()) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhParams.BhModelDef.ELEM_CONNECTOR + ">" + " タグの "
          + BhParams.BhModelDef.ATTR_DEFAULT_BHNODE_ID + " (" + defNodeId + ") " + "と一致する "
          + BhParams.BhModelDef.ATTR_BH_NODE_ID + " を持つ"
          + BhParams.BhModelDef.ELEM_NODE + " の定義が見つかりません.");
      return Optional.empty();
    }
    if (initNode.isEmpty()) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhParams.BhModelDef.ELEM_CONNECTOR + ">" + " タグの "
          + BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID + " (" + initNodeId + ") " + "と一致する "
          + BhParams.BhModelDef.ATTR_BH_NODE_ID + " を持つ "
          + BhParams.BhModelDef.ELEM_NODE + " の定義が見つかりません.");
      return Optional.empty();
    }
    return initNode;
  }

  /**
   * オリジナルノードとイミテーションノード間の制約が満たされているか検査する.
   *
   * @param listOfOrgNodeIdAndImitNodeId オリジナルノードとイミテーションノードの ID のリスト
   * @return 全てのオリジナルノードとイミテーションノード間の制約が満たされていた場合 true
   */
  private boolean checkImitationConsistency(
      List<Pair<BhNodeId, BhNodeId>> listOfOrgNodeIdAndImitNodeId) {
    var allValid = true; 
    for (Pair<BhNodeId, BhNodeId> orgIdAndImitId : listOfOrgNodeIdAndImitNodeId) {
      var orgId = orgIdAndImitId.v1;
      var imitId = orgIdAndImitId.v2;
      if (!bhNodeExists(imitId)) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            "\"" + imitId + "\"" + " を "
            + BhParams.BhModelDef.ATTR_BH_NODE_ID + " に持つ "
            + BhParams.BhModelDef.ELEM_NODE + " が見つかりません. "
            + "(" + orgId + ")");
        allValid = false;
      }
      
      BhNode orgNode = getBhNodeTemplate(orgId).get();
      BhNode imitNode = getBhNodeTemplate(imitId).get();
      if (orgNode.getClass() != imitNode.getClass()) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            BhParams.BhModelDef.ATTR_IMITATION_NODE_ID + " 属性を持つ"
            + BhParams.BhModelDef.ELEM_NODE + " タグの "
            + BhParams.BhModelDef.ATTR_TYPE + " と "
            + BhParams.BhModelDef.ATTR_IMITATION_NODE_ID + " で指定された "
            + BhParams.BhModelDef.ELEM_NODE + " の "
            + BhParams.BhModelDef.ATTR_TYPE + " は同じでなければなりません.\n"
            + "    org: " + orgId + "    imit: " + imitId);
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
  public void registerNodeTemplate(BhNodeId nodeId, BhNode nodeTemplate) {
    // テンプレートノードを登録する場合は, テンプレートコネクタが参照しているノードも変える
    if (nodeIdToNodeTemplate.containsKey(nodeId)) {
      cnctrIdToCntrTemplate.values().stream()
          .filter(connector -> connector.getConnectedNode().getId().equals(nodeId))
          .forEach(connector -> connector.connectNode(nodeTemplate, new UserOperationCommand()));
    }
    nodeIdToNodeTemplate.put(nodeId, nodeTemplate);
  }

  /**
   * コネクタ ID とコネクタテンプレートを登録する.
   *
   * @param cnctrId テンプレートとして登録する {@link Connector} の ID
   * @param cnctrTemplate 登録する {@link Connector} テンプレート
   */
  private void registerCnctrTemplate(ConnectorId cnctrId, Connector cnctrTemplate) {
    cnctrIdToCntrTemplate.put(cnctrId, cnctrTemplate);
  }

  /**
   * オリジナルノードの ID と, そのイミテーションノードの ID を登録する.
   *
   * @param orgNodeId オリジナルノードの ID
   * @param imitNodeId イミテーションノードの ID
   */
  private void registerOrgNodeIdAndImitNodeId(BhNodeId orgNodeId, BhNodeId imitNodeId) {
    orgNodeIdToImitNodeId.add(new Pair<>(orgNodeId, imitNodeId));
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


  /** 複合ノードを作成する. */
  private boolean genCompoundNodes() {
    Script cs = BhScriptManager.INSTANCE.getCompiledScript(BhParams.Path.GEN_COMPOUND_NODES_JS);
    ScriptableObject scriptScope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, new UserOperationCommand());
    ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_TEMPLATES, INSTANCE);
    ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_UTIL, Util.INSTANCE);
    try {
      ContextFactory.getGlobal().call(cx -> cs.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug("eval " + BhParams.Path.GEN_COMPOUND_NODES_JS + "\n" + e);
      return false;
    }
    return true;
  }

  /**
   * {@link BhNode} の処理時に呼ばれるスクリプトが存在するかどうか調べる.
   * ただし, スクリプト名が null か空文字だった場合, そのスクリプトの存在は調べない.
   *
   * @param fileName {@code scriptNames} が書いてあるファイル名
   * @param scriptNames 実行されるスクリプト名
   * @return null か空文字以外のスクリプト名に対応するスクリプトが全て見つかった場合 true を返す
   * */
  public static boolean allScriptsExist(String fileName, String... scriptNames) {
    String[] scriptNamesFiltered = Stream.of(scriptNames)
        .filter(scriptName -> scriptName != null && !scriptName.isEmpty())
        .toArray(String[]::new);

    return BhScriptManager.INSTANCE.scriptsExist(fileName, scriptNamesFiltered);
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
