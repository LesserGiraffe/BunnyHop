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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import pflab.bunnyhop.common.Util;

/**
 * メッセージ出力クラス
 * @author K.Koike
 * */
public class MsgPrinter {

	public static final MsgPrinter instance = new MsgPrinter();	//!< シングルトンインスタンス
	private TextArea mainMsgArea;
	private BlockingQueue<String> queuedMsgs = new ArrayBlockingQueue<>(BhParams.MAX_MAIN_MSG_QUEUE_SIZE);
	private Timeline msgPrintTimer;
	private OutputStream logOutputStream;
	
	private MsgPrinter() {}
		
	public boolean init() {
		startMsgTimer();
		initLogSystem();	//ログシステムがエラーでも処理は続ける
		return true;
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
	 * ログ機能を初期化する
	 */
	private boolean initLogSystem() {
		
		Path logFilePath = genLogFilePath(0);
		if (!Util.createDirectoryIfNotExists(Paths.get(Util.EXEC_PATH, BhParams.Path.LOG_DIR)))
			return false;
		
		if (!Util.createFileIfNotExists(logFilePath))
			return false;
			
		try {
			//ログローテーション
			if (Files.size(logFilePath) > BhParams.LOG_FILE_SIZE_LIMIT)
				if (!renameLogFiles())
					return false;
			logOutputStream = Files.newOutputStream(logFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
		}
		catch (IOException | SecurityException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void errMsgForDebug(String msg) {
		msg = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(Calendar.getInstance().getTime()) + "  ERR : " + msg;
		System.err.print(msg);
		writeMsgToLogFile(msg + "\n");
	}

	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void msgForDebug(String msg) {
		msg = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(Calendar.getInstance().getTime()) + "  MSG : " + msg;
		System.out.print(msg);
		writeMsgToLogFile(msg + "\n");
	}
	
	/**
	 * ログファイルにメッセージを書き込む
	 * @param msg ログファイルに書き込むメッセージ
	 */
	private void writeMsgToLogFile(String msg) {

		try {
			if (logOutputStream != null) {
				logOutputStream.write(msg.getBytes(StandardCharsets.UTF_8));
				logOutputStream.flush();
			}
		}
		catch(IOException | SecurityException e) {}
	}
	
	/**
	 * ログローテンションのため, ログファイルをリネームする
	 * @return リネームに成功した場合true
	 */
	private boolean renameLogFiles() {
				
		try {
			Path oldestLogFilePath = genLogFilePath(BhParams.MAX_LOG_FILE_NUM - 1);
			if (Files.exists(oldestLogFilePath))
				Files.delete(oldestLogFilePath);
			
			for (int fileNo = BhParams.MAX_LOG_FILE_NUM - 2; fileNo >= 0; --fileNo) {
				Path oldLogFilePath = genLogFilePath(fileNo);
				Path newLogFilePath = genLogFilePath(fileNo + 1);
				if (Files.exists(oldLogFilePath))
					Files.move(oldLogFilePath, newLogFilePath, StandardCopyOption.ATOMIC_MOVE);
			}
		}
		catch (IOException | SecurityException e) {
			return false;
		}
		return true;
	}
	
	private Path genLogFilePath(int fileNo) {
		String numStr = ("0000" + fileNo);
		numStr = numStr.substring(numStr.length() - 4, numStr.length());
		String logFileName = BhParams.Path.LOG_FILE_NAME + numStr + ".log";
		return Paths.get(Util.EXEC_PATH, BhParams.Path.LOG_DIR, logFileName);
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
	 * 終了処理をする
	 */
	public void end() {
		try {
			if (logOutputStream != null)
				logOutputStream.close();
		}
		catch (IOException e) {} 
	}
	
	/**
	 * BunnyHopのメッセージ出力機能を止める
	 */
	public void stop() {
		msgPrintTimer.stop();
	}
}
