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


package net.seapanda.bunnyhop.bhprogram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import org.apache.commons.lang3.SerializationUtils;

/**
 * 実行可能な {@link BhNode} の構造を保存するスナップショット.
 *
 * @author K.Koike
 */
public class ExecutableNodeSnapshot implements ExecutableNodeSet {
  
  /** 全ルートノードを格納する. */
  private final Set<BhNode> rootNodeSet;
  /** {@link InstanceId} とそれに対応する BhNode のマップ. */
  private final Map<InstanceId, BhNode> symbolIdToNode;
  /** プログラム開始時に実行されるノード. */
  private final BhNode entryPoint;

  /** {@code wss} にある全ての実行可能なノードの構造を保持するスナップショットを構築する. */
  public ExecutableNodeSnapshot(WorkspaceSet wss, BhNode entryPoint) {
    HashSet<BhNode> originalRootNodeSet = collectRootNodes(wss);
    rootNodeSet = SerializationUtils.clone(originalRootNodeSet);
    symbolIdToNode = collectNode(rootNodeSet);
    if (symbolIdToNode.containsKey(entryPoint.getInstanceId())) {
      this.entryPoint = symbolIdToNode.get(entryPoint.getInstanceId());  
    } else {
      this.entryPoint = SerializationUtils.clone(entryPoint);
    }
  }

  /** ルートノードを集めて返す. */
  private HashSet<BhNode> collectRootNodes(WorkspaceSet wss) {
    return wss.getWorkspaces().stream()
        .flatMap(ws -> ws.getRootNodes().stream())
        .collect(Collectors.toCollection(HashSet::new));
  }

  /** {@code rootNodeList} から辿れるノードをインスタンス ID と共に集めて返す. */
  private Map<InstanceId, BhNode> collectNode(Collection<BhNode> rootNodeList) {
    var symbolIdToNode = new HashMap<InstanceId, BhNode>();
    var registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(
        node -> symbolIdToNode.put(node.getInstanceId(), node));
    rootNodeList.forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
    return symbolIdToNode;
  }

  @Override
  public Collection<BhNode> getRootNodeList() {
    return new ArrayList<>(rootNodeSet);
  }

  @Override
  public Map<InstanceId, BhNode> getMapOfSymbolIdToNode() {
    return new HashMap<>(symbolIdToNode);
  }

  @Override
  public BhNode getEntryPoint() {
    return entryPoint;
  }
}
