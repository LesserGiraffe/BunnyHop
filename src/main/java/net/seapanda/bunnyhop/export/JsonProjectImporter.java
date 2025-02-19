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

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactory;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UserOperation;

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
  public boolean imports(File saveFile, WorkspaceSet wss, UserOperation userOpe) {
    try {
      JsonProjectReader.Result result = 
          JsonProjectReader.imports(saveFile.toPath(), nodeFactory, wsFactory);
      if (!result.warnings().isEmpty()) {
        LogManager.logger().error(result.warningMsg());
      }
      boolean continueLoading = result.warnings().isEmpty() || askIfContinueLoading();
      if (!continueLoading) {
        return false;
      }
      addModelsInOrder(wss, result.workspaces(), userOpe);
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
   * ロードを中断するか確認する.
   *
   * @retval true ロードを中断する
   * @retval false 既存のワークスペースにロードしたワークスペースを追加
   */
  private Boolean askIfContinueLoading() {
    String title = TextDefs.Import.AskIfContinue.title.get();
    String body = TextDefs.Import.AskIfContinue.body.get(
        ButtonType.YES.getText(), ButtonType.NO.getText());
    Optional<ButtonType> buttonType = msgService.alert(
        AlertType.CONFIRMATION,
        title,
        null,
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
    for (Workspace ws : workspaces) {
      var rootNodes = new ArrayList<BhNode>();
      for (BhNode root : ws.getRootNodes()) {
        rootNodes.add(root);
        ws.removeNodeTree(root, userOpe);
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
}
