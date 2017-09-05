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
package pflab.bunnyhop.root;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import pflab.bunnyhop.common.BhParams;

/**
 * メッセージ出力クラス
 * @author K.Koike
 * */
public class MsgPrinter {

	public static final MsgPrinter instance = new MsgPrinter();	//!< シングルトンインスタンス
	private TextArea mainMsgArea;
	private BlockingQueue<String> queuedMsgs = new ArrayBlockingQueue<>(BhParams.MAX_MAIN_MSG_QUEUE_SIZE);
	private final Timeline msgPrintTimer;
	
	private MsgPrinter() {

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
	public void ErrMsgForDebug(String msg) {
		System.err.println("ERR : " + msg);
	}

	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void MsgForDebug(String msg) {
		System.out.println("DBG : " + msg);
	}
	
	/**
	 * BHユーザ向けにメッセージを出力する
	 */
	public void MsgForUser(String msg) {
		
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
	 * BHユーザ向けにエラーメッセージを出力する
	 */
	public void ErrMsgForUser(String msg) {
		MsgForUser(msg);
	}

	/**
	 * アラーウィンドウでメッセージを出力する
	 * @param type アラートの種類
	 * @param title アラートウィンドウのタイトル
	 * @param header アラートウィンドウのヘッダ
	 * @param content アラートウィンドウの本文
	 */
	public void alert(Alert.AlertType type, String title, String header, String content) {
		
		if (Platform.isFxApplicationThread()) {
			Alert alert = new Alert(type);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(content);
			alert.showAndWait();	
		}
		else {
			Platform.runLater(() -> {
				alert(type, title, header, content);
			});
		}
	}
	
	/**
	 * メインのメッセージ出力エリアを登録する
	 * @param mainMsgArea 登録するメインのメッセージ出力エリア
	 */
	public void setMainMsgArea(TextArea mainMsgArea) {		
		this.mainMsgArea = mainMsgArea;
	}
	
	/**
	 * BunnyHopのメッセージ出力機能を止める
	 */
	public void stop() {
		msgPrintTimer.stop();
	}
}
