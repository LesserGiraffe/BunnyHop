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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.modelprocessor.SyntaxErrorNodeCollector;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 構文エラーノードを集めて管理するクラス.
 *
 * @author K.Koike
 */
public class SyntaxErrorNodeManager {

  public static final SyntaxErrorNodeManager INSTANCE = new SyntaxErrorNodeManager();
  /** 構文エラーノードのリスト. */
  private final Set<BhNode> errorNodeList = new HashSet<>();

  private SyntaxErrorNodeManager() {}

  /**
   * 以下の2種類の構文エラーノードを管理対象に入れる.
   * <pre>
   *   ・{@code node} 以下にある構文エラーノード
   *   ・{@code node} 以下にあるオリジナルノードが持つ構文エラーを起こしているイミテーションノード
   * </pre>
   */
  public void collect(BhNode node, UserOperationCommand userOpeCmd) {
    if (MsgService.INSTANCE.isTemplateNode(node)) {
      return;
    }
    for (BhNode errNode : SyntaxErrorNodeCollector.collect(node)) {
      if (!errorNodeList.contains(errNode)) {
        errorNodeList.add(errNode);
        userOpeCmd.pushCmdOfAddToList(errorNodeList, errNode);
      }
    }
  }

  /** 管理下のノードの構文エラー表示を更新する. */
  public void updateErrorNodeIndicator(UserOperationCommand userOpeCmd) {
    errorNodeList.forEach(node -> {
      if (node.getState() != BhNode.State.DELETED) {
        MsgService.INSTANCE.setSyntaxErrorIndicator(node, node.hasSyntaxError(), userOpeCmd);
      }
    });
  }

  /** 構文エラーノード以外のノードを全て管理下から外す. */
  public void unmanageNonErrorNodes(UserOperationCommand userOpeCmd) {
    var nodesToRemove =
        errorNodeList.stream()
        .filter(node -> !node.hasSyntaxError())
        .collect(Collectors.toCollection(ArrayList::new));
    errorNodeList.removeAll(nodesToRemove);
    userOpeCmd.pushCmdOfRemoveFromList(errorNodeList, nodesToRemove);
  }

  /** 管理下の全ての構文エラーノードを削除する.*/
  public void deleteErrorNodes(UserOperationCommand userOpeCmd) {
    var errNodes = errorNodeList.stream()
        .filter(node -> node.hasSyntaxError())
        .toList();

    var nodesToDelete = errNodes.stream()
        .filter(node -> node.getEventAgent().execOnDeletionRequested(
            errNodes, CauseOfDeletion.SYNTAX_ERROR, userOpeCmd))
        .toList();

    List<Swapped> swappedNodes =
        BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpeCmd);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpeCmd);
    }
    errorNodeList.removeAll(nodesToDelete);
    userOpeCmd.pushCmdOfRemoveFromList(errorNodeList, nodesToDelete);
  }

  /**
   * 管理下のノードに構文エラーノードがあるかどうか調べる.
   *
   * @return 構文エラーノードが1つでもある場合 true
   */
  public boolean hasErrorNodes() {
    return errorNodeList.stream().anyMatch(node -> node.hasSyntaxError());
  }
}
