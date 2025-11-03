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

package net.seapanda.bunnyhop.service.message;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.SequencedCollection;
import java.util.concurrent.FutureTask;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import net.seapanda.bunnyhop.ui.view.ViewUtil;

/**
 * アプリケーションユーザ向けにメッセージを出力する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class BhMessageService implements Closeable, MessageService {

  private TextInputControl textInputCtrl;
  private final Queue<String> messages = new LinkedList<>();
  private final Timeline msgPrintTimer;
  private volatile SequencedCollection<Stage> stages = new ArrayList<>();
  private volatile Collection<String> style = new ArrayList<>();
  private volatile boolean isClosed = false;

  /** コンストラクタ. */
  public BhMessageService() throws IOException {
    var keyFrame = new KeyFrame(Duration.millis(100), event -> outputMsg());
    msgPrintTimer = new Timeline(keyFrame);
    msgPrintTimer.setCycleCount(Timeline.INDEFINITE);
    msgPrintTimer.play();
  }

  private synchronized void outputMsg() {
    if (isClosed || textInputCtrl == null) {
      return;
    }
    StringBuilder text = new StringBuilder();
    messages.forEach(text::append);
    messages.clear();
    if (!text.isEmpty()) {
      textInputCtrl.appendText(text.toString());
    }
  }

  @Override
  public synchronized void info(String msg) {
    if (isClosed || textInputCtrl == null) {
      return;
    }
    messages.offer(msg);
  }

  @Override
  public synchronized void error(String msg) {
    info(msg);
  }

  @Override
  public Optional<ButtonType> alert(
      Alert.AlertType type,
      Modality modality,
      String title,
      String header,
      String content,
      ButtonType... buttonTypes) {
    if (isClosed) {
      return Optional.empty();
    }
    FutureTask<Optional<ButtonType>> alertTask = new FutureTask<>(
        () -> showAlertWindow(type, modality, title, header, content, buttonTypes));
    ViewUtil.runSafe(alertTask);
    try {
      return alertTask.get();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<ButtonType> alert(
      Alert.AlertType type,
      String title,
      String header,
      String content,
      ButtonType... buttonTypes) {
    return alert(type, Modality.APPLICATION_MODAL, title, header, content, buttonTypes);
  }

  /** GUI に対する操作の有効 / 無効を切り替える. */
  private void setUiEnablement(boolean val) {
    for (Stage stage : stages) {
      Optional.ofNullable(stage)
          .map(Window::getScene)
          .map(Scene::getRoot)
          .ifPresent(node -> node.setDisable(!val));
    }
  }

  /** アラートウィンドウを表示する. */
  private Optional<ButtonType> showAlertWindow(
      Alert.AlertType type,
      Modality modality,
      String title,
      String header,
      String content,
      ButtonType... buttonTypes) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.getDialogPane().getStylesheets().addAll(style);
    alert.initModality(modality);
    if (!stages.isEmpty()) {
      ((Stage) alert.getDialogPane().getScene().getWindow()).initOwner(stages.getFirst());
    }
    if (modality != Modality.APPLICATION_MODAL) {
      setUiEnablement(false);
    }
    if (buttonTypes.length != 0) {
      alert.getButtonTypes().setAll(buttonTypes);
    }
    Optional<ButtonType> chosen = alert.showAndWait();
    if (modality != Modality.APPLICATION_MODAL) {
      setUiEnablement(true);
    }
    return chosen;
  }

  /**
   * メッセージ出力先を登録する.
   *
   * @param ctrl メッセージ出力先
   */
  public synchronized void setMainMsgArea(TextInputControl ctrl) {
    this.textInputCtrl = ctrl;
  }

  /**
   * アプリケーションを構成する {@link Stage} を登録する.
   *
   * <p>登録した {@link Stage} は {@link Modality#APPLICATION_MODAL} を指定せずにダイアログを表示する際に
   * ウィンドウ全体の操作を受け付けなくするために使用する.
   *
   * @param primaryStage アプリケーションのメインとなるステージ
   * @param stages アプリケーションを構成する {@code primaryStage} 以外のステージ
   */
  public void setAppStages(Stage primaryStage, Stage... stages) {
    if (primaryStage == null) {
      return;
    }
    var tmp = new ArrayList<>(List.of(primaryStage));
    tmp.addAll(List.of(stages));
    this.stages = tmp;
  }

  /** ウィンドウを使うメッセージ出力のスタイルをセットする. */
  public synchronized void setWindowStyle(Collection<String> style) {
    if (style != null) {
      this.style = new ArrayList<>(style);
    }
  }

  /** 終了処理をする. */
  @Override
  public synchronized void close() {
    isClosed = true;
    msgPrintTimer.stop();
  }
}
