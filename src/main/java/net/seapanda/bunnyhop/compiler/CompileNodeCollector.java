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

package net.seapanda.bunnyhop.compiler;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.model.NodeGraphSnapshot;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;

/**
 * コンパイル対象のノードを集める.
 *
 * @author K.Koike
 */
public class CompileNodeCollector {

  private CompileNodeCollector() {}

  /**
   * コンパイル対象のノードを集める.
   * 返されるノードは, ワークスペースに存在するノードのディープコピー.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める.
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code wss} に存在する全ノードのスナップショットと実行ノードのペア
   */
  public static Optional<Pair<NodeGraphSnapshot, BhNode>> collect(
      WorkspaceSet wss, UserOperation userOpe) {
    return new CompileNodeCollector().collectNodes(wss, userOpe);
  }

  /**
   * コンパイル対象のノードを集める.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める.
   * @param userOpe undo 用コマンドオブジェクト.
   * @return コンパイル対象ノードと実行対象ノードのペア
   */
  private Optional<Pair<NodeGraphSnapshot, BhNode>> collectNodes(
      WorkspaceSet wss, UserOperation userOpe) {
    if (!deleteCompileErrorNodes(userOpe)) {
      return Optional.empty();
    }
    return findNodeToExecute(wss.getCurrentWorkspace(), userOpe)
        .map(nodeToExec -> {
          var snapshot = new NodeGraphSnapshot(wss);
          BhNode copyOfNodeToExec = getNodeToExec(snapshot, nodeToExec);
          return new Pair<>(snapshot, copyOfNodeToExec);
        });
  }

  /**
   * コンパイルエラーノードを削除する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return 全てのコンパイルエラーノードが無くなった場合 true.
   */
  private boolean deleteCompileErrorNodes(UserOperation userOpe) {
    if (!BhService.compileErrNodeManager().hasErrorNodes()) {
      return true;
    }
    Optional<ButtonType> btnType = BhService.msgPrinter().alert(
        Alert.AlertType.CONFIRMATION,
        "コンパイルエラーノードの削除",
        null,
        "コンパイルエラーノードを削除してもよろしいですか?\n「いいえ」を選択した場合、実行を中止します",
        ButtonType.NO,
        ButtonType.YES);

    if (btnType.isEmpty()) {
      return false;
    }
    return btnType
        .map(type -> {
          if (type.equals(ButtonType.YES)) {
            BhService.compileErrNodeManager().unmanageNonErrorNodes(userOpe);
            BhService.compileErrNodeManager().deleteErrorNodes(userOpe);
            return true;
          }
          return false;
        })
        .orElse(false);
  }

  /**
   * 実行対象のノードを探す.
   *
   * @param ws このワークスペースに実行対象があるかどうかチェックする.
   * @param userOpe undo 用コマンドオブジェクト.
   * @return 実行対象のノード
   */
  private Optional<BhNode> findNodeToExecute(Workspace ws, UserOperation userOpe) {
    if (ws == null) {
      return Optional.empty();
    }
    List<BhNode> selectedNodeList = ws.getSelectedNodeList();
    if (selectedNodeList.isEmpty()) {
      BhService.msgPrinter().alert(AlertType.ERROR, "実行対象の選択", null, "実行対象を一つ選択してください");
      return Optional.empty();
    }
    // 実行対象以外を非選択に.
    BhNode nodeToExec = selectedNodeList.get(0).findRootNode();
    ws.clearSelectedNodeList(userOpe);
    ws.addSelectedNode(nodeToExec, userOpe);
    return Optional.of(nodeToExec);
  }

  /** 実行ノード対象のノードを取得する. */
  private BhNode getNodeToExec(NodeGraphSnapshot snapshot, BhNode nodeToExec) {
    BhNode copyOfNodeToExec = snapshot.getMapOfSymbolIdToNode().get(nodeToExec.getInstanceId());
    if (copyOfNodeToExec == null) {
      var msg = "The copy of the BhNode to execute was not found in the snapshot.";
      BhService.msgPrinter().errForDebug(msg);
      throw new AssertionError(msg);
    }
    return copyOfNodeToExec;
  }
}
