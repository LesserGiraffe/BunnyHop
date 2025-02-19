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

import java.util.Optional;
import java.util.SequencedSet;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.bhprogram.ExecutableNodeSet;
import net.seapanda.bunnyhop.bhprogram.ExecutableNodeSnapshot;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * {@link WorkspaceSet} から実行可能なノード一式のスナップショットを作成する.
 *
 * @author K.Koike
 */
public class ExecutableNodeCollector {

  /** このワークスペースセットからコンパイル対象ノードを集める. */
  private final WorkspaceSet wss;
  private final MessageService msgService;
  private final UserOperation userOpe;

  private ExecutableNodeCollector(
      WorkspaceSet wss, MessageService msgService, UserOperation userOpe) {
    this.wss = wss;
    this.msgService = msgService;
    this.userOpe = userOpe;
  }

  /**
   * 実行可能なノードを集める.
   * 返されるノードは, ワークスペースに存在するノードのディープコピー.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める.
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   * @param userOpe undo 用コマンドオブジェクト
   * @return コンパイル対象の全ノードとプログラム開始時に実行されるノードのセット
   */
  public static Optional<ExecutableNodeSet> collect(
      WorkspaceSet wss, MessageService msgService, UserOperation userOpe) {
    return new ExecutableNodeCollector(wss, msgService, userOpe).collectNodes();
  }

  /**
   * コンパイル対象のノードを集める.
   *
   * @return コンパイル対象ノードと実行対象ノードのペア
   */
  private Optional<ExecutableNodeSet> collectNodes() {
    if (!deleteCompileErrorNodes()) {
      return Optional.empty();
    }
    return findNodeToExecute(wss.getCurrentWorkspace())
        .map(nodeToExec -> new ExecutableNodeSnapshot(wss, nodeToExec));
  }

  /**
   * コンパイルエラーノードを削除する.
   *
   * @return 全てのコンパイルエラーノードが無くなった場合 true.
   */
  private boolean deleteCompileErrorNodes() {
    SequencedSet<BhNode> compileErrNodes = wss.getCompileErrNodes();
    if (compileErrNodes.isEmpty()) {
      return true;
    }
    Optional<ButtonType> btnType = msgService.alert(
        Alert.AlertType.CONFIRMATION,
        TextDefs.Compile.AskIfDeleteErrNodes.title.get(),
        null,
        TextDefs.Compile.AskIfDeleteErrNodes.body.get(),
        ButtonType.NO,
        ButtonType.YES);

    if (btnType.isEmpty()) {
      return false;
    }
    return btnType
        .map(type -> {
          if (type.equals(ButtonType.YES)) {
            BhNodePlacer.deleteNodes(compileErrNodes, userOpe);
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
   * @return 実行対象のノード
   */
  private Optional<BhNode> findNodeToExecute(Workspace ws) {
    if (ws == null) {
      return Optional.empty();
    }
    SequencedSet<BhNode> selectedNodeList = ws.getSelectedNodes();
    if (selectedNodeList.isEmpty()) {
      msgService.alert(
          AlertType.ERROR,
          TextDefs.Compile.InformSelectNodeToExecute.title.get(),
          null,
          TextDefs.Compile.InformSelectNodeToExecute.body.get());
      return Optional.empty();
    }
    // 実行対象以外を非選択に.
    BhNode nodeToExec = selectedNodeList.getFirst().findRootNode();
    for (BhNode selectedNode : ws.getSelectedNodes()) {
      selectedNode.deselect(userOpe);
    }
    nodeToExec.select(userOpe);
    return Optional.of(nodeToExec);
  }
}
