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

package net.seapanda.bunnyhop.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.model.node.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * コンパイルエラーの詳細をアプリケーションユーザに知らせるクラス.
 *
 * @author K.Koike
 */
public class CompileErrorReporter {

  /** これらのノードから親子関係または派生関係でたどれるノード群を, コンパイルエラーが発生しているか調べる対象とする. */
  private final Set<BhNode> startingPoints = new HashSet<>();
  
  /**
   * コンストラクタ.
   *
   * @param wss このワークスペースセットにあるノードのコンパイルエラーを表示する.
   */
  public CompileErrorReporter(WorkspaceSet wss) {
    WorkspaceSet.CallbackRegistry registry = wss.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> startingPoints.add(event.node()));
    registry.getOnNodeRemoved().add(event -> startingPoints.add(event.node()));
    registry.getOnRootNodeAdded().add(event -> startingPoints.add(event.node()));
    registry.getOnRootNodeRemoved().add(event -> startingPoints.add(event.node()));
  }

  /**
   * アプリケーションユーザにコンパイルエラーの詳細を知らせる.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void report(UserOperation userOpe) {
    Set<BhNode> nodesToCheckErr = collectRelatedNodes();
    for (BhNode node : nodesToCheckErr) {
      node.setCompileErrState(node.hasCompileError(), userOpe);
    }
    startingPoints.clear();
  }

  /** {@link #startingPoints} から親子関係または派生関係でたどれるノード群を取得する. */
  private Set<BhNode> collectRelatedNodes() {
    SequencedSet<BhNode> searched = new LinkedHashSet<>();
    for (BhNode node : startingPoints) {
      search(node, searched);
    }
    return searched;
  }

  private void search(BhNode node, SequencedSet<BhNode> searched) {
    if (node == null || searched.contains(node)) {
      return;
    }
    searched.add(node);
    getChildNodes(node).forEach(child -> search(child, searched));
    search(getParentNode(node), searched);
    search(node.getOriginal(), searched);
    if (node instanceof Derivative derivative) {
      derivative.getDerivatives().forEach(derv -> search(derv, searched));
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
