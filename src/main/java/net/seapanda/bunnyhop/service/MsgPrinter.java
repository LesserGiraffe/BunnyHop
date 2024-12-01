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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.utility.LogManager;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * メッセージ出力クラス.
 *
 * @author K.Koike
 */
public class MsgPrinter implements Closeable {

  private TextArea mainMsgArea;
  private Queue<String> messages = new LinkedList<>();
  private final Timeline msgPrintTimer;
  private final LogManager logManager;
  private Collection<String> style = new ArrayList<>();
  private boolean isClosed = false;


  MsgPrinter() throws IOException {
    logManager = new LogManager(
      Paths.get(Utility.execPath, BhConstants.Path.LOG_DIR),
      BhConstants.Path.LOG_FILE_NAME,
      BhConstants.Message.LOG_FILE_SIZE_LIMIT,
      BhConstants.Message.MAX_LOG_FILE_NUM);
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

  /** デバッグ用エラーメッセージ出力メソッド. */
  public synchronized void errForDebug(String msg) {
    if (isClosed) {
      return;
    }
    Date date = Calendar.getInstance().getTime();
    msg = "[ERR] : %s @ %s\n%s\n----\n".formatted(
        Utility.getMethodName(2),
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date),
        msg);
    System.err.print(msg);
    logManager.writeToFile(msg);
  }

  /** デバッグ用メッセージ出力メソッド. */
  public synchronized void infoForDebug(String msg) {
    if (isClosed) {
      return;
    }
    Date date = Calendar.getInstance().getTime();
    msg = "[INFO] : %s @ %s\n%s\n----\n".formatted(
        Utility.getMethodName(2),
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date),
        msg);
    System.out.print(msg);
    logManager.writeToFile(msg);
  }

  /** アプリケーションユーザ向けにメッセージを出力する. */
  public synchronized void infoForUser(String msg) {
    if (isClosed || mainMsgArea == null) {
      return;
    }
    if (Platform.isFxApplicationThread()) {
      mainMsgArea.appendText(msg);
    } else {
      messages.offer(msg);
    }
  }

  /** アプリケーションユーザ向けにメッセージを出力する. */
  public synchronized void infoForUser(List<Character> charCodeList) {
    char[] charCodeArray = new char[charCodeList.size()];
    for (int i = 0; i < charCodeArray.length; ++i) {
      charCodeArray[i] = charCodeList.get(i);
    }
    infoForUser(new String(charCodeArray));
  }

  /** アプリケーションユーザ向けにエラーメッセージを出力する. */
  public synchronized void errForUser(String msg) {
    infoForUser(msg);
  }

  /** 標準出力にメッセージを出力する. */
  public synchronized void println(String msg) {
    if (isClosed) {
      return;
    }
    System.out.println(msg);
  }

  /**
   * アラーウィンドウでメッセージを出力する.
   *
   * @param type アラートの種類
   * @param title アラートウィンドウのタイトル (nullable)
   * @param header アラートウィンドウのヘッダ (nullable)
   * @param content アラートウィンドウの本文 (nullable)
   * @param buttonTypes 表示するボタン (nullable)
   * @return メッセージに対して選択されたボタン
   */
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
    } catch (Exception e) {
      errForDebug(e.toString());
    }
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
    logManager.close();
    msgPrintTimer.stop();
  }
}
