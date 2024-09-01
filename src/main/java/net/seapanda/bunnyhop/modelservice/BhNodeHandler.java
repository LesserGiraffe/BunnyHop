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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.State;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelprocessor.ImitationFinder;
import net.seapanda.bunnyhop.modelprocessor.ImitationRemover;
import net.seapanda.bunnyhop.modelprocessor.NodeDeselector;
import net.seapanda.bunnyhop.modelprocessor.PasteCanceler;
import net.seapanda.bunnyhop.modelprocessor.WorkspaceRegisterer;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * BhNodeの追加, 移動, 入れ替え, 削除用メソッドを提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeHandler {

  public static final BhNodeHandler INSTANCE = new BhNodeHandler();  //!< シングルトンインスタンス

  private BhNodeHandler() { }

  /**
   * {@link Workspace} へのBhNodeの新規追加と4 分木空間への登録を行う.
   *
   * @param ws BhNodeを追加したいワークスペース
   * @param node WS直下に追加したいノード.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void addRootNode(
      Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {
    final Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    WorkspaceRegisterer.register(node, ws, userOpeCmd);  //ツリーの各ノードへのWSの登録
    MsgService.INSTANCE.addRootNode(node, ws, userOpeCmd);  //ワークスペース直下に追加
    MsgService.INSTANCE.addQtRectangle(node, ws, userOpeCmd);  //4 分木ノード登録(重複登録はされない)
    MsgService.INSTANCE.setPosOnWs(node, x, y);  //ワークスペース内での位置登録
    userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
  }

  /**
   * 引数で指定したBhNodeを削除する.
   *
   * @param node WSから取り除きたいノード.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 削除したノードと入れ替わる子ノードが作成された場合, そのノードを返す
   */
  public Optional<BhNode> deleteNode(BhNode node, UserOperationCommand userOpeCmd) {
    if (!isNodeToDelete(node)) {
      return Optional.empty();
    }
    Set<Imitatable> imitations = new HashSet<>();
    imitations.addAll(ImitationFinder.find(node));
    Optional<BhNode> newNode = delete(node, DeleteOperation.getSet(), userOpeCmd);
    deleteImitations(imitations, userOpeCmd);
    return newNode;
  }

  /**
   * 引数で指定したノードを全て削除する.
   *
   * @param nodeListToDelete 削除するノード.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 削除したノードと入れ替わる子ノードが作成された場合, 削除された古いノードと新しく作成されたノードのペアのリストを返す
   */
  public List<Pair<BhNode, BhNode>> deleteNodes(
      Collection<? extends BhNode> nodeListToDelete, UserOperationCommand userOpeCmd) {
    Collection<? extends BhNode> nodesToDelete = nodeListToDelete.stream()
        .filter(this::isNodeToDelete)
        .collect(Collectors.toCollection(ArrayList::new));

    if (nodesToDelete.isEmpty()) {
      return new ArrayList<>();
    }
    Set<Imitatable> imitations = new HashSet<>();
    nodesToDelete.forEach(node -> imitations.addAll(ImitationFinder.find(node)));
    List<Pair<BhNode, BhNode>> oldAndNewNodeList = new ArrayList<>();
    Set<DeleteOperation> allOperations = DeleteOperation.getSet();
    for (BhNode node : nodesToDelete) {
      if (isNodeToDelete(node)) {
        Optional<BhNode> newNodeOpt = delete(node, allOperations, userOpeCmd);
        newNodeOpt.ifPresent(newNode -> oldAndNewNodeList.add(new Pair<>(node, newNode)));
      }
    }
    deleteImitations(imitations, userOpeCmd);
    return oldAndNewNodeList;
  }

  /**
   * 引数で指定した {@link BhNode} を遅延削除する.
   *
   * <pre>
   * 引数で指定したノードは遅延削除リストに入る.
   * この関数で削除したノードは, {@link DelayedDeleter#delete}
   * もしくは {@link DelayedDeleter#deleteAll} を呼び出すと完全に削除される.
   * </pre>
   *
   * @param node 仮削除するノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @param operationsWithDelay {@link DelayedDeleter} による削除時に実行する削除操作
   * @return 削除したノードと入れ替わる子ノードが作成された場合, そのノードを返す
   */
  public Optional<BhNode> deleteNodeWithDelay(
      BhNode node, UserOperationCommand userOpeCmd, DeleteOperation... operationsWithDelay) {
    if (!isNodeToDelete(node)) {
      return Optional.empty();
    }
    List<DeleteOperation> opeListWithDelay = Arrays.asList(operationsWithDelay);
    Set<DeleteOperation> opeListWithoutDelay = DeleteOperation.getSet();
    opeListWithoutDelay.removeAll(opeListWithDelay);
    Optional<BhNode> newNode = delete(node, opeListWithoutDelay, userOpeCmd);
    DelayedDeleter.INSTANCE.addDeletionCandidate(node, opeListWithDelay);
    return newNode;
  }

  /**
   * 引数で指定したノードの削除操作を実行する.
   *
   * @param node 削除するノード
   * @param optionalOperations 追加で行う削除操作
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 削除された {@code node} と入れ替わるために作成されたノード. ({@code node} が子ノードの時のみ存在)
   */
  private Optional<BhNode> delete(
      BhNode node, Set<DeleteOperation> optionalOperations, UserOperationCommand userOpeCmd) {
    //undo時に削除前の状態のBhNodeを選択ノードとして MultiNodeShifterController に通知するためここで非選択にする
    NodeDeselector.deselect(node, userOpeCmd);
    final Optional<BhNode> newNode = removeDependingOnState(node, userOpeCmd);
    MsgService.INSTANCE.removeQtRectangle(node, userOpeCmd);  //4 分木空間からの削除
    WorkspaceRegisterer.deregister(node, userOpeCmd);
    PasteCanceler.cancel(node, userOpeCmd);
    if (optionalOperations.contains(DeleteOperation.REMOVE_FROM_IMIT_LIST)) {
      ImitationRemover.remove(node, userOpeCmd);
    }
    return newNode;
  }

  /** ノードのステートごとの削除処理を行う. */
  private Optional<BhNode> removeDependingOnState(BhNode node, UserOperationCommand userOpeCmd) {
    Optional<BhNode> newNode = Optional.empty();
    BhNode.State nodeState = node.getState();
    switch (nodeState) {
      case CHILD:
        newNode = Optional.of(removeChild(node, userOpeCmd));
        MsgService.INSTANCE.removeFromGuiTree(node);
        break;

      case ROOT_DANGLING:
        MsgService.INSTANCE.removeFromGuiTree(node);  //GUIツリー上から削除
        break;

      case ROOT_DIRECTLY_UNDER_WS:
        MsgService.INSTANCE.removeRootNode(node, userOpeCmd);  //WS直下から削除
        break;

      case DELETED:
        return newNode;

      default:
        throw new AssertionError("invalid node state " + nodeState);
    }
    return newNode;
  }

  private void deleteImitations(Set<Imitatable> imitations, UserOperationCommand userOpeCmd) {
    Set<Imitatable> imitToDelete = imitations.stream()
        .filter(this::isNodeToDelete)
        .collect(Collectors.toSet());
    if (imitToDelete.isEmpty()) {
      return;
    }

    imitToDelete.forEach(imit -> imit.getEventDispatcher().execOnDeletionRequested(
        imitToDelete, CauseOfDeletion.INFLUENCE_OF_ORIGINAL_DELETION, userOpeCmd));
    List<Pair<BhNode, BhNode>> oldAndNewNodeList = deleteNodes(imitToDelete, userOpeCmd);
    for (var oldAndNewNode : oldAndNewNodeList) {
      BhNode oldNode = oldAndNewNode.v1;
      BhNode newNode = oldAndNewNode.v2;
      newNode.findParentNode().execOnChildReplaced(
          oldNode, newNode, newNode.getParentConnector(), userOpeCmd);
    }
  }

  /**
   * 削除対象のノードかどうかを調べる.
   *
   * @param node 削除対象かどうか調べるノード
   * @return 削除対象のノードである場合 true
   */
  private boolean isNodeToDelete(BhNode node) {
    return !DelayedDeleter.INSTANCE.containsInCandidateList(node)
        && node.getState() != State.DELETED;
  }

  /**
   * {@code node} を {@code ws} に移動する (4 分木空間への登録は行わないが, 4 分木空間上の位置は更新する).
   *
   * @param ws {@code node} を追加したいワークスペース
   * @param node WS直下に追加したいノード.
   * @param x ワークスペース上での位置
   * @param y ワークスペース上での位置
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void moveToWs(
      Workspace ws, BhNode node, double x, double y, UserOperationCommand userOpeCmd) {
    if (node.getState() == State.ROOT_DIRECTLY_UNDER_WS) {
      removeFromWs(node, userOpeCmd);
    } else if (node.getState() == State.CHILD) {
      removeChild(node, userOpeCmd);
    }
    final Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    WorkspaceRegisterer.register(node, ws, userOpeCmd);
    MsgService.INSTANCE.addRootNode(node, ws, userOpeCmd);  //ワークスペースに移動
    MsgService.INSTANCE.setPosOnWs(node, x, y);  //ワークスペース内での位置登録
    userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    
    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
  }

  /**
   * {@code node} を {@code ws} から移動する (4 分木空間からの消去は行わない).
   *
   * @param node ワークスペース直下から移動させるノード.
   *             呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * */
  public void removeFromWs(BhNode node, UserOperationCommand userOpeCmd) {
    Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
    userOpeCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
    MsgService.INSTANCE.removeRootNode(node, userOpeCmd);

    SyntaxErrorNodeManager.INSTANCE.collect(node, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
  }

  /**
   * 子ノードを取り除く (GUIツリー上からは取り除かない).
   *
   * @param childToRemove 取り除く子ノード. 呼び出した後, WS直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 取り除いた子ノードの代わりに作成されたノード
   */
  public BhNode removeChild(BhNode childToRemove, UserOperationCommand userOpeCmd) {
    Workspace ws = childToRemove.getWorkspace();
    BhNode newNode = childToRemove.remove(userOpeCmd);
    // 子ノードを取り除いた結果新しくできたノードを, 4 分木空間に登録し, ビューツリーにつなぐ
    WorkspaceRegisterer.register(newNode, ws, userOpeCmd);  // ツリーの各ノードへのWSの登録
    MsgService.INSTANCE.addQtRectangle(newNode, ws, userOpeCmd);
    MsgService.INSTANCE.replaceChildNodeView(childToRemove, newNode, userOpeCmd);

    SyntaxErrorNodeManager.INSTANCE.collect(childToRemove, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
    return newNode;
  }

  /**
   * 子ノードを入れ替える.
   *
   * @param oldChildNode 入れ替え対象の古いノード.
   *                     呼び出した後, ワークスペース直下にもノードツリーにも居ない状態になるが消去はされない.
   * @param newNode 入れ替え対象の新しいノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void replaceChild(BhNode oldChildNode, BhNode newNode, UserOperationCommand userOpeCmd) {
    if (newNode.getState() == State.ROOT_DIRECTLY_UNDER_WS) {
      removeFromWs(newNode, userOpeCmd);
    } else if (newNode.getState() == State.CHILD) {
      removeChild(newNode, userOpeCmd);
    }
    WorkspaceRegisterer.register(newNode, oldChildNode.getWorkspace(), userOpeCmd);
    //新しいノードをビューツリーにつないで, 4 分木空間内の位置を更新する
    MsgService.INSTANCE.replaceChildNodeView(oldChildNode, newNode, userOpeCmd);
    //イミテーションの自動追加は, ビューツリーにつないだ後でなければならないので, モデルの変更はここで行う
    oldChildNode.replace(newNode, userOpeCmd);

    SyntaxErrorNodeManager.INSTANCE.collect(oldChildNode, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.collect(newNode, userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
  }

  /**
   * 2つのノードを入れ替える.
   *
   * @param nodeA 入れ替えたいノード (ダングリング状態のノードはエラー)
   * @param nodeB 入れ替えたいノード (ダングリング状態のノードはエラー)
   * */
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
        BhNode newNodeA = removeChild(nodeA, userOpeCmd);
        BhNode newNodeB = removeChild(nodeB, userOpeCmd);
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
