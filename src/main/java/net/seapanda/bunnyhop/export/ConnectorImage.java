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

package net.seapanda.bunnyhop.export;

import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;

/**
 * コネクタを保存する際の情報を保持するクラス.
 *
 * @author K.Koike
 */
public class ConnectorImage {

  final ConnectorId connectorId;
  final InstanceId instanceId; // 保存はするがロード時に使用しない.
  final BhNodeId defaultNodeId;
  final BhNodeImage connectedNode;

  /**
   * コンストラクタ.
   * 保存対象の 1 つのコネクタが持つ情報をこのオブジェクトに格納する.
   *
   * @param connectorId 対象のコネクタの ID
   * @param instanceId 対象のコネクタのインスタンス ID
   * @param defaultNodeId 対象のコネクタのデフォルトノードの ID
   * @param connectedNode 対象のコネクタに接続されている BhNode の保存イメージ
   */
  ConnectorImage(
      InstanceId instanceId,
      ConnectorId connectorId,
      BhNodeImage connectedNode,
      BhNodeId defaultNodeId) {
    this.instanceId = instanceId;
    this.connectorId = connectorId;
    this.defaultNodeId = defaultNodeId;
    this.connectedNode = connectedNode;
  }

  /** デフォルトコンストラクタ (デシリアライズ用). */
  public ConnectorImage() {
    this.instanceId = InstanceId.NONE;
    this.connectorId = ConnectorId.NONE;
    this.defaultNodeId = BhNodeId.NONE;
    this.connectedNode = new BhNodeImage();
  }
}
