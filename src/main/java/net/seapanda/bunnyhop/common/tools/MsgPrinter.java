/**
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
package net.seapanda.bunnyhop.common.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.root.BunnyHop;

/**
 * メッセージ出力クラス
 * @author K.Koike
 * */
public class MsgPrinter {

	public static final MsgPrinter INSTANCE = new MsgPrinter();	//!< シングルトンインスタンス
	private TextArea mainMsgArea;
	private BlockingQueue<String> queuedMsgs = new ArrayBlockingQueue<>(BhParams.Message.MAX_MAIN_MSG_QUEUE_SIZE);
	private Timeline msgPrintTimer;

	private MsgPrinter() {}

	public boolean init() {
		startMsgTimer();
		return LogManager.INSTANCE.init();
	}

	/**
	 * メッセージ出力タイマーを駆動する
	 */
	private void startMsgTimer() {

		msgPrintTimer = new Timeline(
			new KeyFrame(
				Duration.millis(100),
				(event) ->{
					if (mainMsgArea == null)
						return;

					List<String> msgList = new ArrayList<>(queuedMsgs.size());
					queuedMsgs.drainTo(msgList);
					StringBuilder text = new StringBuilder();
					msgList.forEach(msg -> text.append(msg));
					if (!msgList.isEmpty()) {
						mainMsgArea.appendText(text.toString());
					}
				}));
		msgPrintTimer.setCycleCount(Timeline.INDEFINITE);
		msgPrintTimer.play();
	}

	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void errMsgForDebug(String msg) {
		msg = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(Calendar.getInstance().getTime()) + "  ERR : " + msg;
		System.err.print(msg + "\n");
		LogManager.INSTANCE.writeMsgToLogFile(msg + "\n");
	}

	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void msgForDebug(String msg) {
		msg = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(Calendar.getInstance().getTime()) + "  MSG : " + msg;
		System.out.print(msg + "\n");
		LogManager.INSTANCE.writeMsgToLogFile(msg + "\n");
	}

	/**
	 * BHユーザ向けにメッセージを出力する
	 */
	public void msgForUser(String msg) {

		if (Platform.isFxApplicationThread()) {
			mainMsgArea.appendText(msg);
		}
		else {
			try {
				queuedMsgs.put(msg);
			}
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * BHユーザ向けにメッセージを出力する
	 */
	public void msgForUser(List<Character> charCodeList) {

		if (charCodeList.isEmpty())
			return;

		char[] charCodeArray = new char[charCodeList.size()];
		for (int i = 0; i < charCodeArray.length; ++i)
			charCodeArray[i] = charCodeList.get(i);
		String progressInfo = new String(charCodeArray);	//サイズ0の配列の場合 readStr == '\0'
		msgForUser(progressInfo + "\n");
	}

	/**
	 * BHユーザ向けにエラーメッセージを出力する
	 */
	public void errMsgForUser(String msg) {
		msgForUser(msg);
	}

	/**
	 * アラーウィンドウでメッセージを出力する
	 * @param type アラートの種類
	 * @param title アラートウィンドウのタイトル
	 * @param header アラートウィンドウのヘッダ
	 * @param content アラートウィンドウの本文
	 * @param buttonTypes 表示するボタン
	 * @return メッセージに対して選択されたボタン
	 */
	public Optional<ButtonType> alert(
			Alert.AlertType type,
			String title,
			String header,
			String content,
			ButtonType ...buttonTypes) {

		FutureTask<Optional<ButtonType>> alertTask = new FutureTask<>(() -> {
			Alert alert = new Alert(type);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(content);
			alert.getDialogPane().getStylesheets().addAll(BunnyHop.INSTANCE.getAllStyles());
			if (buttonTypes.length != 0)
				alert.getButtonTypes().setAll(buttonTypes);
			return alert.showAndWait();
		});

		if (Platform.isFxApplicationThread())
			alertTask.run();
		else
			Platform.runLater(alertTask);

		Optional<ButtonType> buttonType = Optional.empty();
		try {
			buttonType = alertTask.get();
		}
		catch(Exception e) {
			errMsgForDebug(e.toString());
		}
		return buttonType;
	}

	/**
	 * メインのメッセージ出力エリアを登録する
	 * @param mainMsgArea 登録するメインのメッセージ出力エリア
	 */
	public void setMainMsgArea(TextArea mainMsgArea) {
		this.mainMsgArea = mainMsgArea;
	}

	/**
	 * 終了処理をする
	 */
	public void end() {
		LogManager.INSTANCE.end();
	}

	/**
	 * BunnyHopのメッセージ出力機能を止める
	 */
	public void stop() {
		msgPrintTimer.stop();
	}
}
