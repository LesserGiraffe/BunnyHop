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
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
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
  final boolean isBreakpointSet;
  final boolean isCorrupted;
  final BhNodeVersion version;
  final Vec2D pos;
  private final ArrayList<ConnectorImage> children;

  /**
   * コンストラクタ.
   * 保存対象の 1 つの BhNode が持つ情報をこのオブジェクトに格納する.
   *
   * @param nodeId 対象の BhNode の ID
   * @param instanceId 対象の BhNode のインスタンス ID
   * @param derivativeIds 対象の BhNode が持つ派生ノードのインスタンス ID
   * @param text 対象の BhNode が TextNode の場合に持っているテキスト
   * @param isDefault 対象の BhNode がデフォルトノードである場合 true
   * @param isBreakpointSet 対象の BhNode にブレークポイントが設定されている場合 true
   * @param isCorrupted 対象の BhNode が破損している場合 true
   * @param version 対象の BhNode のバージョン
   * @param pos このノードのワークスペース上での位置
   */
  public BhNodeImage(
      BhNodeId nodeId,
      InstanceId instanceId,
      Collection<InstanceId> derivativeIds,
      String text,
      boolean isDefault,
      boolean isBreakpointSet,
      boolean isCorrupted,
      BhNodeVersion version,
      Vec2D pos) {
    this.nodeId = nodeId;
    this.instanceId = instanceId;
    this.derivativeIds = new ArrayList<>(derivativeIds);
    this.text = (text == null) ? "" : text;
    this.isDefault = isDefault;
    this.isBreakpointSet = isBreakpointSet;
    this.isCorrupted = isCorrupted;
    this.version = version;
    this.pos = pos;
    children = new ArrayList<>();
  }

  /** デフォルトコンストラクタ (デシリアライズ用). */
  public BhNodeImage() {
    this.nodeId = BhNodeId.NONE;
    this.instanceId = InstanceId.NONE;
    this.derivativeIds = new ArrayList<>();
    this.text = "";
    this.isDefault = false;
    this.isBreakpointSet = false;
    this.isCorrupted = false;
    this.version = BhNodeVersion.NONE;
    this.pos = new Vec2D(0, 0);
    this.children = new ArrayList<>();
  }

  /** 子要素のコネクタを追加する. */
  public void addChild(ConnectorImage child) {
    children.add(child);
  }

  /** 子要素のコネクタを取得する. */
  public Collection<ConnectorImage> getChildren() {
    return new ArrayList<>(children);
  }
}
