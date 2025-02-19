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

package net.seapanda.bunnyhop.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.FutureTask;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

/**
 * アプリケーションユーザ向けにメッセージを出力する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class BhMessageService implements Closeable, MessageService {

  private TextArea mainMsgArea;
  private Queue<String> messages = new LinkedList<>();
  private final Timeline msgPrintTimer;
  private Collection<String> style = new ArrayList<>();
  private boolean isClosed = false;

  /** コンストラクタ. */
  public BhMessageService() throws IOException {
    var keyFrame = new KeyFrame(Duration.millis(100), event -> outputMsg());
    msgPrintTimer = new Timeline(keyFrame);
    msgPrintTimer.setCycleCount(Timeline.INDEFINITE);
    msgPrintTimer.play();
  }

  private synchronized void outputMsg() {
    if (isClosed || mainMsgArea == null) {
      return;
    }
    StringBuilder text = new StringBuilder("");
    messages.forEach(text::append);
    messages.clear();
    if (!text.isEmpty()) {
      mainMsgArea.appendText(text.toString());
    }
  }

  @Override
  public synchronized void info(String msg) {
    if (isClosed || mainMsgArea == null) {
      return;
    }
    if (Platform.isFxApplicationThread()) {
      mainMsgArea.appendText(msg);
    } else {
      messages.offer(msg);
    }
  }

  @Override
  public synchronized void error(String msg) {
    info(msg);
  }

  @Override
  public Optional<ButtonType> alert(
      Alert.AlertType type,
      String title,
      String header,
      String content,
      ButtonType ... buttonTypes) {
    if (isClosed) {
      return Optional.empty();
    }
    FutureTask<Optional<ButtonType>> alertTask = new FutureTask<>(() -> {
      Alert alert = new Alert(type);
      alert.setTitle(title);
      alert.setHeaderText(header);
      alert.setContentText(content);
      alert.getDialogPane().getStylesheets().addAll(style);
      if (buttonTypes.length != 0) {
        alert.getButtonTypes().setAll(buttonTypes);
      }
      return alert.showAndWait();
    });

    if (Platform.isFxApplicationThread()) {
      alertTask.run();
    } else {
      Platform.runLater(alertTask);
    }
    Optional<ButtonType> buttonType = Optional.empty();
    try {
      buttonType = alertTask.get();
    } catch (Exception e) { /* do nothing */ }
    return buttonType;
  }

  /**
   * メインのメッセージ出力エリアを登録する.
   *
   * @param mainMsgArea 登録するメインのメッセージ出力エリア
   */
  public synchronized void setMainMsgArea(TextArea mainMsgArea) {
    this.mainMsgArea = mainMsgArea;
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
