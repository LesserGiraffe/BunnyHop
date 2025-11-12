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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.derivative.Derivative;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
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
  }

  /**
   * ワークスペースセットのノードのコンパイルエラーの状態を更新する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void check(UserOperation userOpe) {
    Set<BhNode> targetNodes = collectTargetNodes();
    for (BhNode node : targetNodes) {
      node.checkCompileError(userOpe);
    }
    startingPoints.clear();
  }

  /** {@link #startingPoints} から親子関係または派生関係でたどれるノード群を取得する. */
  private Set<BhNode> collectTargetNodes() {
    SequencedSet<BhNode> collection = new LinkedHashSet<>();
    for (BhNode node : startingPoints) {
      search(node, collection);
    }
    return collection;
  }

  /** {@code node} から親子関係または派生関係でたどれるノードを探査して, {@code collection} に格納する. */
  private void search(BhNode node, SequencedSet<BhNode> collection) {
    if (node == null || collection.contains(node)) {
      return;
    }
    collection.add(node);
    getChildNodes(node).forEach(child -> search(child, collection));
    search(getParentNode(node), collection);
    search(node.getOriginal(), collection);
    if (node instanceof Derivative derivative) {
      derivative.getDerivatives().forEach(derv -> search(derv, collection));
    }
  }

  /** {@code node} の親ノードを取得する. */
  private BhNode getParentNode(BhNode node) {
    Connector cnctr = node.getParentConnector();
    return (cnctr == null) ? null : cnctr.getParentNode();
  }

  /** {@code parent} の子ノードを取得する. */
  private Set<BhNode> getChildNodes(BhNode parent) {
    var children = new LinkedHashSet<BhNode>();
    var walker = new BhNodeWalker() {
      @Override
      public void visit(ConnectiveNode node) {
        if (node == parent) {
          node.sendToSections(this);
        } else {
          children.add(node);
        }
      }
    
      @Override
      public void visit(TextNode node) {
        if (node != parent) {
          children.add(node);
        }
      }
    };
    parent.accept(walker);
    return children;
  }
}
