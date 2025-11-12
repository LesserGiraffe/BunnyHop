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

package net.seapanda.bunnyhop.bhprogram;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * {@link WorkspaceSet} から実行可能なノード一式のスナップショットを作成する.
 *
 * @author K.Koike
 */
public class ExecutableNodeCollector {

  /** このワークスペースセットからコンパイル対象ノードを集める. */
  private final WorkspaceSet wss;
  private final CompileErrorNodeCache compileErrorNodeCache;
  private final MessageService msgService;


  /**
   * コンストラクタ.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める
   * @param compileErrorNodeCache コンパイルエラーノードを管理するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト
   */
  public ExecutableNodeCollector(
      WorkspaceSet wss,
      CompileErrorNodeCache compileErrorNodeCache,
      MessageService msgService) {
    this.wss = wss;
    this.compileErrorNodeCache = compileErrorNodeCache;
    this.msgService = msgService;
  }

  /**
   * 実行可能なノードを集める.
   * 返されるノードは, ワークスペースに存在するノードのディープコピー.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return コンパイル対象の全ノードとプログラム開始時に実行されるノードのセット
   */
  public Optional<ExecutableNodeSet> collect(UserOperation userOpe) {
    return collectNodes(userOpe);
  }

  /**
   * コンパイル対象のノードを集める.
   *
   * @return コンパイル対象ノードと実行対象ノードのペア
   */
  private Optional<ExecutableNodeSet> collectNodes(UserOperation userOpe) {
    if (!deleteCompileErrorNodes(userOpe)) {
      return Optional.empty();
    }
    BhNode entryPoint = findEntryPointNode(wss.getCurrentWorkspace(), userOpe);
    if (BhSettings.BhProgram.entryPointMustExist && entryPoint == null) {
      msgService.alert(
          Alert.AlertType.ERROR,
          TextDefs.Compile.InformSelectNodeToExecute.title.get(),
          null,
          TextDefs.Compile.InformSelectNodeToExecute.body.get());
      return Optional.empty();
    }
    return Optional.of(new ExecutableNodeSnapshot(wss, entryPoint));
  }

  /**
   * コンパイルエラーノードを削除する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return 全てのコンパイルエラーノードが無くなった場合 true.
   */
  private boolean deleteCompileErrorNodes(UserOperation userOpe) {
    if (compileErrorNodeCache.isEmpty()) {
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
            return deleteCompileErrorNodes(compileErrorNodeCache.getCompileErrorNodes(), userOpe);
          }
          return false;
        })
        .orElse(false);
  }

  /** コンパイルエラーノードを削除する. */
  private boolean deleteCompileErrorNodes(Set<BhNode> nodes, UserOperation userOpe) {
    var nodesToDelete = nodes.stream()
        .filter(node -> node.getEventInvoker().onDeletionRequested(
            new ArrayList<>(nodes), CauseOfDeletion.COMPILE_ERROR, userOpe))
        .toList();
    List<Swapped> swappedNodes = BhNodePlacer.deleteNodes(nodesToDelete, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
    if (nodesToDelete.size() != nodes.size()) {
      LogManager.logger().error("Cannot delete compile error nodes");
      msgService.error(TextDefs.Compile.cannotDeleteErrorNodes.get());
      return false;
    }
    return true;
  }
  
  /**
   * プログラム開始時に実行するノードを探す.
   *
   * @param ws このワークスペースに実行対象があるかどうかチェックする.
   * @param userOpe undo 用コマンドオブジェクト
   * @return プログラム開始時に実行するノード.  存在しない場合 null.
   */
  private BhNode findEntryPointNode(Workspace ws, UserOperation userOpe) {
    if (ws == null) {
      return null;
    }
    SequencedSet<BhNode> selectedNodeList = ws.getSelectedNodes();
    if (selectedNodeList.isEmpty()) {
      return null;
    }
    // 実行対象以外を非選択に.
    BhNode entryPoint = selectedNodeList.getFirst().findRootNode();
    ws.getSelectedNodes().forEach(node -> node.deselect(userOpe));
    entryPoint.select(userOpe);
    return entryPoint;
  }
}
