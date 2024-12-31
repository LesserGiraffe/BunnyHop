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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.traverse.CompileErrorNodeCollector;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * コンパイルエラーノードを集めて管理するクラス.
 *
 * @author K.Koike
 */
public class CompileErrorNodeManager {

  /** コンパイルエラーノードのリスト. */
  private final Set<BhNode> errorNodeList = new HashSet<>();

  CompileErrorNodeManager() {}

  /**
   * 以下の2種類のコンパイルエラーノードを管理対象に入れる.
   * <pre>
   *   ・{@code node} 以下にあるコンパイルエラーノード
   *   ・{@code node} 以下にあるオリジナルノードが持つ派生ノードのうち, コンパイルエラーを起こしているもの
   * </pre>
   */
  public synchronized void collect(BhNode node, UserOperation userOpe) {
    if (node.getViewProxy().isTemplateNode()) {
      return;
    }
    for (BhNode errNode : CompileErrorNodeCollector.collect(node)) {
      if (!errorNodeList.contains(errNode)) {
        errorNodeList.add(errNode);
        userOpe.pushCmdOfAddToList(errorNodeList, errNode);
      }
    }
  }

  /** 管理下のノードのコンパイルエラー表示を更新する. */
  public synchronized void updateErrorNodeIndicator(UserOperation userOpe) {
    errorNodeList.forEach(node -> {
      if (!node.isDeleted()) {
        node.getViewProxy().setCompileErrorVisibility(node.hasCompileError(), userOpe);
      }
    });
  }

  /** コンパイルエラーノード以外のノードを全て管理下から外す. */
  public synchronized void unmanageNonErrorNodes(UserOperation userOpe) {
    var nodesToRemove =
        errorNodeList.stream()
        .filter(node -> !node.hasCompileError())
        .collect(Collectors.toCollection(ArrayList::new));
    errorNodeList.removeAll(nodesToRemove);
    userOpe.pushCmdOfRemoveFromList(errorNodeList, nodesToRemove);
  }

  /** 管理下の全てのコンパイルエラーノードを削除する.*/
  public synchronized void deleteErrorNodes(UserOperation userOpe) {
    var errNodes = errorNodeList.stream()
        .filter(node -> node.hasCompileError())
        .toList();

    var nodesToDelete = errNodes.stream()
        .filter(node -> node.getEventAgent().execOnDeletionRequested(
            errNodes, CauseOfDeletion.COMPILE_ERROR, userOpe))
        .toList();

    List<Swapped> swappedNodes = BhService.bhNodePlacer().deleteNodes(nodesToDelete, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
    errorNodeList.removeAll(nodesToDelete);
    userOpe.pushCmdOfRemoveFromList(errorNodeList, nodesToDelete);
  }

  /**
   * 管理下のノードにコンパイルエラーノードがあるかどうか調べる.
   *
   * @return コンパイルエラーノードが1つでもある場合 true
   */
  public synchronized boolean hasErrorNodes() {
    return errorNodeList.stream().anyMatch(node -> node.hasCompileError());
  }
}
