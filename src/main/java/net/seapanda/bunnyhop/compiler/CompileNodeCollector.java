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
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.model.NodeGraphSnapshot;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.service.MsgPrinter;
import net.seapanda.bunnyhop.service.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * コンパイル対象のノードを準備するクラス.
 *
 * @author K.Koike
 */
public class CompileNodeCollector {

  private CompileNodeCollector() {}

  /**
   * コンパイル対象のノードを準備する.
   * 返されるノードは, ワークスペースに存在するノードのディープコピー.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める.
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code wss} に存在する全ノードのスナップショットと実行ノードのペア
   */
  public static Optional<Pair<NodeGraphSnapshot, BhNode>> collect(
      WorkspaceSet wss, UserOperation userOpe) {
    return new CompileNodeCollector().collectNodesToCompile(wss, userOpe);
  }

  /**
   * コンパイル前の準備をする.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める.
   * @param userOpe undo 用コマンドオブジェクト.
   * @return コンパイル対象ノードと実行対象ノードのペア
   */
  private Optional<Pair<NodeGraphSnapshot, BhNode>> collectNodesToCompile(
      WorkspaceSet wss, UserOperation userOpe) {
    if (!deleteSyntaxErrorNodes(userOpe)) {
      BunnyHop.INSTANCE.pushUserOperation(userOpe);
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
   * 構文エラーノードを削除する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return 全ての構文エラーノードが無くなった場合 true.
   */
  private boolean deleteSyntaxErrorNodes(UserOperation userOpe) {
    if (!SyntaxErrorNodeManager.INSTANCE.hasErrorNodes()) {
      return true;
    }
    Optional<ButtonType> btnType = MsgPrinter.INSTANCE.alert(
        Alert.AlertType.CONFIRMATION,
        "構文エラーノードの削除",
        null,
        "構文エラーノードを削除してもよろしいですか?\n「いいえ」を選択した場合、実行を中止します",
        ButtonType.NO,
        ButtonType.YES);

    if (btnType.isEmpty()) {
      return false;
    }
    return btnType
        .map(type -> {
          if (type.equals(ButtonType.YES)) {
            SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(userOpe);
            SyntaxErrorNodeManager.INSTANCE.deleteErrorNodes(userOpe);
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
      MsgPrinter.INSTANCE.alert(AlertType.ERROR, "実行対象の選択", null, "実行対象を一つ選択してください");
      return Optional.empty();
    }
    // 実行対象以外を非選択に.
    BhNode nodeToExec = selectedNodeList.get(0).findRootNode();
    ws.clearSelectedNodeList(userOpe);
    ws.addSelectedNode(nodeToExec, userOpe);
    BunnyHop.INSTANCE.pushUserOperation(userOpe);
    return Optional.of(nodeToExec);
  }

  /** 実行ノード対象のノードを取得する. */
  private BhNode getNodeToExec(
      NodeGraphSnapshot snapshot, BhNode nodeToExec) {
    BhNode copyOfNodeToExec = snapshot.getMapOfSymbolIdToNode().get(nodeToExec.getInstanceId());
    if (copyOfNodeToExec == null) {
      var msg = "The copy of the BhNode to execute was not found in the snapshot.";
      MsgPrinter.INSTANCE.errMsgForDebug(msg);
      throw new AssertionError(msg);
    }
    return copyOfNodeToExec;
  }
}
