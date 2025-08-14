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


import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorParamSetId;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.service.LogManager;
import org.w3c.dom.Element;

/**
 * コネクタもしくはコネクタパラメータセットが定義された xml のタグが持つ属性一覧を保持するクラス.
 *
 * @author K.Koike
 */
public record ConnectorAttribute(
    ConnectorId connectorId,
    String name,
    ConnectorParamSetId paramSetId,
    BhNodeId defaultNodeId,
    DerivationId derivationId,
    BhNodeId derivativeId,
    DerivativeJointId derivativeJointId,
    Boolean fixed,
    ConnectorParamSetId imports,
    String onConnectabilityChecking) {
  
  /**
   * Connector もしくは ConnectorParamSet タグが持つ属性一覧を読んで, {@link ConnectorAttribute} を返す.
   *
   * @param node Node タグを表すオブジェクト
   */
  public static ConnectorAttribute of(Element elem) {
    var connectorId =
        ConnectorId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID));
    String name = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    var paramSetId =
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_PARAM_SET_ID));
    var defaultNodeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID));
    var derivationId =
        DerivationId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATION_ID));
    var derivativeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_ID));
    var derivativeJoint =
        DerivativeJointId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_JOINT));
    var imports = 
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMPORT));

    String fixedStr = elem.getAttribute(BhConstants.BhModelDef.ATTR_FIXED);
    if (!fixedStr.isEmpty()
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE)
        && !fixedStr.equals(BhConstants.BhModelDef.ATTR_VAL_FALSE)) {
      LogManager.logger().error(String.format(
          "The value of a '%s' attribute must be '%s' or '%s'.    '%s=%s' is ignored.\n%s",
          BhConstants.BhModelDef.ATTR_FIXED,
          BhConstants.BhModelDef.ATTR_VAL_TRUE,
          BhConstants.BhModelDef.ATTR_VAL_FALSE,
          BhConstants.BhModelDef.ATTR_FIXED,
          fixedStr,
          elem.getOwnerDocument().getBaseURI()));
    }
    Boolean fixed = switch (fixedStr) {
      case BhConstants.BhModelDef.ATTR_VAL_TRUE -> true;
      case BhConstants.BhModelDef.ATTR_VAL_FALSE -> false;
      default -> null;
    };

    String onConnectabilityChecking =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_CONNECTABILITY_CHECKING);

    return new ConnectorAttribute(
        connectorId,
        name,
        paramSetId,
        defaultNodeId,
        derivationId,
        derivativeId,
        derivativeJoint,
        fixed,
        imports,
        onConnectabilityChecking
    );
  }
}
