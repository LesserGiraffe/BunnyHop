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

import java.util.Optional;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.factory.XmlBhNodeRepository.ModelArchive;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorParamSetId;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.script.BhScriptRepository;
import org.w3c.dom.Element;

/**
  * {@link Connector} が定義された xml の Connector エレメント以下の情報から {@link Connector} を作成する.
  *
  * @author K.Koike
  */
class ConnectorBuilder {
  
  private final ModelArchive archive;
  private final BhScriptRepository repository;
  private final ModelGenerator generator;

  /**
   * コンストラクタ.
   *
   * @param repository {@link Connector} の定義ファイルに書かれた外部スクリプトを保持するオブジェクト
   */
  ConnectorBuilder(
      ModelArchive archive, BhScriptRepository repository, ModelGenerator generator) {
    this.archive = archive;
    this.repository = repository;
    this.generator = generator;
  }

  /**
   * コネクタを作成する.
   *
   * @return 作成したコネクタオブジェクト
   */
  public Optional<Connector> build(Element elem) {
    // ルートエレメントチェック
    if (!elem.getNodeName().equals(BhConstants.BhModelDef.ELEM_CONNECTOR)) {
      LogManager.logger().error(String.format("""
          Invalid connector definition. (%s)
          A connector definition must have a '%s' root element.\n%s
          """,
          elem.getNodeName(),
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          elem.getBaseURI()));
      return Optional.empty();
    }

    var cnctrAttrs = ConnectorAttribute.of(elem);
    // コネクタ ID 存在チェック
    if (cnctrAttrs.connectorId().equals(ConnectorId.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ATTR_BH_CONNECTOR_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }
    // スクリプト存在チェック
    boolean allScriptsFound = repository.allExistIgnoringEmptyWithHandler(
        scriptName -> outputScriptNotFoundMsg(scriptName, elem.getBaseURI()),
        cnctrAttrs.onConnectabilityChecking());
    if (!allScriptsFound) {
      return Optional.empty();
    }
    ConnectorAttribute imported =
        archive.getConnectorAttribute(cnctrAttrs.imports()).orElse(cnctrAttrs);
    // デフォルトノードの存在チェック.
    if (imported.defaultNodeId().equals(BhNodeId.NONE)
        && cnctrAttrs.defaultNodeId().equals(BhNodeId.NONE)) {
      LogManager.logger().error(String.format(
          "A '%s' elements must have a '%s' attribute.\n%s",
          BhConstants.BhModelDef.ELEM_CONNECTOR,
          BhConstants.BhModelDef.ATTR_DEFAULT_BHNODE_ID,
          elem.getBaseURI()));
      return Optional.empty();
    }
    return Optional.of(generator.newConnector(buildConnectorParams(imported, cnctrAttrs)));
  }

  /**
   * コネクタパラメータセットの定義に指定されたパラメータとコネクタの定義に指定されたパラメータを合わせて, 新しくコネクタパラメータを作成する.
   *
   * @param imported コネクタパラメータセットの定義に指定されたパラメータ
   * @param defined コネクタの定義に指定されたパラメータ
   * @return {@code imported} と {@code defined} を合わせたパラメータ
   */
  private ConnectorAttribute buildConnectorParams(
      ConnectorAttribute imported, ConnectorAttribute defined) {
    var name = defined.name().isEmpty() ? imported.name() : defined.name();
    var defaultNodeId = defined.defaultNodeId().equals(BhNodeId.NONE)
        ? imported.defaultNodeId() : defined.defaultNodeId();
    var derivationId = defined.derivationId().equals(DerivationId.NONE)
        ? imported.derivationId() : defined.derivationId();
    var derivativeId = defined.derivativeId().equals(BhNodeId.NONE)
        ? imported.derivativeId() : defined.derivativeId();
    var derivativeJoint = defined.derivativeJointId().equals(DerivativeJointId.NONE)
        ? imported.derivativeJointId() : defined.derivativeJointId();
    boolean fixed = false;
    if (defined.fixed() != null) {
      fixed = defined.fixed();
    } else if (imported.fixed() != null) {
      fixed = imported.fixed();
    }
    var onConnectabilityChecking = defined.onConnectabilityChecking().isEmpty()
        ? imported.onConnectabilityChecking() : defined.onConnectabilityChecking();
    
    return new ConnectorAttribute(
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

  /**
   * 外部スクリプトが見つからなかったときのエラーメッセージを出力する.
   *
   * @param scriptName 見つからなかった外部スクリプト名
   * @param fileName {@code scriptName} が書いてあったファイルの名前
   */
  private static void outputScriptNotFoundMsg(String scriptName, String fileName) {
    LogManager.logger().error(
        "Cannot find '%s'.  file: %s".formatted(scriptName, fileName));
  }
}
  