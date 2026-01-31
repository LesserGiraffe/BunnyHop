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

package net.seapanda.bunnyhop.compiler.nodecollector;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.compiler.SourceSet;
import net.seapanda.bunnyhop.compiler.SymbolNames;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * {@link WorkspaceSet} からコンパイルの対象となるノード一式のスナップショットを作成する.
 *
 * @author K.Koike
 */
public class SourceNodeCollector {

  /** このワークスペースセットからコンパイル対象ノードを集める. */
  private final WorkspaceSet wss;
  private final CompileErrorNodeCache compileErrorNodeCache;
  private final MessageService msgService;
  private final SequencedSet<BhNode> selectedNodes = new LinkedHashSet<>();

  /**
   * コンストラクタ.
   *
   * @param wss このワークスペースセットからコンパイル対象ノードを集める
   * @param compileErrorNodeCache コンパイルエラーノードを管理するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト
   */
  public SourceNodeCollector(
      WorkspaceSet wss,
      CompileErrorNodeCache compileErrorNodeCache,
      MessageService msgService) {
    this.wss = wss;
    this.compileErrorNodeCache = compileErrorNodeCache;
    this.msgService = msgService;
    setEventHandlers();
  }

  private void setEventHandlers() {
    WorkspaceSet.CallbackRegistry cbRegistry = wss.getCallbackRegistry();
    cbRegistry.getOnNodeSelectionStateChanged().add(event -> {
      if (event.isSelected()) {
        selectedNodes.add(event.node());
      } else {
        selectedNodes.remove(event.node());
      }
    });
    cbRegistry.getOnNodeRemoved().add(event -> selectedNodes.remove(event.node()));
  }

  /**
   * 実行可能なノードを集める.
   * 返されるノードは, ワークスペースに存在するノードのディープコピー.
   *
   * @return コンパイル対象の全ノードとプログラム開始時に実行されるノードのセット
   */
  public Optional<SourceSet> collect() {
    return collectNodes();
  }

  /** コンパイルの対象となるノード一式のスナップショットを作成する. */
  private Optional<SourceSet> collectNodes() {
    if (compileErrorNodesExit()) {
      return Optional.empty();
    }
    BhNode mainEntryPoint = findMainEntryPoint();
    Set<BhNode> rootNodes = collectRootNodes(wss);
    boolean executableNodesExist =
        mainEntryPoint != null
        || rootNodes.stream()
        .map(SyntaxSymbol::getSymbolName)
        .anyMatch(SymbolNames.EntryPoint.AUTO_LIST::contains);

    if (!executableNodesExist) {
      msgService.alert(
          Alert.AlertType.ERROR,
          TextDefs.Compile.InformSelectNodeToExecute.title.get(),
          null,
          TextDefs.Compile.InformSelectNodeToExecute.body.get());
      return Optional.empty();
    }
    InstanceId mainEntryPointId = (mainEntryPoint == null) ? null : mainEntryPoint.getInstanceId();
    return Optional.of(new SourceSetSnapshot(mainEntryPointId, rootNodes));
  }

  /**
   * コンパイルエラーノードが存在するか調べる.
   *
   * @return コンパイルエラーノードがある場合 true.
   */
  private boolean compileErrorNodesExit() {
    if (compileErrorNodeCache.isEmpty()) {
      return false;
    }
    msgService.alert(
        Alert.AlertType.ERROR,
        TextDefs.Compile.InformCompileError.title.get(),
        null,
        TextDefs.Compile.InformCompileError.body.get(),
        ButtonType.OK);
    return true;
  }
  
  /**
   * プログラム開始時に実行するノードを探す.
   *
   * @return プログラム開始時に実行するノード.  存在しない場合 null.
   */
  private BhNode findMainEntryPoint() {
    if (selectedNodes.isEmpty()) {
      return null;
    }
    BhNode rootOfLastSelected = selectedNodes.getLast().findRootNode();
    return SymbolNames.EntryPoint.MAIN_LIST.contains(rootOfLastSelected.getSymbolName())
        ? rootOfLastSelected : null;
  }

  /** {@code wss} 以下のルートノードを集めて返す. */
  private Set<BhNode> collectRootNodes(WorkspaceSet wss) {
    return wss.getWorkspaces().stream()
        .flatMap(ws -> ws.getRootNodes().stream())
        .collect(Collectors.toCollection(HashSet::new));
  }
}
