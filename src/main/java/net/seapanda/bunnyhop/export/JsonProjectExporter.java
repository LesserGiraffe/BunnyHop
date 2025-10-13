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

import java.io.File;
import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
  * ワークスペースセットの情報を JSON 形式で出力する機能を規定したインタフェース.
  *
  * @author K.KOike
  */
public class JsonProjectExporter implements ProjectExporter {

  private final MessageService msgService;

  /** コンストラクタ. */
  public JsonProjectExporter(MessageService msgService) {
    this.msgService = msgService;
  }

  @Override
  public boolean export(File saveFile, WorkspaceSet wss) {
    try {
      JsonProjectWriter.export(wss.getWorkspaces(), saveFile.toPath());
      msgService.info(TextDefs.Export.hasSaved.get(saveFile.getPath()));
      wss.setDirty(false);
      return true;
    } catch (Exception e) {
      LogManager.logger().error(
          "Failed to save the project.\n%s\n%s".formatted(saveFile.getPath(), e));
      msgService.alert(
          Alert.AlertType.ERROR,
          TextDefs.Export.InformFailedToSave.title.get(),
          null,
          saveFile.getPath() + "\n" + e);
      return false;
    }
  }
}
