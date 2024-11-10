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

package net.seapanda.bunnyhop.saveandload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.syntaxsymbol.InstanceId;

/**
 * セーブされる {@link BhNode} の情報を保持する.
 *
 * @author K.Koike
 */
public class NodeImage {
  public final BhNodeId nodeId;
  public final InstanceId symbolId;
  public final List<InstanceId> derivativeInstIdList;
  public final String text;
  public final ConnectorId parentConnectorId;
  public final BhNodeVersion version;
  public final Vec2D pos;
  private NodeImage parent;
  private final List<NodeImage> children = new ArrayList<>();

  /**
   * コンストラクタ.
   * セーブの対象となる 1 つの BhNode が持つ情報をこのオブジェクトに格納する.
   *
   * @param nodeId 対象の BhNode の ID
   * @param symbolId 対象の BhNode のシンボル ID
   * @param derivativeInstIdList 対象の BhNode が持つ派生ノードのシンボル ID
   * @param text 対象の BhNode が TextNode の場合に持っているテキスト
   * @param parentConnectorId 対象の BhNode がつながれているコネクタの ID
   * @param version 対象の BhNode のバージョン
   * @param pos このノードのワークスペース上での位置
   */
  public NodeImage(
      BhNodeId nodeId,
      InstanceId symbolId,
      Collection<InstanceId> derivativeInstIdList,
      String text,
      ConnectorId parentConnectorId,
      BhNodeVersion version,
      Vec2D pos) {
    this.nodeId = nodeId;
    this.symbolId = symbolId;
    this.derivativeInstIdList = new ArrayList<>(derivativeInstIdList);
    this.text = (text == null) ? "" : text;
    this.parentConnectorId = parentConnectorId;
    this.version = version;
    this.pos = pos;
  }

  /** 子要素を追加する. */
  public void addChild(NodeImage child) {
    children.add(child);
    child.parent = this;
  }

  /** 子要素を取得する. */
  public Collection<NodeImage> getChildren() {
    return new ArrayList<>(children);
  }

  /** 親要素を取得する. */
  public Optional<NodeImage> getParent() {
    return Optional.ofNullable(parent);
  }
}
