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

package net.seapanda.bunnyhop.compile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.seapanda.bunnyhop.compiler.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.SourceSet;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxtree.NodeInstanceId;
import net.seapanda.bunnyhop.node.model.syntaxtree.SyntaxTreeNode;

/**
 * コンパイルの対象となる {@link BhNode} のスナップショット.
 *
 * @author K.Koike
 */
class SourceSetSnapshot implements SourceSet {

  /** コンパイルの対象となるノード群のうち, ルートノードであるものの一覧. */
  private final Set<CompilableSyntaxTreeNode> rootNodes = new HashSet<>();

  /** エントリポイントとして選択されたノード.  存在しない場合は null. */
  private final CompilableSyntaxTreeNode selectedEntryPoint;

  /** コンパイルの対象となるノード群のインスタンス ID とそのノードのマップ. */
  private final Map<String, CompilableSyntaxTreeNode> instanceIdToNode = new HashMap<>();

  /**
   * コンストラクタ.
   *
   * @param selectedEntryPointId エントリポイントとして選択されたノードの {@link NodeInstanceId}.  (nullable)
   * @param rootNodes コンパイルの対象となる {@link BhNode} 一式のルートノード.
   */
  SourceSetSnapshot(NodeInstanceId selectedEntryPointId, Set<BhNode> rootNodes) {
    Map<SyntaxTreeNode, CompilableSyntaxTreeNodeImpl> built = new IdentityHashMap<>();
    for (BhNode root : rootNodes) {
      this.rootNodes.add(buildNode(root, built));
    }
    selectedEntryPoint = (selectedEntryPointId == null)
        ? null : instanceIdToNode.get(selectedEntryPointId.toString());
  }

  /**
   * {@code node} に対応する {@link CompilableSyntaxTreeNodeImpl} を作成する.
   *
   * <p>{@code node} の子要素と, {@code node} が {@link BhNode} であった場合の派生元ノードを再帰的にたどり,
   * 同じ構造を持つ {@link CompilableSyntaxTreeNodeImpl} のグラフを構築する.
   *
   * @param node 対応する {@link CompilableSyntaxTreeNodeImpl} を作成する {@link SyntaxTreeNode}
   * @param built 作成済みの {@link SyntaxTreeNode} と {@link CompilableSyntaxTreeNodeImpl} のマップ
   * @return {@code node} に対応する {@link CompilableSyntaxTreeNodeImpl}
   */
  private CompilableSyntaxTreeNodeImpl buildNode(
      SyntaxTreeNode node, Map<SyntaxTreeNode, CompilableSyntaxTreeNodeImpl> built) {
    CompilableSyntaxTreeNodeImpl existing = built.get(node);
    if (existing != null) {
      return existing;
    }
    CompilableSyntaxTreeNodeImpl impl = (node instanceof BhNode bhNode)
        ? new CompilableSyntaxTreeNodeImpl(bhNode)
        : new CompilableSyntaxTreeNodeImpl(node);
    built.put(node, impl);
    instanceIdToNode.put(impl.getInstanceId(), impl);

    for (SyntaxTreeNode child : node.getChildren()) {
      CompilableSyntaxTreeNodeImpl childImpl = buildNode(child, built);
      childImpl.setParent(impl);
      impl.addChild(childImpl);
    }
    if (node instanceof BhNode bhNode && bhNode.getOriginal() != null) {
      impl.setOriginal(buildNode(bhNode.getOriginal(), built));
    }
    return impl;
  }


  @Override
  public Set<CompilableSyntaxTreeNode> getRootNodes() {
    return rootNodes;
  }

  @Override
  public CompilableSyntaxTreeNode getSelectedEntryPoint() {
    return selectedEntryPoint;
  }

  @Override
  public Map<String, CompilableSyntaxTreeNode> getInstanceIdToNode() {
    return instanceIdToNode;
  }
}
