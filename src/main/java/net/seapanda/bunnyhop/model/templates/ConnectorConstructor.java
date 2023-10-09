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

import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeID;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorID;

/**
 * \<Conncetor\> タグからコネクタを作成するクラス
 * @author K.Koike
 */
public class ConnectorConstructor {

  public ConnectorConstructor(){}

  /**
   * コネクタテンプレートを作成する
   * @param doc テンプレートを作成するxml の Document オブジェクト
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> genTemplate(Document doc) {

    //ルートタグチェック
    Element root = doc.getDocumentElement();
    if (!root.getNodeName().equals(BhParams.BhModelDef.ELEM_CONNECTOR)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
        "コネクタ定義のルート要素は " + BhParams.BhModelDef.ELEM_CONNECTOR 
        + " で始めてください.  " + doc.getBaseURI());
      return Optional.empty();
    }
    return genTemplate(root);
  }

  /**
   * コネクタテンプレートを作成する
   * @param cnctrRoot \<Connector\> タグの要素
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> genTemplate(Element cnctrRoot) {

    //コネクタID
    ConnectorID cnctrID = ConnectorID.createCnctrID(
      cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_BH_CONNECTOR_ID));
    if (cnctrID.equals(ConnectorID.NONE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
        "<" + BhParams.BhModelDef.ELEM_CONNECTOR + ">" + " タグには "
        + BhParams.BhModelDef.ATTR_BH_CONNECTOR_ID + " 属性を付加してください.  " + cnctrRoot.getBaseURI());
      return Optional.empty();
    }

    //Fixed
    String fixedStr = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_FIXED);
    if (!fixedStr.isEmpty() && 
      !fixedStr.equals(BhParams.BhModelDef.ATTR_VAL_TRUE) && 
      !fixedStr.equals(BhParams.BhModelDef.ATTR_VAL_FALSE)) {
      MsgPrinter.INSTANCE.errMsgForDebug("<" + BhParams.BhModelDef.ELEM_CONNECTOR + ">" + " タグの "
        + BhParams.BhModelDef.ATTR_FIXED + " 属性は, " + cnctrRoot.getBaseURI()
        + BhParams.BhModelDef.ATTR_VAL_TRUE + "か" + BhParams.BhModelDef.ATTR_VAL_FALSE 
        + "で無ければなりません.  " + cnctrRoot.getBaseURI());
      return Optional.empty();
    }
    boolean fixed = fixedStr.equals(BhParams.BhModelDef.ATTR_VAL_TRUE);

    //初期接続ノードID
    BhNodeID initNodeID = BhNodeID.create(
      cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID));
    //デフォルトノードID
    BhNodeID defNodeID = BhNodeID.create(
      cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_DEFAULT_BHNODE_ID));
    boolean hasFixedInitNode = fixed && !initNodeID.equals(BhNodeID.NONE);
    //初期ノードが固定ノードである => 初期ノードがデフォルトノードとなる
    if (hasFixedInitNode) {
      defNodeID = initNodeID;
    }
    else if (defNodeID.equals(BhNodeID.NONE)) {  //初期ノードが固定ノードではないのに, デフォルトノードの指定がない
      MsgPrinter.INSTANCE.errMsgForDebug(
        "固定初期ノードを持たない "
        + "<" + BhParams.BhModelDef.ELEM_CONNECTOR + "> および "
        + "<" + BhParams.BhModelDef.ELEM_PRIVATE_CONNECTOR + "> タグは"
        + BhParams.BhModelDef.ATTR_DEFAULT_BHNODE_ID + " 属性を持たなければなりません.  "
        + cnctrRoot.getBaseURI());
      return Optional.empty();
    }

    //コネクタクラス
    String cnctrClass = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_CLASS);
    //ノード入れ替え時の実行スクリプト
    String scriptName =
      cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_ON_CONNECTABILITY_CHECKING);
    if (!BhNodeTemplates.allScriptsExist(cnctrRoot.getBaseURI(), scriptName))
      return Optional.empty();

    return Optional.of(
      new Connector(cnctrID, defNodeID, initNodeID, cnctrClass, fixed, scriptName));
  }
}
