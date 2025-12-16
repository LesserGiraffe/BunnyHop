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

package net.seapanda.bunnyhop.compiler.nodecollector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.seapanda.bunnyhop.compiler.SourceSet;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import org.apache.commons.lang3.SerializationUtils;

/**
 * コンパイルの対象となる {@link BhNode} のスナップショット.
 *
 * @author K.Koike
 */
class SourceSetSnapshot implements SourceSet {

  private final Set<BhNode> rootNodes;
  /** プログラム開始時に実行されるノード. */
  private final BhNode mainEntryPoint;

  /**
   * コンストラクタ.
   *
   * @param mainEntryPointId プログラム開始時に実行されるノードの {@link InstanceId}.  (nullable)
   * @param rootNodes コンパイルの対象となる {@link BhNode} 一式のルートノード.
   */
  SourceSetSnapshot(InstanceId mainEntryPointId, Set<BhNode> rootNodes) {
    this.rootNodes = SerializationUtils.clone(new HashSet<>(rootNodes));
    Map<InstanceId, BhNode> symbolIdToNode = collectNode(rootNodes);
    mainEntryPoint = (mainEntryPointId == null) ? null : symbolIdToNode.get(mainEntryPointId);
  }

  /** {@code rootNodeList} から辿れるノードをインスタンス ID と共に集めて返す. */
  private Map<InstanceId, BhNode> collectNode(Set<BhNode> rootNodes) {
    var symbolIdToNode = new HashMap<InstanceId, BhNode>();
    var registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(
        node -> symbolIdToNode.put(node.getInstanceId(), node));
    rootNodes.forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
    return symbolIdToNode;
  }

  @Override
  public Set<BhNode> getRootNodes() {
    return new HashSet<>(rootNodes);
  }

  @Override
  public BhNode getMainEntryPoint() {
    return mainEntryPoint;
  }
}
