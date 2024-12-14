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

import java.util.Optional;
import java.util.function.Function;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorAttributes;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorParamSetId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.attribute.DerivativeJointId;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.BhService;
import org.w3c.dom.Element;

/**
 * {@link Connector} が定義された xml の Connector エレメント以下の情報から {@link Connector} を作成する.
 *
 * @author K.Koike
 */
public class ConnectorConstructor {
  
  private final Element elem;
  /** コネクタアトリビュート取得用関数. */
  private final Function<ConnectorParamSetId, Optional<ConnectorAttributes>> fnGetCnctrAttr;
  /** {@link Connector} のパラメータの定義ファイルに書かれた JavaScript ファイルを保持する {@link BhScriptManager} オブジェクト. */
  private final BhScriptManager scriptManager;

  /**
   * コンストラクタ.
   *
   * @param elem ConnectorParameterSet もしくは Connector エレメント.
   * @param fnGetCnctrAttr コネクタアトリビュート取得用関数
   * @param scriptManager {@link Connector} のパラメータの定義ファイルに書かれた
   *                      JavaScript ファイルを保持する {@link BhScriptManager} オブジェクト.
   */
  ConnectorConstructor(
      Element elem,
      Function<ConnectorParamSetId, Optional<ConnectorAttributes>> fnGetCnctrAttr,
      BhScriptManager scriptManager) {
    this.elem = elem;
    this.fnGetCnctrAttr = fnGetCnctrAttr;
    this.scriptManager = scriptManager;
  }

  /** このオブジェクトが持つエレメントがコネクタパラメータセットであるか調べる. */
  public boolean isParamSet() {
    return elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET);
  }

  /** このオブジェクトが持つエレメントがコネクタであるか調べる. */
  public boolean isConnector() {
    return elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR);
  }

  /** コネクタパラメータセットを作成する. */
  public Optional<ConnectorAttributes> genParamSet() {
    //ルートエレメントチェック
    if (!elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET)) {
      BhService.msgPrinter().errForDebug(String.format("""
          Invalid connector parameter set definition (%s).
          A connector parameter set definition must have a '%s' root element.\n%s
          """,
          elem.getNodeName(),
          BhConstants.BhModelDef.ELEM_CONNECTOR_PARAM_SET,
          elem.getBaseURI()));
      return Optional.empty();
    }
    return Optional.of(ConnectorAttributes.of(elem));
  }

  /**
   * コネクタのを作成する.
   *
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> genConnector() {
    // ルートエレメントチェック
    if (!elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR)) {
      BhService.msgPrinter().errForDebug(String.format("""
          Invalid connector definition. (%s)
          A connector definition must have a '%s' root element.\n%s
          """,
          elem.getNodeName(),
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          elem.getBaseURI()));
      return Optional.empty();
    }

    var cnctrAttrs = ConnectorAttributes.of(elem);
    // コネクタ ID 存在チェック
    if (cnctrAttrs.connectorId().equals(ConnectorId.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }
    // スクリプト存在チェック
    if (!scriptManager.allExistIgnoringEmpty(
        elem.getBaseURI(), cnctrAttrs.onConnectabilityChecking())) {
      return Optional.empty();
    }
    ConnectorAttributes imported = fnGetCnctrAttr.apply(cnctrAttrs.imports()).orElse(cnctrAttrs);
    // デフォルトノードの存在チェック.
    if (imported.defaultNodeId().equals(BhNodeId.NONE)
        && cnctrAttrs.defaultNodeId().equals(BhNodeId.NONE)) {
      BhService.msgPrinter().errForDebug(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }

    return Optional.of(new Connector(buildConnectorParams(imported, cnctrAttrs)));
  }

  /**
   * コネクタパラメータセットの定義に指定されたパラメータとコネクタの定義に指定されたパラメータを合わせて, 新しくコネクタパラメータを作成する.
   *
   * @param imported コネクタパラメータセットの定義に指定されたパラメータ
   * @param defined コネクタの定義に指定されたパラメータ
   * @return {@code imported} と {@code defined} を合わせたパラメータ
   */
  private ConnectorAttributes buildConnectorParams(
      ConnectorAttributes imported, ConnectorAttributes defined) {
    var name = defined.name().isEmpty() ? imported.name() : defined.name();
    var defaultNodeId = defined.defaultNodeId().equals(BhNodeId.NONE)
        ? imported.defaultNodeId() : defined.defaultNodeId();
    var derivationId = defined.derivationId().equals(DerivationId.NONE)
        ? imported.derivationId() : defined.derivationId();
    var derivativeId = defined.derivativeId().equals(BhNodeId.NONE)
        ? imported.derivativeId() : defined.derivativeId();
    var derivativeJoint = defined.derivativeJoint().equals(DerivativeJointId.NONE)
        ? imported.derivativeJoint() : defined.derivativeJoint();
    boolean fixed = false;
    if (defined.fixed() != null) {
      fixed = defined.fixed();
    } else if (imported.fixed() != null) {
      fixed = imported.fixed();
    }
    var onConnectabilityChecking = defined.onConnectabilityChecking().isEmpty()
        ? imported.onConnectabilityChecking() : defined.onConnectabilityChecking();
    
    return new ConnectorAttributes(
        defined.connectorId(),
        name,
        ConnectorParamSetId.NONE,
        defaultNodeId,
        derivationId,
        derivativeId,
        derivativeJoint,
        fixed,
        ConnectorParamSetId.NONE,
        onConnectabilityChecking);
  }

  /** コネクタで定義されるスクリプト名のセット. */
  record Scripts(String cnctCheck, String selDefaultNodeScript) { }
}
