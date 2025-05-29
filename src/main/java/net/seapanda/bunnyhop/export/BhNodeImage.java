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

import java.util.ArrayList;
import java.util.Collection;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * BhNode を保存する際の情報を保持するクラス.
 *
 * @author K.Koike
 */
public class BhNodeImage {
  final BhNodeId nodeId;
  final InstanceId instanceId;
  final ArrayList<InstanceId> derivativeIds;
  final String text;
  final boolean isDefault;
  final ConnectorId parentConnectorId;
  final BhNodeVersion version;
  final Vec2D pos;
  private final ArrayList<BhNodeImage> children = new ArrayList<>();

  /**
   * コンストラクタ.
   * 保存対象の 1 つの BhNode が持つ情報をこのオブジェクトに格納する.
   *
   * @param nodeId 対象の BhNode の ID
   * @param instanceId 対象の BhNode のインスタンス ID
   * @param derivativeIds 対象の BhNode が持つ派生ノードのインスタンス ID
   * @param text 対象の BhNode が TextNode の場合に持っているテキスト
   * @param isDefault 対象の BhNode がデフォルトノードである場合 true
   * @param parentConnectorId 対象の BhNode がつながれているコネクタの ID
   * @param version 対象の BhNode のバージョン
   * @param pos このノードのワークスペース上での位置
   */
  public BhNodeImage(
      BhNodeId nodeId,
      InstanceId instanceId,
      Collection<InstanceId> derivativeIds,
      String text,
      boolean isDefault,
      ConnectorId parentConnectorId,
      BhNodeVersion version,
      Vec2D pos) {
    this.nodeId = nodeId;
    this.instanceId = instanceId;
    this.derivativeIds = new ArrayList<>(derivativeIds);
    this.text = (text == null) ? "" : text;
    this.isDefault = isDefault;
    this.parentConnectorId = parentConnectorId;
    this.version = version;
    this.pos = pos;
  }

  /** デフォルトコンストラクタ (デシリアライズ用). */
  public BhNodeImage() {
    this.nodeId = BhNodeId.NONE;
    this.instanceId = InstanceId.NONE;
    this.derivativeIds = new ArrayList<>();
    this.text = "";
    this.isDefault = false;
    this.parentConnectorId = ConnectorId.NONE;
    this.version = BhNodeVersion.NONE;
    this.pos = new Vec2D(0, 0);
  }

  /** 子要素を追加する. */
  public void addChild(BhNodeImage child) {
    children.add(child);
  }

  /** 子要素を取得する. */
  public Collection<BhNodeImage> getChildren() {
    return new ArrayList<>(children);
  }
}
