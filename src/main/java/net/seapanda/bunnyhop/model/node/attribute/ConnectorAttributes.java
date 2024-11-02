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

package net.seapanda.bunnyhop.model.node.attribute;


import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import org.w3c.dom.Element;

/**
 * コネクタもしくはコネクタパラメータセットが定義された xml のタグが持つ属性一覧を保持するクラス.
 *
 * @author K.Koike
 */
public record ConnectorAttributes(
    ConnectorId connectorId,
    String name,
    ConnectorParamSetId paramSetId,
    BhNodeId defaultNodeId,
    ImitationId imitId,
    BhNodeId imitNodeId,
    ImitCnctPosId imitCnctPosId,
    Boolean fixed,
    ConnectorParamSetId imports,
    String onConnectabilityChecking) {
  
  /**
   * Connector もしくは ConnectorParamSet タグが持つ属性一覧を読んで, {@link ConnectorAttributes} を返す.
   *
   * @param node Node タグを表すオブジェクト
   */
  public static ConnectorAttributes of(Element elem) {
    var connectorId =
        ConnectorId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID));
    String name = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    var paramSetId =
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_PARAM_SET_ID));
    var defaultNodeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID));
    var imitId =
        ImitationId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMITATION_ID));
    var imitNodeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMITATION_NODE_ID));
    var imitCnctPosId =
        ImitCnctPosId.create(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMIT_CNCT_POS));
    var imports = 
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMPORT));

    String fixedStr = elem.getAttribute(BhConstants.BhModelDef.ATTR_FIXED);
    if (!fixedStr.isEmpty()
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE)
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_FALSE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "The value of a '" + BhConstants.BhModelDef.ATTR_FIXED + "' attribute "
          + "must be '" + BhConstants.BhModelDef.ATTR_VAL_TRUE + "' or '"
          + BhConstants.BhModelDef.ATTR_VAL_FALSE + "'.    "
          + "'" + BhConstants.BhModelDef.ATTR_FIXED + "=" + fixedStr + "' is ignored.\n"
          + elem.getOwnerDocument().getBaseURI());
    }
    Boolean fixed = switch (fixedStr) {
      case BhConstants.BhModelDef.ATTR_VAL_TRUE -> true;
      case BhConstants.BhModelDef.ATTR_VAL_FALSE -> false;
      default -> null;
    };

    String onConnectabilityChecking =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_CONNECTABILITY_CHECKING);

    return new ConnectorAttributes(
        connectorId,
        name,
        paramSetId,
        defaultNodeId,
        imitId,
        imitNodeId,
        imitCnctPosId,
        fixed,
        imports,
        onConnectabilityChecking
    );
  }
}
