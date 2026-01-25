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


import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorParamSetId;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.parameter.DerivativeJointId;
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
    Boolean restoreLastDefaultNode,
    DerivationId derivationId,
    BhNodeId derivativeId,
    DerivativeJointId derivativeJointId,
    Boolean fixed,
    ConnectorParamSetId imports,
    String onConnectabilityChecking) {
  
  /**
   * Connector もしくは ConnectorParamSet タグが持つ属性一覧を読んで, {@link ConnectorAttribute} を返す.
   *
   * @param elem Connector タグを表すオブジェクト
   */
  public static ConnectorAttribute of(Element elem) {
    var connectorId =
        ConnectorId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID));
    String name = elem.getAttribute(BhConstants.BhModelDef.ATTR_NAME);
    var paramSetId =
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_PARAM_SET_ID));
    var defaultNodeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID));
    Boolean restoreLastDefaultNode =
        getBoolAttribute(BhConstants.BhModelDef.ATTR_RESTORE_LAST_DEFAULT_NODE, elem);
    var derivationId =
        DerivationId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATION_ID));
    var derivativeId =
        BhNodeId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_ID));
    var derivativeJoint =
        DerivativeJointId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_DERIVATIVE_JOINT));
    Boolean fixed = getBoolAttribute(BhConstants.BhModelDef.ATTR_FIXED, elem);
    var imports =
        ConnectorParamSetId.of(elem.getAttribute(BhConstants.BhModelDef.ATTR_IMPORT));
    String onConnectabilityChecking =
        elem.getAttribute(BhConstants.BhModelDef.ATTR_ON_CONNECTABILITY_CHECKING);

    return new ConnectorAttribute(
        connectorId,
        name,
        paramSetId,
        defaultNodeId,
        restoreLastDefaultNode,
        derivationId,
        derivativeId,
        derivativeJoint,
        fixed,
        imports,
        onConnectabilityChecking
    );
  }

  /** 真偽値を取るアトリビュートの値を取得する. */
  private static Boolean getBoolAttribute(String attrName, Element elem) {
    String valStr = elem.getAttribute(attrName);
    if (!valStr.isEmpty()
        && !valStr.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE)
        && !valStr.equals(BhConstants.BhModelDef.ATTR_VAL_FALSE)) {
      logBoolAttributeError(BhConstants.BhModelDef.ATTR_FIXED, valStr, elem);
    }
    return switch (valStr) {
      case BhConstants.BhModelDef.ATTR_VAL_TRUE -> true;
      case BhConstants.BhModelDef.ATTR_VAL_FALSE -> false;
      default -> null;
    };
  }

  private static void logBoolAttributeError(String attrName, String attrValue, Element elem) {
    LogManager.logger().error(String.format(
        "The value of a '%s' attribute must be '%s' or '%s'.    '%s=%s' is ignored.\n%s",
        attrName,
        BhConstants.BhModelDef.ATTR_VAL_TRUE,
        BhConstants.BhModelDef.ATTR_VAL_FALSE,
        attrName,
        attrValue,
        elem.getOwnerDocument().getBaseURI()));
  }
}
