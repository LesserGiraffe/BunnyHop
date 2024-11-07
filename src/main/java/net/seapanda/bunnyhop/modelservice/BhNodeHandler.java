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

package net.seapanda.bunnyhop.modelservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelprocessor.ImitationFinder;
import net.seapanda.bunnyhop.modelprocessor.ImitationRemover;
import net.seapanda.bunnyhop.modelprocessor.NodeDeselector;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.PasteCanceler;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.modelprocessor.WorkspaceRegisterer;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

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
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return この操作で消した子ノードの削除結果のリスト.
   *         0 番目が {@code node} と, これの代わりに作成されたノードのペアであることが保証される.
   */
  public List<Swapped> deleteNode(BhNode node, UserOperationCommand userOpeCmd) {
    if (node.isDeleted()) {
      return new ArrayList<>();
    }
    Set<Imitatable> imitations = new HashSet<>();
    imitations.addAll(ImitationFinder.find(node));
    List<Swapped> swappedNodes = delete(node, userOpeCmd);
    swappedNodes.addAll(deleteNodes(imitations, userOpeCmd));
    return swappedNodes;
  }

  /**
   * 引数で指定したノードを全て削除する.
   *
   * @param nodes 削除するノード.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return この操作で消した子ノードの削除結果のリスト.
   */
  public List<Swapped> deleteNodes(
      Collection<? extends BhNode> nodes, UserOperationCommand userOpeCmd) {
    List<Swapped> swappedNodes = new ArrayList<>();
    for (BhNode node : nodes) {
      swappedNodes.addAll(deleteNode(node, userOpeCmd));
    }
    return swappedNodes;
  }

  private List<Swapped> delete(BhNode node, UserOperationCommand userOpeCmd) {
    //undo時に削除前の状態のBhNodeを選択ノードとして MultiNodeShifterController に通知するためここで非選択にする
    NodeDeselector.deselect(node, userOpeCmd);
    final List<Swapped> swappedNodes = removeDependingOnState(node, userOpeCmd);
    MsgService.INSTANCE.removeQtRectangle(node, userOpeCmd);  //4 分木空間からの削除
    WorkspaceRegisterer.deregister(node, userOpeCmd);
    PasteCanceler.cancel(node, userOpeCmd);
    ImitationRemover.remove(node, userOpeCmd);
    return swappedNodes;
  }
  
  /** ノードのステートごとの削除処理を行う. */
  private List<Swapped> removeDependingOnState(BhNode node, UserOperationCommand userOpeCmd) {
    List<Swapped> swappedNodes = new ArrayList<>();
    BhNode.State nodeState = node.getState();
    switch (nodeState) {
      case CHILD:
        swappedNodes.addAll(removeChild(node, userOpeCmd));
        MsgService.INSTANCE.removeFromGuiTree(node);
        break;

      case ROOT_DANGLING:
        MsgService.INSTANCE.removeFromGuiTree(node);  // GUIツリー上から削除
        break;

      case ROOT_DIRECTLY_UNDER_WS:
        removeFromWs(node, userOpeCmd);
        break;

      case DELETED:
        return swappedNodes;

      default:
        throw new AssertionError("invalid node state " + nodeState);
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
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式.
   *         {@code node} が子ノードであった場合, 0 番目が {@code node} と, これの代わりに作成されたノードのペアであることが保証される.
   */
  public List<Swapped> moveToWs(
      Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {
    final Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    final List<Swapped> swappedNodes = removeDependingOnState(node, userOpeCmd);
    WorkspaceRegisterer.register(node, ws, userOpeCmd);
    MsgService.INSTANCE.addRootNode(node, ws, userOpeCmd);  //ワークスペースに移動
    MsgService.INSTANCE.addQtRectangle(node, ws, userOpeCmd);
    MsgService.INSTANCE.setPosOnWs(node, x, y);  //ワークスペース内での位置登録
    userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
    return swappedNodes;
  }

  /**
   * {@code node} を {@code ws} から移動する.
   *
   * @param node ワークスペース直下から移動させるノード.
   *             呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private void removeFromWs(BhNode node, UserOperationCommand userOpeCmd) {
    Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    MsgService.INSTANCE.removeRootNode(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
  }

  /**
   * 子ノードを取り除く (GUI ツリー上からは取り除かない).
   * {@code oldChild} が子ノードでなかった場合何もしない.
   *
   * @param toRemove 取り除く子ノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return {@code toRemove} を取り除くことで変更のあったノード一式
   *         0 番目が {@code toRemove} と, これの代わりに作成されたノードのペアであることが保証される.
   *         {@code oldChild} が子ノードでなかった場合は, 空のリスト.
   */
  public List<Swapped> removeChild(BhNode toRemove, UserOperationCommand userOpeCmd) {
    Workspace ws = toRemove.getWorkspace();
    List<Swapped> swappedNodes = toRemove.remove(userOpeCmd);
    // 子ノードを取り除いた結果新しくできたノードを, 4 分木空間に登録し, ビューツリーにつなぐ
    BhNode newNode = swappedNodes.get(0).newNode();
    NodeMvcBuilder.build(newNode);
    TextImitationPrompter.prompt(newNode);
    WorkspaceRegisterer.register(newNode, ws, userOpeCmd);
    MsgService.INSTANCE.addQtRectangle(newNode, ws, userOpeCmd);
    MsgService.INSTANCE.replaceChildNodeView(toRemove, newNode, userOpeCmd);

    for (Swapped imits : new ArrayList<>(swappedNodes.subList(1, swappedNodes.size()))) {
      swappedNodes.addAll(completeNewNodeReplacement(imits, userOpeCmd));
    }
    SyntaxErrorNodeManager.INSTANCE.collect(toRemove, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.collect(newNode, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
    return swappedNodes;
  }

  /**
   * 子ノードを入れ替える ({@code oldChild} は GUI ツリー上からは取り除かない).
   * {@code oldChild} が子ノードでなかった場合何もしない.
   *
   * @param oldChild 入れ替え対象の古いノード.
   *                 呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param newChild 入れ替え対象の新しいノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式
   *         0 番目が {@code oldChild} と {@code newChild} のペアであることが保証される.
   *         {@code oldChild} が子ノードでなかった場合は, 空のリスト.
   */
  public List<Swapped> replaceChild(
      BhNode oldChild, BhNode newChild, UserOperationCommand userOpeCmd) {
    if (!oldChild.isChild()) {
      return new ArrayList<>();
    }
    final List<Swapped> swappedNodes = removeDependingOnState(newChild, userOpeCmd);
    Workspace ws = oldChild.getWorkspace();
    WorkspaceRegisterer.register(newChild, ws, userOpeCmd);
    swappedNodes.addAll(oldChild.replace(newChild, userOpeCmd));
    MsgService.INSTANCE.addQtRectangle(newChild, ws, userOpeCmd);
    MsgService.INSTANCE.replaceChildNodeView(oldChild, newChild, userOpeCmd);
    
    for (Swapped imits : new ArrayList<>(swappedNodes.subList(1, swappedNodes.size()))) {
      swappedNodes.addAll(completeNewNodeReplacement(imits, userOpeCmd));
    }
    SyntaxErrorNodeManager.INSTANCE.collect(oldChild, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.collect(newChild, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
    return swappedNodes;
  }

  /** ノード入れ替えにともなって新しく作成されたノードの入れ替え処理を完了させる. */
  private List<Swapped> completeNewNodeReplacement(Swapped nodes, UserOperationCommand userOpeCmd) {
    Workspace ws = nodes.oldNode().getWorkspace();
    NodeMvcBuilder.build(nodes.newNode());
    TextImitationPrompter.prompt(nodes.newNode());
    WorkspaceRegisterer.register(nodes.newNode(), ws, userOpeCmd);
    MsgService.INSTANCE.addQtRectangle(nodes.newNode(), ws, userOpeCmd);
    MsgService.INSTANCE.replaceChildNodeView(nodes.oldNode(), nodes.newNode(), userOpeCmd);
    HomologueCache.INSTANCE.put((Imitatable) nodes.oldNode());
    return deleteNode(nodes.oldNode(), userOpeCmd);
  }

  /**
   * 2つのノードを入れ替える.
   *
   * @param nodeA 入れ替えたいノード (ダングリング状態のノードはエラー)
   * @param nodeB 入れ替えたいノード (ダングリング状態のノードはエラー)
   */
  public void exchangeNodes(BhNode nodeA, BhNode nodeB, UserOperationCommand userOpeCmd) {
    if (nodeA.getState() == BhNode.State.DELETED
        || nodeA.getState() == BhNode.State.ROOT_DANGLING
        || nodeB.getState() == BhNode.State.DELETED
        || nodeB.getState() == BhNode.State.ROOT_DANGLING) {
      throw new AssertionError("try to exchange dangling/deleted nodes.  "
          + nodeA.getSymbolName() + "(" + nodeA.getState() + ")    "
          + nodeB.getSymbolName() + "(" + nodeB.getState() + ")");

    } else if (nodeA.isDescendantOf(nodeB) || nodeB.isDescendantOf(nodeA)) {
      throw new AssertionError("try to exchange parent-child relationship nodes.  "
          + nodeA.getSymbolName() + "    " + nodeB.getSymbolName());
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
        List<Swapped> swappedNodesA = removeChild(nodeA, userOpeCmd);
        BhNode newNodeA = swappedNodesA.get(0).newNode();
        List<Swapped> swappedNodesB = removeChild(nodeB, userOpeCmd);
        BhNode newNodeB = swappedNodesB.get(0).newNode();        
        replaceChild(newNodeA, nodeB, userOpeCmd);
        replaceChild(newNodeB, nodeA, userOpeCmd);
        deleteNode(newNodeA, userOpeCmd);
        deleteNode(newNodeB, userOpeCmd);
      // (child, workspace)
      } else {
        replaceChild(nodeA, nodeB, userOpeCmd);
        moveToWs(wsB, nodeA, posB.x, posB.y, userOpeCmd);
      }
    //(workspace, workspace)
    } else {
      moveToWs(wsA, nodeB, posA.x, posA.y, userOpeCmd);
      moveToWs(wsB, nodeA, posB.x, posB.y, userOpeCmd);
    }
  }
}
