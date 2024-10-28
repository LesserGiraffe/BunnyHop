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

import java.util.Optional;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * {@link Connector} が定義された xml の Connector タグ以下の情報から {@link Connector} を作成する.
 *
 * @author K.Koike
 */
public class ConnectorConstructor {

  /** コンストラクタ. */
  public ConnectorConstructor() {}

  /**
   * コネクタテンプレートを作成する.
   *
   * @param doc テンプレートを作成するxml の Document オブジェクト
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> genTemplate(Document doc) {
    //ルートタグチェック
    Element root = doc.getDocumentElement();
    if (!root.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "コネクタ定義のルート要素は " + BhConstants.BhModelDef.ELEM_CONNECTOR 
          + " で始めてください.  " + doc.getBaseURI());
      return Optional.empty();
    }
    return genTemplate(root);
  }

  /**
   * コネクタテンプレートを作成する.
   *
   * @param cnctrRoot Connector タグの要素.
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> genTemplate(Element cnctrRoot) {
    //コネクタID
    ConnectorId cnctrId = ConnectorId.createCnctrId(
        cnctrRoot.getAttribute(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID));
    if (cnctrId.equals(ConnectorId.NONE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + ">" + " タグには "
          + BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID + " 属性を付加してください.  "
          + cnctrRoot.getBaseURI());
      return Optional.empty();
    }

    // Fixed
    String fixedStr = cnctrRoot.getAttribute(BhConstants.BhModelDef.ATTR_FIXED);
    if (!fixedStr.isEmpty()
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE)
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_FALSE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + ">" + " タグの "
          + BhConstants.BhModelDef.ATTR_FIXED + " 属性は, " + cnctrRoot.getBaseURI()
          + BhConstants.BhModelDef.ATTR_VAL_TRUE + "か" + BhConstants.BhModelDef.ATTR_VAL_FALSE 
          + "で無ければなりません.  " + cnctrRoot.getBaseURI());
      return Optional.empty();
    }
    // スクリプト名
    String cnctCheckScriptName = getScriptName(cnctrRoot);
    if (cnctCheckScriptName == null) {
      return Optional.empty();
    }
    // 固定ノードフラグ
    boolean fixed = fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE);
    // デフォルトノードID
    BhNodeId defNodeId = BhNodeId.create(
        cnctrRoot.getAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID));
    // デフォルトノードの指定がない
    if (defNodeId.equals(BhNodeId.NONE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_CONNECTOR + "> および "
          + "<" + BhConstants.BhModelDef.ELEM_PRIVATE_CONNECTOR + "> タグは"
          + BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID + " 属性か, "
          + BhConstants.BhModelDef.ATTR_ON_DEFAULT_BH_NODE_SELECTING + " 属性を持たなければなりません.  "
          + cnctrRoot.getBaseURI());
      return Optional.empty();
    }

    return Optional.of(
        new Connector(
            cnctrId,
            defNodeId,
            fixed,
            cnctCheckScriptName));
  }

  /** スクリプト名を取得. */
  private String getScriptName(Element cnctrRoot) {
    // ノードを入れ替え可能かチェックするスクリプト.
    String cnctCheckScriptName =
        cnctrRoot.getAttribute(BhConstants.BhModelDef.ATTR_ON_CONNECTABILITY_CHECKING);
    if (!BhNodeTemplates.allScriptsExist(cnctrRoot.getBaseURI(), cnctCheckScriptName)) {
      return null;
    }
    return cnctCheckScriptName;
  }

  /** コネクタで定義されるスクリプト名のセット. */
  record Scripts(String cnctCheck, String selDefaultNodeScript) { }
}
