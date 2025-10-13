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

package net.seapanda.bunnyhop.export;

import static net.seapanda.bunnyhop.export.JsonProjectReader.Result;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.model.factory.WorkspaceFactory;

/**
 * JSON ファイルからプロジェクトを読みだして, ワークスペースセットに追加する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public class JsonProjectImporter implements ProjectImporter {

  /** {@link BhNode} 生成用オブジェクト. */
  private final BhNodeFactory nodeFactory;
  /** {@link Workspace} 生成用オブジェクト. */
  private final WorkspaceFactory wsFactory;
  private final MessageService msgService;

  /** コンストラクタ. */
  public JsonProjectImporter(
      BhNodeFactory nodeFactory, WorkspaceFactory wsFactory, MessageService msgService) {
    this.nodeFactory = nodeFactory;
    this.wsFactory = wsFactory;
    this.msgService = msgService;
  }

  @Override
  public boolean imports(
      File saveFile, WorkspaceSet wss, boolean replaceAll, UserOperation userOpe) {
    try {
      Result tmp = JsonProjectReader.imports(saveFile.toPath(), nodeFactory, wsFactory);
      Result result = tmp;
      if (!replaceAll) {
        result = replaceDuplicateInstIds(wss, tmp.instanceIdToNode())
            .map(msg -> new Result(tmp, msg, ImportWarning.DUPLICATE_INSTANCE_ID))
            .orElse(tmp);
      }
      if (!result.warnings().isEmpty()) {
        LogManager.logger().error(result.warningMsg());
      }
      boolean continueLoading = result.warnings().isEmpty()
          || askIfContinueLoading(createWarningMsg(result.warnings()));
      if (!continueLoading) {
        return false;
      }
      SequencedSet<Workspace> workspaces = replaceAll ? wss.getWorkspaces() : new LinkedHashSet<>();
      addModelsInOrder(wss, result.workspaces(), userOpe);
      workspaces.forEach(ws -> wss.removeWorkspace(ws, userOpe));
      return true;

    } catch (IncompatibleSaveFormatException e) {
      String msg = TextDefs.Import.Error.unsupportedSaveDataVersion.get(
          e.version, BhConstants.SAVE_DATA_VERSION);
      outputLoadErrMsg(saveFile, e, msg);
      return false;

    } catch (CorruptedSaveDataException | JsonSyntaxException e) {
      String msg = TextDefs.Import.Error.corruptedSaveData.get();
      outputLoadErrMsg(saveFile, e, msg);
      return false;

    } catch (Exception e) {
      String msg = TextDefs.Import.Error.failedToReadSaveFile.get();
      outputLoadErrMsg(saveFile, e, msg);
      return false;
    }
  }

  /**
   * ロードを続けるか確認する.
   *
   * @param warningMsg 警告の詳細な情報
   * @return ロードを続ける 場合 true, 止める場合 false
   */
  private Boolean askIfContinueLoading(String warningMsg) {
    String title = TextDefs.Import.AskIfContinue.title.get();
    String body = TextDefs.Import.AskIfContinue.body.get(
        ButtonType.YES.getText(), ButtonType.NO.getText());
    Optional<ButtonType> buttonType = msgService.alert(
        AlertType.CONFIRMATION,
        title,
        warningMsg,
        body,
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

    return buttonType.map(type -> type.equals(ButtonType.YES)).orElse(false);
  }

  /**
   * ワークスぺース -> ノードという順番で, 各モデルが追加されたときのイベントハンドラが呼ばれるように
   * ワークスペースとノードをワークスペースセットに追加する.
   */
  private void addModelsInOrder(
      WorkspaceSet wss, List<Workspace> workspaces, UserOperation userOpe) {
    // ワークスペースから全てのルートノードを取り除く.
    var wsToRootNodes = new HashMap<Workspace, List<BhNode>>();
    var tmp = new UserOperation();
    for (Workspace ws : workspaces) {
      var rootNodes = new ArrayList<BhNode>();
      for (BhNode root : ws.getRootNodes()) {
        rootNodes.add(root);
        // ワークスペースからのノードの削除は undo / redo の対象にしない.
        ws.removeNodeTree(root, tmp);
      }
      wsToRootNodes.put(ws, rootNodes);
    }
    // ワークスペースだけ先にワークスペースセットに追加する
    workspaces.forEach(ws -> wss.addWorkspace(ws, userOpe));
    // ワークスペースにルートノードを追加する
    workspaces.forEach(ws -> wsToRootNodes.get(ws)
        .forEach(root -> ws.addNodeTree(root, userOpe)));
  }

  /** ロード失敗時のエラーメッセージを出力する. */
  private void outputLoadErrMsg(File saveFile, Exception e, String msg) {
    msg += "\n" + saveFile.getAbsolutePath();
    String title = TextDefs.Import.Error.title.get();
    LogManager.logger().error(e.toString());
    msgService.alert(Alert.AlertType.INFORMATION, title, null, msg);
  }

  /**
   *  {@code instIdToNode} のキーが {@code wss} に存在する {@link BhNode} の {@link InstanceId} と一致する場合,
   *  {@code instIdToNode} 上でそのキーに対応する {@link BhNode} の {@link InstanceId} を新規作成したものに変更する.
   *
   * @return {@link InstanceId} の重複があった場合, その詳細なメッセージ.  そうでない場合 empty.
   */
  private static Optional<String> replaceDuplicateInstIds(
      WorkspaceSet wss, Map<InstanceId, BhNode> instIdToNode) {
    Set<InstanceId> instanceIds = collectInstIds(wss);
    Map<InstanceId, BhNode> orgInstIdToNode = replaceDuplicateInstIds(instanceIds, instIdToNode);
    if (orgInstIdToNode.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(createDuplicateInstIdWarnDetails(orgInstIdToNode));
  }

  /**
   *  {@code instIdToNode} のキーが {@code existing} に含まれる場合,
   * そのキーに対応する {@link BhNode} の {@link InstanceId} を新規作成したものに変更する.
   *
   * @return 変更前の {@link InstanceId} と {@link InstanceId} を変更された {@link BhNode} のマップ.
   */
  private static Map<InstanceId, BhNode> replaceDuplicateInstIds(
      Set<InstanceId> existing, Map<InstanceId, BhNode> instIdToNode) {
    var orgInstIdToNode = new HashMap<InstanceId, BhNode>();
    for (Map.Entry<InstanceId, BhNode> instIdAndNode : instIdToNode.entrySet()) {
      if (existing.contains(instIdAndNode.getKey())) {
        instIdAndNode.getValue().setInstanceId(InstanceId.newId());
        orgInstIdToNode.put(instIdAndNode.getKey(), instIdAndNode.getValue());
      }
    }
    return orgInstIdToNode;
  }

  /** {@code wss} に存在する全ての {@link BhNode} の {@link InstanceId} を取得する. */
  private static Set<InstanceId> collectInstIds(WorkspaceSet wss) {
    var symbolIds = new HashSet<InstanceId>();
    var registry = CallbackInvoker.newCallbackRegistry()
        .setForAllNodes(node -> symbolIds.add(node.getInstanceId()));

    wss.getWorkspaces().stream()
        .flatMap(ws -> ws.getRootNodes().stream())
        .forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
    return symbolIds;
  }

  /**
   * {@link InstanceId} の重複に関する詳細を作成する.
   *
   * @param instIdToNode 重複した {@link InstanceId} とそれを保持していた {@link BhNode} のマップ.
   * @return {@link InstanceId} の重複に関する詳細
   */
  private static String createDuplicateInstIdWarnDetails(Map<InstanceId, BhNode> instIdToNode) {
    if (instIdToNode.isEmpty()) {
      return "";
    }
    StringBuilder msg = new StringBuilder();
    msg.append(ImportWarning.DUPLICATE_INSTANCE_ID.toString() + "\n");
    for (Map.Entry<InstanceId, BhNode> orgInstIdAndNode : instIdToNode.entrySet()) {
      msg.append("  instance id: %s,  node id %s\n".formatted(
          orgInstIdAndNode.getKey(), orgInstIdAndNode.getValue().getId()));
    }
    msg.append("\n");
    return msg.toString();
  }

  /** ロード時にユーザに表示する警告メッセージを作成する. */
  private static String createWarningMsg(Set<ImportWarning> warnings) {
    var msgs = new ArrayList<String>();
    if (warnings.contains(ImportWarning.UNKNOWN_BH_NODE_ID)
        || warnings.contains(ImportWarning.INCOMPATIBLE_BH_NODE_VERSION)
        || warnings.contains(ImportWarning.CONNECTOR_NOT_FOUND)
        || warnings.contains(ImportWarning.DERIVATIVE_NOT_FOUND)) {
      msgs.add(TextDefs.Import.AskIfContinue.someNodesAreMissing.get());
    }
    if (warnings.contains(ImportWarning.DUPLICATE_INSTANCE_ID)) {
      msgs.add(TextDefs.Import.AskIfContinue.overwroteDuplicateInstIds.get());
    }
    return String.join("\n", msgs);
  }
}
