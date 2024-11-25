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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelprocessor.DerivativeCollector;
import net.seapanda.bunnyhop.modelprocessor.DerivativeRemover;
import net.seapanda.bunnyhop.modelprocessor.NodeDeselector;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.PasteCanceler;
import net.seapanda.bunnyhop.modelprocessor.TextPrompter;
import net.seapanda.bunnyhop.modelprocessor.WorkspaceRegisterer;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * BhNodeの追加, 移動, 入れ替え, 削除用メソッドを提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeHandler {

  public static final BhNodeHandler INSTANCE = new BhNodeHandler();

  private BhNodeHandler() { }

  /**
   * 引数で指定したBhNodeを削除する.
   *
   * @param node 削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作で消した子ノードの削除結果のリスト.
   *         0 番目が {@code node} と, これの代わりに作成されたノードのペアであることが保証される.
   */
  public List<Swapped> deleteNode(BhNode node, UserOperation userOpe) {
    if (node.isDeleted()) {
      return new ArrayList<>();
    }
    Set<Derivative> derivatives = new HashSet<>();
    derivatives.addAll(DerivativeCollector.find(node));
    List<Swapped> swappedNodes = delete(node, userOpe);
    swappedNodes.addAll(deleteNodes(derivatives, userOpe));
    return swappedNodes;
  }

  /**
   * 引数で指定したノードを全て削除する.
   *
   * @param nodes 削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作で消した子ノードの削除結果のリスト.
   */
  public List<Swapped> deleteNodes(
      Collection<? extends BhNode> nodes, UserOperation userOpe) {
    List<Swapped> swappedNodes = new ArrayList<>();
    for (BhNode node : nodes) {
      swappedNodes.addAll(deleteNode(node, userOpe));
    }
    return swappedNodes;
  }

  private List<Swapped> delete(BhNode node, UserOperation userOpe) {
    //undo時に削除前の状態のBhNodeを選択ノードとして MultiNodeShifterController に通知するためここで非選択にする
    NodeDeselector.deselect(node, userOpe);
    MsgService.INSTANCE.removeQtRectangle(node, userOpe);  //4 分木空間からの削除
    final List<Swapped> swappedNodes = removeDependingOnState(node, userOpe);
    WorkspaceRegisterer.deregister(node, userOpe);
    PasteCanceler.cancel(node, userOpe);
    DerivativeRemover.remove(node, userOpe);
    return swappedNodes;
  }
  
  /** ノードのステートごとの削除処理を行う. */
  private List<Swapped> removeDependingOnState(BhNode node, UserOperation userOpe) {
    List<Swapped> swappedNodes = new ArrayList<>();
    BhNode.State nodeState = node.getState();
    switch (nodeState) {
      case CHILD:
        swappedNodes.addAll(removeChild(node, userOpe));
        MsgService.INSTANCE.removeFromGuiTree(node);
        break;

      case ROOT_DANGLING:
        MsgService.INSTANCE.removeFromGuiTree(node);  // GUIツリー上から削除
        break;

      case ROOT_DIRECTLY_UNDER_WS:
        removeFromWs(node, userOpe);
        break;

      case DELETED:
        return swappedNodes;

      default:
        throw new AssertionError("Invalid node state " + nodeState);
    }
    return swappedNodes;
  }

  /**
   * {@code node} を {@code ws} に移動する.
   *
   * @param ws {@code node} を追加したいワークスペース
   * @param node WS直下に追加したいノード.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param userOpe undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式.
   *         {@code node} が子ノードであった場合, 0 番目が {@code node} と, これの代わりに作成されたノードのペアであることが保証される.
   */
  public List<Swapped> moveToWs(
      Workspace ws, BhNode node, double x, double y, UserOperation userOpe) {
    return moveToWs(ws, node, x, y, true, userOpe);
  }

  /**
   * {@code node} を {@code ws} に移動する.
   *
   * @param ws {@code node} を追加したいワークスペース
   * @param node WS直下に追加したいノード.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param checkSyntaxErr 構文エラーをチェックする場合 true
   * @param userOpe undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式.
   *         {@code node} が子ノードであった場合, 0 番目が {@code node} と, これの代わりに作成されたノードのペアであることが保証される.
   */
  public List<Swapped> moveToWs(
      Workspace ws,
      BhNode node,
      double x,
      double y,
      boolean checkSyntaxErr,
      UserOperation userOpe) {
    final Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    final List<Swapped> swappedNodes = removeDependingOnState(node, userOpe);
    WorkspaceRegisterer.register(node, ws, userOpe);
    MsgService.INSTANCE.addRootNode(node, ws, userOpe);  //ワークスペースに移動
    MsgService.INSTANCE.setQtRectangle(node, ws, userOpe);
    MsgService.INSTANCE.setPosOnWs(node, x, y);  //ワークスペース内での位置登録
    userOpe.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    if (checkSyntaxErr) {
      SyntaxErrorNodeManager.INSTANCE.collect(node, userOpe);
      SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpe);
    }
    return swappedNodes;
  }

  /**
   * {@code node} を {@code ws} から移動する.
   *
   * @param node ワークスペース直下から移動させるノード.
   *             呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void removeFromWs(BhNode node, UserOperation userOpe) {
    Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    userOpe.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    MsgService.INSTANCE.removeRootNode(node, userOpe);
    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpe);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpe);
  }

  /**
   * 子ノードを取り除く (GUI ツリー上からは取り除かない).
   * {@code oldChild} が子ノードでなかった場合何もしない.
   *
   * @param toRemove 取り除く子ノード. 呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code toRemove} を取り除くことで変更のあったノード一式
   *         0 番目が {@code toRemove} と, これの代わりに作成されたノードのペアであることが保証される.
   *         {@code oldChild} が子ノードでなかった場合は, 空のリスト.
   */
  public List<Swapped> removeChild(BhNode toRemove, UserOperation userOpe) {
    Workspace ws = toRemove.getWorkspace();
    List<Swapped> swappedNodes = toRemove.remove(userOpe);
    // 子ノードを取り除いた結果新しくできたノードを, 4 分木空間に登録し, ビューツリーにつなぐ
    BhNode newNode = swappedNodes.get(0).newNode();
    NodeMvcBuilder.build(newNode);
    TextPrompter.prompt(newNode);
    WorkspaceRegisterer.register(newNode, ws, userOpe);
    MsgService.INSTANCE.setQtRectangle(newNode, ws, userOpe);
    MsgService.INSTANCE.replaceChildNodeView(toRemove, newNode, userOpe);

    for (Swapped swapped : new ArrayList<>(swappedNodes.subList(1, swappedNodes.size()))) {
      swappedNodes.addAll(completeNewNodeReplacement(swapped, userOpe));
    }
    SyntaxErrorNodeManager.INSTANCE.collect(toRemove, userOpe);
    SyntaxErrorNodeManager.INSTANCE.collect(newNode, userOpe);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpe);
    return swappedNodes;
  }

  /**
   * 子ノードを入れ替える ({@code oldChild} は GUI ツリー上からは取り除かない).
   * {@code oldChild} が子ノードでなかった場合何もしない.
   *
   * @param oldChild 入れ替え対象の古いノード.
   *                 呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param newChild 入れ替え対象の新しいノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式
   *         0 番目が {@code oldChild} と {@code newChild} のペアであることが保証される.
   *         {@code oldChild} が子ノードでなかった場合は, 空のリスト.
   */
  public List<Swapped> replaceChild(
      BhNode oldChild, BhNode newChild, UserOperation userOpe) {
    if (!oldChild.isChild()) {
      return new ArrayList<>();
    }
    final List<Swapped> allSwappedNodes = removeDependingOnState(newChild, userOpe);
    Workspace ws = oldChild.getWorkspace();
    WorkspaceRegisterer.register(newChild, ws, userOpe);
    List<Swapped> swappedNodes = oldChild.replace(newChild, userOpe);
    allSwappedNodes.addAll(swappedNodes);
    MsgService.INSTANCE.setQtRectangle(newChild, ws, userOpe);
    MsgService.INSTANCE.replaceChildNodeView(oldChild, newChild, userOpe);
    
    for (Swapped swapped : new ArrayList<>(swappedNodes.subList(1, swappedNodes.size()))) {
      allSwappedNodes.addAll(completeNewNodeReplacement(swapped, userOpe));
    }
    SyntaxErrorNodeManager.INSTANCE.collect(oldChild, userOpe);
    SyntaxErrorNodeManager.INSTANCE.collect(newChild, userOpe);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpe);
    return allSwappedNodes;
  }

  /** ノード入れ替えにともなって新しく作成されたノードの入れ替え処理を完了させる. */
  private List<Swapped> completeNewNodeReplacement(Swapped nodes, UserOperation userOpe) {
    Workspace ws = nodes.oldNode().getWorkspace();
    NodeMvcBuilder.build(nodes.newNode());
    TextPrompter.prompt(nodes.newNode());
    WorkspaceRegisterer.register(nodes.newNode(), ws, userOpe);
    MsgService.INSTANCE.setQtRectangle(nodes.newNode(), ws, userOpe);
    MsgService.INSTANCE.replaceChildNodeView(nodes.oldNode(), nodes.newNode(), userOpe);
    DerivativeCache.INSTANCE.put((Derivative) nodes.oldNode());
    return deleteNode(nodes.oldNode(), userOpe);
  }

  /**
   * 2つのノードを入れ替える.
   *
   * @param nodeA 入れ替えたいノード (ダングリング状態のノードはエラー)
   * @param nodeB 入れ替えたいノード (ダングリング状態のノードはエラー)
   */
  public void exchangeNodes(BhNode nodeA, BhNode nodeB, UserOperation userOpe) {
    if (nodeA.getState() == BhNode.State.DELETED
        || nodeA.getState() == BhNode.State.ROOT_DANGLING
        || nodeB.getState() == BhNode.State.DELETED
        || nodeB.getState() == BhNode.State.ROOT_DANGLING) {

      throw new IllegalStateException(
          "try to exchange dangling/deleted nodes.    %s(%s)  %s(%s)".formatted(
              nodeA.getSymbolName(), nodeA.getState(), nodeB.getSymbolName(), nodeB.getState()));

    } else if (nodeA.isDescendantOf(nodeB) || nodeB.isDescendantOf(nodeA)) {
      throw new IllegalStateException("try to exchange parent-child relationship nodes.    %s  %s"
          .formatted(nodeA.getSymbolName(), nodeB.getSymbolName()));
    }
    if (nodeA.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS
        && nodeB.getState() == BhNode.State.CHILD) {
      //swap
      BhNode tmp = nodeA;
      nodeA = nodeB;
      nodeB = tmp;
    }

    Vec2D posA = MsgService.INSTANCE.getPosOnWs(nodeA);
    Vec2D posB = MsgService.INSTANCE.getPosOnWs(nodeB);
    Workspace wsA = nodeB.getWorkspace();
    Workspace wsB = nodeB.getWorkspace();

    if (nodeA.getState() == BhNode.State.CHILD) {
      // (child, child)
      if (nodeB.getState() == BhNode.State.CHILD) {
        List<Swapped> swappedNodesA = removeChild(nodeA, userOpe);
        BhNode newNodeA = swappedNodesA.get(0).newNode();
        List<Swapped> swappedNodesB = removeChild(nodeB, userOpe);
        BhNode newNodeB = swappedNodesB.get(0).newNode();        
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
