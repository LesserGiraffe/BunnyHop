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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorAttributes;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorParamSetId;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
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

  public static final BhNodeTemplates INSTANCE = new BhNodeTemplates();

  /** {@link BhNode} のテンプレートを格納するハッシュ.*/
  private final HashMap<BhNodeId, BhNode> nodeIdToNodeTemplate = new HashMap<>();
  /** {@link Connector} のテンプレートを格納するハッシュ.*/
  private final HashMap<ConnectorId, Connector> cnctrIdToCntrTemplate = new HashMap<>();
  /** オリジナルノードと, そのイミテーションノードの ID を格納する. */
  private final Set<Pair<BhNodeId, BhNodeId>> orgAndImitNodeIdSet = new HashSet<>();
  /** コネクタパラメータセットを格納するハッシュ.  */
  private final HashMap<ConnectorParamSetId, ConnectorAttributes> cnctrParamIdToCnctrAttributes
      = new HashMap<>();

  private BhNodeTemplates() {}

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
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return id で指定した {@link BhNode} のオブジェクト
   */
  public BhNode genBhNode(BhNodeId id, UserOperationCommand userOpeCmd) {
    BhNode newNode = nodeIdToNodeTemplate.get(id);
    if (newNode == null) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - template not found " + id);
    } else {
      newNode = newNode.copy(bhNode -> true, userOpeCmd);
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
    return nodeIdToNodeTemplate.get(id) != null;
  }

  /**
   * テンプレートを作成する.
   *
   * @return テンプレートノードの作成に成功した場合 true
   */
  public boolean genTemplate() {
    boolean success = true;  //全てのファイルから正しくテンプレートを作成できた場合 true
    success &= genCnctrParamSet();
    success &= genNodeTemplate();
    if (!success) {
      return false;
    }
    success &= checkIfAllDefaultNodesExist();
    success &= onTemplateCompleted();
    return success;
  }

  /**
   * コネクタのパラメータセットを作成し, ハッシュに格納する.
   *
   * @return コネクタのパラメータセットの作成に成功した場合 true
   */
  private boolean genCnctrParamSet() {
    // コネクタファイルパスリスト取得
    Path dirPath = Paths.get(
        Util.INSTANCE.execPath, BhConstants.Path.BH_DEF_DIR, BhConstants.Path.CONNECTOR_DEF_DIR);
    List<Path> files;  // 読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.toString().endsWith(".xml"))
          .toList();
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("connector directory not found " + dirPath);
      return false;
    }
    // コネクタ設定ファイル読み込み & 登録
    boolean success = true;
    for (Path file : files) {
      Optional<ConnectorConstructor> constructor = toRootElem(file).map(ConnectorConstructor::new);
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
      MsgPrinter.INSTANCE.errMsgForDebug(
          "A '" + BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET + "' element must have a '"
          + BhConstants.BhModelDef.ATTR_PARAM_SET_ID + "' attribute.\n" + file);
      return false;
    }
    if (cnctrParamIdToCnctrAttributes.containsKey(attrbutes.paramSetId())) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "Duplicated '" + BhConstants.BhModelDef.ATTR_PARAM_SET_ID
          + "' ("  + attrbutes.paramSetId() + ")\n" + file);
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
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + "\n" + e + "\n" +  file);
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
        Util.INSTANCE.execPath, BhConstants.Path.BH_DEF_DIR, BhConstants.Path.NODE_DEF_DIR);
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
    success &= checkImitationConsistency();
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
  private boolean checkIfAllDefaultNodesExist() {
    List<Connector> errCnctrs = cnctrIdToCntrTemplate.values().stream()
        .filter(cnctr -> !this.nodeIdToNodeTemplate.containsKey(cnctr.getDefaultNodeId()))
        .toList();
    
    for (Connector errCnctr : errCnctrs) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "There is no '" + BhConstants.BhModelDef.ELEM_NODE + "' with the '"
          + BhConstants.BhModelDef.ATTR_BH_NODE_ID + "' matching the '" 
          + BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID + "' "
          + errCnctr.getDefaultNodeId() + ".");
    }
    return errCnctrs.isEmpty();
  }

  /**
   * オリジナルノードとイミテーションノード間の制約が満たされているか検査する.
   *
   * @return 全てのオリジナルノードとイミテーションノード間の制約が満たされていた場合 true
   */
  private boolean checkImitationConsistency() {
    var allValid = true; 
    for (Pair<BhNodeId, BhNodeId> orgIdAndImitId : orgAndImitNodeIdSet) {
      var orgId = orgIdAndImitId.v1;
      var imitId = orgIdAndImitId.v2;
      if (!bhNodeExists(imitId)) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            "There is no '" + BhConstants.BhModelDef.ELEM_NODE + "' with the '"
            + BhConstants.BhModelDef.ATTR_BH_NODE_ID + "' matching the '"
            + BhConstants.BhModelDef.ATTR_IMITATION_ID + " " + imitId 
            + " that is defined in " + orgId + ".");
        allValid = false;
      }
      
      BhNode orgNode = getBhNodeTemplate(orgId).get();
      BhNode imitNode = getBhNodeTemplate(imitId).get();
      if (orgNode.getClass() != imitNode.getClass()) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            "An original node and it's imitation node must have the same '"
            + BhConstants.BhModelDef.ATTR_TYPE + "' attribute.\n"
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
  public boolean registerNodeTemplate(BhNodeId nodeId, BhNode nodeTemplate) {
    if (nodeIdToNodeTemplate.containsKey(nodeId)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "Duplicated '" + BhConstants.BhModelDef.ATTR_BH_NODE_ID + "' ("  + nodeId + ")");
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
      MsgPrinter.INSTANCE.errMsgForDebug(
          "Duplicated '" + BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID + "' ("  + cnctrId + ")");
      return false;
    }
    cnctrIdToCntrTemplate.put(cnctrId, cnctrTemplate);
    return true;
  }

  /**
   * オリジナルノードの ID と, そのイミテーションノードの ID を登録する.
   *
   * @param orgNodeId オリジナルノードの ID
   * @param imitNodeId イミテーションノードの ID
   */
  private void registerOrgNodeIdAndImitNodeId(BhNodeId orgNodeId, BhNodeId imitNodeId) {
    orgAndImitNodeIdSet.add(new Pair<>(orgNodeId, imitNodeId));
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


  /** ノードテンプレートが完成した時の外部処理を呼ぶ. */
  private boolean onTemplateCompleted() {
    Script cs = BhScriptManager.INSTANCE.getCompiledScript(
        BhConstants.Path.ON_NODE_TEMPLATE_COMPLETE_JS);
    ScriptableObject scriptScope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, new UserOperationCommand());
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_TEMPLATES, INSTANCE);
    try {
      ContextFactory.getGlobal().call(cx -> cs.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "eval " + BhConstants.Path.ON_NODE_TEMPLATE_COMPLETE_JS + "\n" + e);
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
