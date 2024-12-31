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

package net.seapanda.bunnyhop.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import org.apache.commons.lang3.SerializationUtils;

/**
 * 全 {@link BhNode} の構造を保存するスナップショット.
 *
 * @author K.Koike
 */
public class NodeGraphSnapshot {
  /** 全ルートノードを格納する. */
  private final Set<BhNode> rootNodeSet;
  private final WorkspaceSet wss;
  /** {@link InstanceId} とそれに対応する BhNode のマップ. */
  private final Map<InstanceId, BhNode> symbolIdToNode;

  /**
   * 引数で指定したワークスペースセットにある全ノードの構造を保持するスナップショットを構築する.
   *
   * @param wss 保存するノードを含むワークスペースセット
   */
  public NodeGraphSnapshot(WorkspaceSet wss) {
    this.wss = wss;
    HashSet<BhNode> originalRootNodeSet = collectRootNodes(wss);
    rootNodeSet = SerializationUtils.clone(originalRootNodeSet);
    symbolIdToNode = collectNode(rootNodeSet);
  }

  /** ルートノードを集めて返す. */
  private HashSet<BhNode> collectRootNodes(WorkspaceSet wss) {
    var rootNodes = wss.getWorkspaces().stream()
        .flatMap(ws -> ws.getRootNodes().stream())
        .collect(Collectors.toSet());
    return new HashSet<BhNode>(rootNodes);
  }

  /** {@link rootNodeList} から辿れるノードをインスタンス ID と共に集めて返す. */
  private Map<InstanceId, BhNode> collectNode(Collection<BhNode> rootNodeList) {
    var symbolIdToNode = new HashMap<InstanceId, BhNode>();
    var registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(
        node -> symbolIdToNode.put(node.getInstanceId(), node));
    rootNodeList.forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
    return symbolIdToNode;
  }

  /**
   * スナップショットを取ったノードが存在したワークスペースセットを返す.
   *
   * @return スナップショットを取ったノードが存在したワークスペースセット
   */
  public WorkspaceSet getWorkspaceSet() {
    return wss;
  }

  /**
   * 全ルートノードのスナップショットを返す.
   *
   * @return 全ルートノードのスナップショット
   */
  public Collection<BhNode> getRootNodeList() {
    return new ArrayList<>(rootNodeSet);
  }

  /**
   * インスタンス ID とそれに対応するノード (スナップショット) のマップを返す.
   *
   * @return インスタンス ID とそれに対応するノード (スナップショット) のマップ
   */
  public Map<InstanceId, BhNode> getMapOfSymbolIdToNode() {
    return new HashMap<>(symbolIdToNode);
  }
}
