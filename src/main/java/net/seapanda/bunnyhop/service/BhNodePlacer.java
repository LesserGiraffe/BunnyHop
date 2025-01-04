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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.traverse.DerivativeCollector;
import net.seapanda.bunnyhop.model.traverse.DerivativeRemover;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;

/**
 * {@link BhNode} の移動, 入れ替え, 削除, ワークスペースへの追加を行う機能を提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodePlacer {

  BhNodePlacer() {}

  /**
   * {@code node} 以下のノードを全て削除する.
   * {@code node} 以下のノードの派生ノードも削除される.
   *
   * @param node このノード以下のノードを全て削除する.
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作の結果消えた子ノードとそれと入れ替わったノードのペアのリスト.
   *         <pre>
   *         {@code node} が子ノードであった場合
   *             最初の要素 : {@code node} とこれと入れ替わったノードのペア
   *             残りの要素 : {@code node} 以下のノードの削除によって消えた'子'派生ノードとそれと入れ替わったノードのペア.
   *                         ただし, 入れ替わった新しいノードがこの削除操作によって削除された場合,
   *                         戻り値のリストにそのペアは含まれない.
   * 
   *         {@code node} がルートノードであった場合
   *             全ての要素 : {@code node} 以下のノードの削除によって消えた'子'派生ノードとそれと入れ替わったノードのペア
   *                         ただし, 入れ替わった新しいノードがこの削除操作によって削除された場合,
   *                         戻り値のリストにそのペアは含まれない.
   *         </pre>
   */
  public SequencedSet<Swapped> deleteNode(BhNode node, UserOperation userOpe) {
    if (node.isDeleted()) {
      return new LinkedHashSet<>();
    }
    var derivatives = DerivativeCollector.collect(node);
    var tmp = new LinkedHashSet<Swapped>();
    tmp.addAll(deleteNodes(derivatives, userOpe));
    SequencedSet<Swapped> swappedNodes = delete(node, userOpe);
    swappedNodes.addAll(tmp);
    return swappedNodes;
  }

  /**
   * 引数で指定したノードを全て削除する.
   *
   * @param nodes 削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作の結果消えた子ノードとそれと入れ替わったノードのペアのリスト.
   *         ただし, 入れ替わった新しいノードがこの削除操作によって削除された場合,
   *         戻り値のリストにそのペアは含まれない.
   */
  public List<Swapped> deleteNodes(Collection<? extends BhNode> nodes, UserOperation userOpe) {
    List<Swapped> swappedNodes = new ArrayList<>();
    for (BhNode node : nodes) {
      swappedNodes.addAll(deleteNode(node, userOpe));
    }
    return swappedNodes;
  }

  private SequencedSet<Swapped> delete(BhNode node, UserOperation userOpe) {
    node.deselect(userOpe);
    DerivativeRemover.remove(node, userOpe);
    SequencedSet<Swapped> swappedNodes = node.remove(userOpe);
    node.getWorkspace().removeNodeTree(node, userOpe);
    return swappedNodes;
  }
  
  /**
   * {@code node} 以下のノードを {@code ws} に移動する.
   *
   * @param ws {@code node} 以下のノードを移動させたいワークスペース
   * @param node これ以下のノードをワークスペース直下に移動させる.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作で入れ替わった子ノードのペアのリスト.
   *         <pre>
   *         {@code node} が子ノードであった場合
   *             最初の要素 : {@code node} とこれと入れ替わったノードのペア
   *             残りの要素 : {@code node} の入れ替えによって入れ替わった'子'派生ノードとそれと入れ替わったノードのペア.
   * 
   *         {@code node} がルートノードであった場合
   *             空のセット
   *         </pre>
   */
  public SequencedSet<Swapped> moveToWs(
      Workspace ws, BhNode node, double x, double y, UserOperation userOpe) {
    return moveToWs(ws, node, x, y, true, userOpe);
  }

  /**
   * {@code node} 以下のノードを {@code ws} に移動する.
   *
   * @param ws {@code node} 以下のノードを移動させたいワークスペース
   * @param node これ以下のノードをワークスペース直下に移動させる.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param checkCompileErr コンパイルエラーをチェックする場合 true
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作によって入れ替わった子ノードのペアのリスト.
   *         <pre>
   *         {@code node} が子ノードであった場合
   *             最初の要素 : {@code node} とこれと入れ替わったノードのペア
   *             残りの要素 : {@code node} の入れ替えによって入れ替わった'子'派生ノードとそれと入れ替わったノードのペア.
   * 
   *         {@code node} がルートノードであった場合
   *             空のセット
   *         </pre>
   */
  public SequencedSet<Swapped> moveToWs(
      Workspace ws,
      BhNode node,
      double x,
      double y,
      boolean checkCompileErr,
      UserOperation userOpe) {
    var swappedNodes = new LinkedHashSet<Swapped>();
    if (node.isChild()) {
      swappedNodes.addAll(node.remove(userOpe));
    }
    ws.addNodeTree(node, userOpe);
    node.getViewProxy().setPosOnWorkspace(new Vec2D(x, y), userOpe);
    if (!swappedNodes.isEmpty()) {
      for (Swapped swapped : new ArrayList<>(swappedNodes).subList(1, swappedNodes.size())) {
        swappedNodes.addAll(deleteNode(swapped.oldNode(), userOpe));
      }
    }
    if (checkCompileErr) {
      BhService.compileErrNodeManager().collect(node, userOpe);
      BhService.compileErrNodeManager().updateErrorNodeIndicator(userOpe);
    }
    return swappedNodes;
  }

  /**
   * 子ノードを入れ替える.
   * {@code oldChild} が子ノードでなかった場合何もしない.
   *
   * @param oldChild 入れ替え対象の古いノード.
   *                 呼び出した後, 元々属していたワークスペースのルートノードとなる.
   * @param newChild 入れ替え対象の新しいノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作で入れ替わった子ノードのペアのリスト.
   *         <pre>
   *         {@code node} が子ノードであった場合
   *             最初の要素 : {@code oldChild} と {@code newChild} のペア
   *             残りの要素 : この操作によって入れ替わった'子'派生ノードとそれと入れ替わったノードのペア.
   * 
   *         {@code node} がルートノードであった場合
   *             空のセット
   *         </pre>
   */
  public SequencedSet<Swapped> replaceChild(
      BhNode oldChild, BhNode newChild, UserOperation userOpe) {
    if (!oldChild.isChild() || oldChild == newChild) {
      return new LinkedHashSet<>();
    }
    SequencedSet<Swapped> swappedNodes = new LinkedHashSet<>();
    swappedNodes.addAll(oldChild.replace(newChild, userOpe));
    for (Swapped swapped : new ArrayList<>(swappedNodes).subList(1, swappedNodes.size())) {
      swappedNodes.addAll(deleteNode(swapped.oldNode(), userOpe));
    }
    BhService.compileErrNodeManager().collect(oldChild, userOpe);
    BhService.compileErrNodeManager().collect(newChild, userOpe);
    BhService.compileErrNodeManager().updateErrorNodeIndicator(userOpe);
    return swappedNodes;
  }

  /**
   * 2つのノードを入れ替える.
   * {@code nodeA} と {@code nodeB} は直系であってはならない.
   * {@code nodeA} と {@code nodeB} はどちらも削除されていてはならない.
   *
   * @param nodeA 入れ替えたいノード
   * @param nodeB 入れ替えたいノード
   */
  public void exchangeNodes(BhNode nodeA, BhNode nodeB, UserOperation userOpe) {
    if (nodeA.isDeleted() || nodeB.isDeleted()) {
      throw new IllegalStateException(
          "try to exchange deleted node(s).    %s(%s)  %s(%s)".formatted(
              nodeA.getSymbolName(), nodeA.getState(), nodeB.getSymbolName(), nodeB.getState()));

    } else if (nodeA.isLinealWith(nodeB)) {
      throw new IllegalStateException("try to exchange lineal nodes.    %s  %s"
          .formatted(nodeA.getSymbolName(), nodeB.getSymbolName()));
    }
    if (nodeA.getState() == BhNode.State.ROOT
        && nodeB.getState() == BhNode.State.CHILD) {
      //swap
      BhNode tmp = nodeA;
      nodeA = nodeB;
      nodeB = tmp;
    }

    Vec2D posA = nodeA.getViewProxy().getPosOnWorkspace();
    Vec2D posB = nodeB.getViewProxy().getPosOnWorkspace();
    Workspace wsA = nodeB.getWorkspace();
    Workspace wsB = nodeB.getWorkspace();

    if (nodeA.getState() == BhNode.State.CHILD) {
      // (child, child)
      if (nodeB.getState() == BhNode.State.CHILD) {
        SequencedSet<Swapped> swappedNodesA = moveToWs(wsA, nodeA, 0, 0, userOpe);
        BhNode newNodeA = swappedNodesA.getFirst().newNode();
        SequencedSet<Swapped> swappedNodesB = moveToWs(wsB, nodeB, 0, 0, userOpe);
        BhNode newNodeB = swappedNodesB.getFirst().newNode();
        replaceChild(newNodeA, nodeB, userOpe);
        replaceChild(newNodeB, nodeA, userOpe);
        deleteNode(newNodeA, userOpe);
        deleteNode(newNodeB, userOpe);
      // (child, workspace)
      } else {
        replaceChild(nodeA, nodeB, userOpe);
        moveToWs(wsB, nodeA, posB.x, posB.y, userOpe);
      }
    //(workspace, workspace)
    } else {
      moveToWs(wsA, nodeB, posA.x, posA.y, userOpe);
      moveToWs(wsB, nodeA, posB.x, posB.y, userOpe);
    }
  }
}
