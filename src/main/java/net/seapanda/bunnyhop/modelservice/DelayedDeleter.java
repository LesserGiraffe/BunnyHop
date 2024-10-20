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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.State;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.modelprocessor.ImitationFinder;
import net.seapanda.bunnyhop.modelprocessor.ImitationRemover;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 遅延削除を実行するクラス.
 *
 * @author Koike
 */
public class DelayedDeleter {

  /** シングルトンインスタンス. */
  public static final DelayedDeleter INSTANCE = new DelayedDeleter();
  /** 削除候補ノードと未実行の削除操作のマップ. */
  private Map<BhNode, List<DeleteOperation>> candidateToOpeList = new HashMap<>();

  private DelayedDeleter() {}

  /**
   * 削除候補のノードを追加する.
   * 登録するリストは, 4 分木空間, ワークスペースからの削除とモデルの親子関係の削除が済んでいること.
   *
   * @param candidate 削除候補のノード
   * @param operationsToDo 削除時に実行する操作のリスト
   */
  public void addDeletionCandidate(BhNode candidate, List<DeleteOperation> operationsToDo) {
    candidateToOpeList.put(candidate, operationsToDo);
  }

  /**
   * 引数で指定した削除候補のノードの未完了の削除操作を実行し, 削除候補リストから消す.
   * ただし, 指定したノードがワークスペース上にあった場合は削除されずに, 削除候補リストから消える.
   *
   * @param node 削除するノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void delete(BhNode node, UserOperationCommand userOpeCmd) {
    Set<Imitatable> imitations = new HashSet<>();
    if (candidateToOpeList.containsKey(node)) {
      // ワークスペースにあるノードは削除をキャンセルされたものとみなす
      if (!node.isInWorkspace()) {
        imitations.addAll(ImitationFinder.find(node));
        candidateToOpeList.get(node).forEach(ope -> delete(node, ope, userOpeCmd));
      }
    }
    candidateToOpeList.remove(node);
    deleteImitations(imitations, userOpeCmd);
  }

  /** 未実行の削除処理を行う. */
  private void delete(BhNode nodeToDelete, DeleteOperation ope, UserOperationCommand userOpeCmd) {
    switch (ope) {
      case REMOVE_FROM_IMIT_LIST:
        ImitationRemover.remove(nodeToDelete, userOpeCmd);
        break;

      default:
        throw new AssertionError("invalid deletion operation " + ope);
    }
  }

  /**
   * 削除候補のノードの未完了の削除操作を実行し, 削除候補リストから消す.
   * ただし, ワークスペース上にあるノードは削除されずに, 削除候補リストから消える.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void deleteAll(UserOperationCommand userOpeCmd) {
    Set<Imitatable> imitations = new HashSet<>();
    for (BhNode candidate : candidateToOpeList.keySet()) {
      //ワークスペースにあるノードは削除をキャンセルされたものとみなす
      if (candidate.isInWorkspace()) {
        continue;
      }
      imitations.addAll(ImitationFinder.find(candidate));
      candidateToOpeList.get(candidate).forEach(ope -> delete(candidate, ope, userOpeCmd));
    }
    candidateToOpeList.clear();
    deleteImitations(imitations, userOpeCmd);
  }

  private void deleteImitations(Set<Imitatable> imitations, UserOperationCommand userOpeCmd) {
    Set<Imitatable> imitToDelete = imitations.stream()
        .filter(this::isNodeToDelete)
        .collect(Collectors.toSet());

    if (imitToDelete.isEmpty()) {
      return;
    }
    imitToDelete.forEach(imit -> imit.getEventDispatcher().execOnDeletionRequested(
        imitToDelete, CauseOfDeletion.ORIGINAL_DELETION, userOpeCmd));

    List<Pair<BhNode, BhNode>> oldAndNewNodeList =
        BhNodeHandler.INSTANCE.deleteNodes(imitToDelete, userOpeCmd);
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
    return !containsInCandidateList(node) && node.getState() != State.DELETED;
  }

  /**
   * 削除候補とその子孫ノードの中に引数で指定したノードが入っているか調べる.
   *
   * @param node 調べるノード
   * @return 削除候補とその子孫ノードの中に引数で指定したノードが入っている場合 true
   */
  public boolean containsInCandidateList(BhNode node) {
    return candidateToOpeList.keySet().stream().anyMatch(
        candidate -> node.isDescendantOf(candidate));
  }

  /**
   * 削除候補のリストを返す.
   *
   * @return 削除候補のリスト
   */
  public Collection<BhNode> getDeletionCadidateList() {
    return candidateToOpeList.keySet();
  }
}
