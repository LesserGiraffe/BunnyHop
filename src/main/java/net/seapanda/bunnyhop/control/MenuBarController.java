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

package net.seapanda.bunnyhop.control;

import java.io.File;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * メニューバーのコントローラクラス.
 *
 * @author K.Koike
 */
public class MenuBarController {

  @FXML private MenuBar menuBar;
  @FXML private MenuItem load;
  @FXML private MenuItem save;
  @FXML private MenuItem saveAs;
  @FXML private MenuItem versionInfo;
  @FXML private MenuItem freeMemory;
  @FXML private MenuItem focusSimulator;
  /** 現在保存対象になっているファイル. */
  private File currentSaveFile;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private ModelAccessNotificationService notifService;
  private UndoRedoAgent undoRedoAgent;
  private ProjectImporter importer;
  private ProjectExporter exporter;
  private MessageService msgService;

  /** 初期化する. */
  public void initialize(
      WorkspaceSet wss,
      ModelAccessNotificationService notifService,
      UndoRedoAgent undoRedoAgent,
      ProjectImporter importer,
      ProjectExporter exporter,
      MessageService msgService) {
    this.notifService = notifService;
    this.undoRedoAgent = undoRedoAgent;
    this.importer = importer;
    this.exporter = exporter;
    this.msgService = msgService;
    saveAs.setOnAction(action -> saveAs(wss)); // セーブ(新規保存)
    save.setOnAction(action -> save(wss)); // 上書きセーブ
    load.setOnAction(action -> load(wss));
    freeMemory.setOnAction(action -> freeMemory());
    versionInfo.setOnAction(action -> showBunnyVersion());
    focusSimulator.setOnAction(action ->
        switchMenuSetting(focusSimulator, BhSettings.BhSimulator.focusOnStartBhProgram));
    if (BhSettings.BhSimulator.focusOnStartBhProgram.get()) {
      focusSimulator.setText(focusSimulator.getText() + " ✓");
    }
  }

  /**
   * 新規保存セーブを行う.
   *
   * @param wss 保存時に操作するワークスペースセット
   * @return 保存した場合true
   */
  private boolean saveAs(WorkspaceSet wss) {
    notifService.begin();
    try {
      if (wss.getWorkspaces().isEmpty()) {
        msgService.alert(
            Alert.AlertType.INFORMATION,
            TextDefs.Export.InformNoWsToSave.title.get(),
            null,
            TextDefs.Export.InformNoWsToSave.body.get());
        return false;
      }
      Optional<File> fileToSave = getFileToSave();
      boolean success = fileToSave.map(file -> exporter.export(file, wss)).orElse(false);
      if (success) {
        currentSaveFile = fileToSave.get();
      }
      return success;
    } finally {
      notifService.end();
    }
  }

  /**
   * 保存先のファイルを取得する.
   *
   * @return 保存先のファイル
   */
  private Optional<File> getFileToSave() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(TextDefs.Export.FileChooser.title.get());
    fileChooser.setInitialDirectory(getInitDir());
    fileChooser.getExtensionFilters().addAll(
      new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"),
      new FileChooser.ExtensionFilter("All Files", "*.*"));
    File selectedFile = fileChooser.showSaveDialog(menuBar.getScene().getWindow());
    return Optional.ofNullable(selectedFile);
  }

  /**
   * 上書きセーブを行う.
   *
   * @param wss 保存時に操作するワークスペースセット
   * @return 保存した場合true
   */
  public boolean save(WorkspaceSet wss) {
    notifService.begin();
    try {
      if (wss.getWorkspaces().isEmpty()) {
        msgService.alert(
            Alert.AlertType.INFORMATION,
            TextDefs.Export.InformNoWsToSave.title.get(),
            null,
            TextDefs.Export.InformNoWsToSave.body.get());
        return false;
      }

      boolean fileExists = false;
      if (currentSaveFile != null) {
        fileExists = currentSaveFile.exists();
      }
      if (fileExists) {
        return exporter.export(currentSaveFile, wss);
      } else {
        return saveAs(wss);  //保存対象のファイルが無い場合, 名前をつけて保存
      }
    } finally {
      notifService.end();
    }
  }

  /** ロードボタン押下時の処理. */
  private void load(WorkspaceSet wss) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(TextDefs.Import.FileChooser.title.get());
    fileChooser.setInitialDirectory(getInitDir());
    fileChooser.getExtensionFilters().addAll(
      new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"),
      new FileChooser.ExtensionFilter("All Files", "*.*"));
    File selectedFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow());
    if (selectedFile == null) {
      return;
    }
    Optional<Boolean> clearWs = askIfClearOldWs();
    if (clearWs.isEmpty()) {
      return;
    }
    Context context = notifService.begin();
    try {
      SequencedSet<Workspace> workspaces = wss.getWorkspaces();
      boolean success = importer.imports(selectedFile, wss, context.userOpe());
      if (success) {
        currentSaveFile = selectedFile;
        if (clearWs.get()) {
          workspaces.forEach(ws ->  wss.removeWorkspace(ws, context.userOpe()));
        }
      }
    } finally {
      notifService.end();
    }
  }

  /**
   * ファイル保存時の初期ディレクトリを返す.
   *
   * @return ファイル保存時の初期ディレクトリ
   */
  private File getInitDir() {
    if (currentSaveFile != null) {
      File parent = currentSaveFile.getParentFile();
      if (parent != null) {
        if (parent.exists()) {
          return parent;
        }
      }
    }
    return new File(Utility.execPath);
  }

  /**
   * ロード方法を確認する.
   *
   * @retval true 既存のワークスペースをすべて削除
   * @retval false 既存のワークスペースにロードしたワークスペースを追加
   * @retval empty どちらも選択されなかった場合
   */
  private Optional<Boolean> askIfClearOldWs() {
    String title = TextDefs.Import.AskIfClearOldWs.title.get();
    String body = TextDefs.Import.AskIfClearOldWs.body.get(
        ButtonType.YES.getText(), ButtonType.NO.getText());
    Optional<ButtonType> buttonType = msgService.alert(
        AlertType.CONFIRMATION,
        title,
        null,
        body,
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

    return buttonType.flatMap(type -> {
      if (type.equals(ButtonType.NO)) {
        return Optional.of(true);
      } else if (type.equals(ButtonType.YES)) {
        return Optional.of(false);
      }
      return Optional.empty();
    });
  }

  /** アプリケーションが使用しているメモリを開放する. */
  private void freeMemory() {
    notifService.begin();
    try {
      undoRedoAgent.deleteCommands();
      msgService.info(TextDefs.MenubarOps.freeMemory.get());
      System.gc();
    } finally {
      notifService.end();
    }
  }

  /** BunnyHop のバージョン情報を表示する. */
  private void showBunnyVersion() {
    msgService.alert(
        Alert.AlertType.INFORMATION,
        TextDefs.MenubarOps.Version.title.get(),
        null, String.format(
        """
        %s: %s
        %s: %s
        %s: %s
        %s: %s
        """,
        TextDefs.MenubarOps.Version.system.get(),
        BhConstants.SYS_VERSION,
        BhConstants.APP_NAME,
        BhConstants.APP_VERSION,
        TextDefs.MenubarOps.Version.runtime.get(),
        net.seapanda.bunnyhop.runtime.BhConstants.APP_VERSION,
        TextDefs.MenubarOps.Version.simulator.get(),
        net.seapanda.bunnyhop.simulator.BhConstants.APP_VERSION));
  }

  /** 有効/無効を切り替え可能なメニューの設定を変更する. */
  private static void switchMenuSetting(MenuItem menu, AtomicBoolean setting) {
    var val = setting.get();
    setting.set(!val);
    if (val) {
      menu.setText(menu.getText().replace("✓", ""));
    } else {
      menu.setText(menu.getText() + " ✓");
    }
  }

  /**
   * ユーザメニュー操作のイベントを起こす.
   *
   * @param op 基本操作を表す列挙子
   */
  public void fireEvent(MenuBarItem op) {
    switch (op) {
      case SAVE:
        save.fire();
        break;

      case SAVE_AS:
        saveAs.fire();
        break;

      case FREE_MEMORY:
        freeMemory.fire();
        break;

      default:
        throw new AssertionError("Invalid menu bar operation " + op);
    }
  }

  /** メニューバーの操作. */
  public enum MenuBarItem {
    SAVE,
    SAVE_AS,
    FREE_MEMORY,
  }
}
