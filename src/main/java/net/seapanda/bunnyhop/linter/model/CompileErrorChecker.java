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

package net.seapanda.bunnyhop.linter.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * ワークスペースセットのノードのコンパイルエラーを調べる機能を提供するクラス.
 *
 * @author K.Koike
 */
public class CompileErrorChecker {

  /** これらのノードから親子関係または派生関係でたどれるノード群を, コンパイルエラーが発生しているか調べる対象とする. */
  private final Set<BhNode> startingPoints = new HashSet<>();

  /**
   * コンストラクタ.
   *
   * @param wss このワークスペースセットにあるノードのコンパイルエラーを調べる.
   */
  public CompileErrorChecker(WorkspaceSet wss) {
    WorkspaceSet.CallbackRegistry registry = wss.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> startingPoints.add(event.node()));
    registry.getOnNodeRemoved().add(event -> startingPoints.add(event.node()));
    registry.getOnRootNodeAdded().add(event -> startingPoints.add(event.node()));
    registry.getOnRootNodeRemoved().add(event -> startingPoints.add(event.node()));
    registry.getOnOriginalNodeChanged().add(event -> {
      startingPoints.add(event.node());
      Optional.ofNullable(event.newOriginal()).ifPresent(startingPoints::add);
      Optional.ofNullable(event.oldOriginal()).ifPresent(startingPoints::add);
    });
  }

  /**
   * ワークスペースセットのノードのコンパイルエラーの状態を更新する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void check(UserOperation userOpe) {
    Set<BhNode> targetNodes = collectTargetNodes();
    targetNodes.forEach(node -> node.checkCompileError(userOpe));
    startingPoints.clear();
  }

  /** {@link #startingPoints} のノードから親子関係または派生関係で推移的に辿れるノードを取得する. */
  private Set<BhNode> collectTargetNodes() {
    var liveStartingPoints = startingPoints.stream().filter(node -> !node.isDeleted()).toList();
    Set<BhNode> rootNodes = collectRootNodes(liveStartingPoints);
    var descendants = new HashSet<BhNode>();
    rootNodes.forEach(node -> collectDescendants(node, descendants));

    Set<BhNode> rootOriginals = collectRootOriginals(descendants);
    var derivatives = new HashSet<BhNode>();
    rootOriginals.forEach(node -> collectDerivatives(node, derivatives));
    
    descendants.addAll(derivatives);
    return descendants;
  }

  /** {@code nodes} のルートノードを全て集める. */
  private static Set<BhNode> collectRootNodes(Collection<BhNode> nodes) {
    var rootNodes = new HashSet<BhNode>();
    var targets = new LinkedHashSet<>(nodes);
    while (!targets.isEmpty()) {
      BhNode target = targets.removeFirst();
      if (!target.isDeleted()) {
        BhNode root = findRootNode(target, targets);
        rootNodes.add(root);
      }
    }
    return rootNodes;
  }

  /** {@code node} のルートノードを探し, 途中のノードを {@code collection} から除外する. */
  private static BhNode findRootNode(BhNode node, Set<BhNode> collection) {
    while (!node.isRoot()) {
      node = node.findParentNode();
      collection.remove(node); // 余分な走査を省略する
    }
    return node;
  }

  /** {@code node} とその子孫ノードを {@code collection} に格納する. */
  private static void collectDescendants(BhNode node, Set<BhNode> collection) {
    if (node == null || node.isDeleted()) {
      return;
    }
    var callBack = CallbackInvoker.newCallbackRegistry().setForAllNodes(collection::add);
    CallbackInvoker.invoke(callBack, node);
  }

  /** {@code nodes} から推移的に辿れる最後のオリジナルノードを全て集める. */
  private static Set<BhNode> collectRootOriginals(Collection<BhNode> nodes) {
    var rootOriginals = new HashSet<BhNode>();
    var targets = new LinkedHashSet<>(nodes);
    while (!targets.isEmpty()) {
      BhNode target = targets.removeFirst();
      if (!target.isDeleted()) {
        BhNode node = findRootOriginal(target, targets);
        rootOriginals.add(node);
      }
    }
    return rootOriginals;
  }

  /** {@code nodes} から推移的に辿れる最後のオリジナルノードを探し, 途中のノードを {@code collection} から除外する. */
  private static BhNode findRootOriginal(BhNode nodes, Set<BhNode> collection) {
    while (nodes.isDerivative()) {
      nodes = nodes.getOriginal();
      collection.remove(nodes); // 余分な走査を省略する
    }
    return nodes;
  }

  /** {@code node} と {@code node} から推移的に辿れる派生ノードを {@code collection} に格納する. */
  private static void collectDerivatives(BhNode node, Set<BhNode> collection) {
    if (node == null || node.isDeleted()) {
      return;
    }
    collection.add(node);
    node.getDerivatives().forEach(derv -> collectDerivatives(derv, collection));
  }
}
