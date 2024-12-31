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
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
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
  @FXML private MenuItem aboutBunnyHop;
  @FXML private MenuItem freeMemory;
  @FXML private MenuItem focusSimulator;
  /** 現在保存対象になっているファイル. */
  private File currentSaveFile;

  /**
   * 初期化する.
   *
   * @param wss ワークスペースセット
   */
  public void initialize(WorkspaceSet wss) {
    saveAs.setOnAction(action -> saveAs(wss)); // セーブ(新規保存)
    save.setOnAction(action -> save(wss)); // 上書きセーブ
    load.setOnAction(action -> load(wss));
    freeMemory.setOnAction(action -> freeMemory());
    aboutBunnyHop.setOnAction(action -> showBunnyHopInfo());
    focusSimulator.setOnAction(action -> switchSimFocusSetting());
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
    if (wss.getWorkspaces().isEmpty()) {
      BhService.msgPrinter().alert(
          Alert.AlertType.INFORMATION,
          TextDefs.Export.InformNoWsToSave.title.get(),
          null,
          TextDefs.Export.InformNoWsToSave.body.get());
      return false;
    }
    Optional<File> fileToSave = getFileToSave();
    boolean success = fileToSave.map(file -> wss.save(file)).orElse(false);
    if (success) {
      currentSaveFile = fileToSave.get();
    }
    return success;
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
    if (wss.getWorkspaces().isEmpty()) {
      BhService.msgPrinter().alert(
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
      return wss.save(currentSaveFile);
    } else {
      return saveAs(wss);  //保存対象のファイルが無い場合, 名前をつけて保存
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
    boolean success =
        askIfClearOldWs().map(clearWs -> wss.load(selectedFile, clearWs)).orElse(false);
    if (success) {
      currentSaveFile = selectedFile;
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
    Optional<ButtonType> buttonType = BhService.msgPrinter().alert(
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
    ModelExclusiveControl.lockForModification();
    try {
      BhService.undoRedoAgent().deleteCommands();
      BhService.msgPrinter().infoForUser(TextDefs.MenubarOps.freeMemory.get());
      System.gc();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** BunnyHopの基本情報を表示する. */
  private void showBunnyHopInfo() {
    BhService.msgPrinter().alert(
        Alert.AlertType.INFORMATION,
        TextDefs.MenubarOps.bunnyHopDetails.get(),
        null,
        "Version: " + BhConstants.APP_VERSION);
  }

  /** BhProgram 実行時にシミュレータにフォーカスするかどうかを切り替える. */
  private void switchSimFocusSetting() {
    var val = BhSettings.BhSimulator.focusOnStartBhProgram.get();
    BhSettings.BhSimulator.focusOnStartBhProgram.set(!val);
    if (val) {
      focusSimulator.setText(focusSimulator.getText().replace("✓", ""));
    } else {
      focusSimulator.setText(focusSimulator.getText() + " ✓");
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
