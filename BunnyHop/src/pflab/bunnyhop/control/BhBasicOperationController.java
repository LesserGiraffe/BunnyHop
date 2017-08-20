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
package pflab.bunnyhop.control;

import java.awt.Event;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pflab.bunnyhop.bhprogram.BhProgramExecEnvError;
import pflab.bunnyhop.bhprogram.common.BhProgramData;
import pflab.bunnyhop.bhprogram.LocalBhProgramManager;
import pflab.bunnyhop.bhprogram.RemoteBhProgramManager;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Point2D;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.compiler.BhCompiler;
import pflab.bunnyhop.compiler.CompileOption;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.model.WorkspaceSet;
import pflab.bunnyhop.root.BunnyHop;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.view.BhNodeCategoryListView;

/**
 * 画面上部のボタンのコントローラクラス
 * @author K.Koike
 */
public class BhBasicOperationController {

	private @FXML VBox bhBasicOpViewBase;	//!< ボタンの基底ペイン
	private @FXML Button copyBtn;	 //!< コピーボタン
	private @FXML Button cutBtn;	 //!< カットボタン
	private @FXML Button pasteBtn;	 //!< ペーストボタン
	private @FXML Button deleteBtn; //!< デリートボタン
	private @FXML Button undoBtn;	 //!< アンドゥボタン
	private @FXML Button redoBtn;	 //!< リドゥボタン
	private @FXML Button zoonInBtn;	 //!< ズームインボタン
	private @FXML Button zoonOutBtn; //!< ズームアウトボタン
    private @FXML Button widenBtn;	//!< ワークスペース拡張ボタン
	private @FXML Button narrowBtn;	//!< ワークスペース縮小ボタン
    private @FXML Button saveAsBtn;	//!< セーブボタン (新規保存)
	private @FXML Button saveBtn;	//!< セーブボタン (上書き保存)
	private @FXML Button loadBtn;	//!< ロードボタン
	private @FXML Button addWorkspaceBtn; //!< ワークスペース追加ボタン
	private @FXML Button executeBtn;	//!< 実行ボタン
	private @FXML Button terminateBtn;	//!< 終了ボタン
	private @FXML Button connectBtn;	//!< 接続ボタン
	private @FXML Button disconnectBtn;	//!< 切断ボタン
	private @FXML TextField ipAddrTextField;	//IPアドレス入力欄
	private @FXML TextField unameTextField; //!< ユーザ名
	private @FXML PasswordField passwordTextField;	//!< ログインパスワード
	private @FXML Button sendBtn;	//!< 送信ボタン
	private @FXML TextField stdInTextField;	//!< 標準入力テキストフィールド
	private File currentSaveFile;	//!< 現在保存対象になっているファイル
	private boolean bhProgramIsRunningLocally = true;	//!< ルーカルマシン上でBhProgramを実行している場合true
		
	/**
	 * イベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 * @param workspaceSetTab ワークスペース表示用のタブペイン
	 * @param bhNodeCategoryListView ノードカテゴリ選択ビュー
	 */
	public void init(
		WorkspaceSet wss, 
		TabPane workspaceSetTab, 
		BhNodeCategoryListView bhNodeCategoryListView) {

		setCutHandler(wss);	//カット
		setCopyHandler(wss);	//コピー
		setPasteHandler(wss, workspaceSetTab);	//ペースト
		setDeleteHandler(wss);	//デリート
		setUndoHandler(wss);	//アンドゥ
		setRedoHandler(wss);	//リドゥ
		setZoomInHandler(wss, bhNodeCategoryListView);	//ズームイン
		setZoomOutHandler(wss, bhNodeCategoryListView);	//ズームアウト
		setWidenHandler(wss);	//ワークスペースの範囲拡大
		setNarrowHandler(wss);	//ワークスペースの範囲縮小
		setSaveAsHandler(wss);	//セーブ (名前をつけて保存)
		setSaveHandler(wss); //セーブ (上書き保存)
		setLoadHandler(wss);	//ロード		
		setAddWorkspaceHandler(wss);//ワークスペース追加
		setExecuteHandler(wss);
		setTerminateHandler();
		setConnectHandler();
		setDisconnectHandler();
		setSendHandler();
	}
	
	/**
	 * コピーボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setCopyHandler(WorkspaceSet wss) {
		
		copyBtn.setOnAction(action -> {
		Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			wss.addNodeListReadyToCopy(currentWS.getSelectedNodeList());
		});
	}
	
	/**
	 * カットボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setCutHandler(WorkspaceSet wss) {
		
		cutBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			wss.addNodeListReadyToCut(currentWS.getSelectedNodeList());
		});
	}
	
	/**
	 * ペーストボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 * @param workspaceSetTab ワークスペースセットに対応するタブペイン
	 */
	private void setPasteHandler(WorkspaceSet wss, TabPane workspaceSetTab) {
		
		pasteBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			
			javafx.geometry.Point2D pos = workspaceSetTab.localToScene(0, workspaceSetTab.getHeight() / 2.0);	
			MsgData localPos = MsgTransporter.instance().sendMessage(BhMsg.SCENE_TO_WORKSPACE, new MsgData(pos.getX(), pos.getY()), currentWS);
			double pastePosX = localPos.doublePair._1 + BhParams.replacedNodePos * 2;
			double pastePosY = localPos.doublePair._2;
			wss.paste(currentWS, new Point2D(pastePosX, pastePosY));
		});
	}
	
	/**
	 * デリートボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setDeleteHandler(WorkspaceSet wss) {
		
		deleteBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			currentWS.deleteNodes(currentWS.getSelectedNodeList(), userOpeCmd);
			BunnyHop.instance().pushUserOpeCmd(userOpeCmd);
		});
	}

	/**
	 * アンドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */	
	private void setUndoHandler(WorkspaceSet wss) {
		
		undoBtn.setOnAction(action -> {
			MsgTransporter.instance().sendMessage(BhMsg.UNDO, wss);
		});
	}

	/**
	 * リドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */		
	private void setRedoHandler(WorkspaceSet wss) {
		
		redoBtn.setOnAction(action -> {
			MsgTransporter.instance().sendMessage(BhMsg.REDO, wss);
		});
	}
	
	/**
	 * ズームインボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 * @param bhNodeCategoryListView ノードカテゴリ選択ビュー
	 */
	private void setZoomInHandler(WorkspaceSet wss, BhNodeCategoryListView bhNodeCategoryListView) {
		
		zoonInBtn.setOnAction(action -> {
			
			if (bhNodeCategoryListView.isAnyShowed()) {
				bhNodeCategoryListView.zoomAll(true);
				return;
			}
			
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.ZOOM, new MsgData(true), currentWS);
		});
	}
	
	/**
	 * ズームインボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 * @param bhNodeCategoryListView ノードカテゴリ選択ビュー
	 */
	private void setZoomOutHandler(WorkspaceSet wss, BhNodeCategoryListView bhNodeCategoryListView) {
		
		zoonOutBtn.setOnAction(action -> {
			
			if (bhNodeCategoryListView.isAnyShowed()) {
				bhNodeCategoryListView.zoomAll(false);
				return;
			}
			
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.ZOOM, new MsgData(false), currentWS);
		});
	}
	
	/**
	 * ワークスペース拡大ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setWidenHandler(WorkspaceSet wss) {
		
		widenBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(true), currentWS);
		});
	}
	
	/**
	 * ワークスペース縮小ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setNarrowHandler(WorkspaceSet wss) {
		
		narrowBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(false), currentWS);
		});
	}
	
	/**
	 * セーブ(新規保存)ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setSaveAsHandler(WorkspaceSet wss) {
		
		saveAsBtn.setOnAction(action -> {
			if (wss.getWorkspaceList().isEmpty()) {
				MsgPrinter.instance.alert(
					AlertType.INFORMATION, 
					"名前を付けて保存",
					null,
					"保存すべきワークスペースがありません");
				return;
			}
			
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("名前を付けて保存");
			fileChooser.setInitialDirectory(getInitDir());
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"), 
				new FileChooser.ExtensionFilter("All Files", "*.*"));
			File selectedFile = fileChooser.showSaveDialog(bhBasicOpViewBase.getScene().getWindow());
			boolean success = false;
			if (selectedFile != null) {
				success = wss.save(selectedFile);
			}
			if (success) {
				currentSaveFile = selectedFile;
			}
		});
	}

	/**
	 * セーブ(上書き保存)ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setSaveHandler(WorkspaceSet wss) {
		
		saveBtn.setOnAction(action -> {
			if (wss.getWorkspaceList().isEmpty()) {
				MsgPrinter.instance.alert(
					AlertType.INFORMATION, 
					"上書き保存",
					null,
					"保存すべきワークスペースがありません");
				return;
			}
						
			boolean fileExists = false;
			if (currentSaveFile != null)
				fileExists = currentSaveFile.exists();
			
			if (fileExists) {				
				wss.save(currentSaveFile);
			}
			else {
				saveAsBtn.fireEvent(action);	//保存対象のファイルが無い場合, 名前をつけて保存
			}
		});
	}
	
	/**
	 * ロードボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setLoadHandler(WorkspaceSet wss) {
		
		loadBtn.setOnAction(action -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("開く");
			fileChooser.setInitialDirectory(getInitDir());
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"), 
				new FileChooser.ExtensionFilter("All Files", "*.*"));
			File selectedFile = fileChooser.showOpenDialog(bhBasicOpViewBase.getScene().getWindow());
			boolean success = false;
			if (selectedFile != null)
				success = wss.load(selectedFile, this::isOldWsCleared);
			
			if (success) {
				currentSaveFile = selectedFile;
			}
			else if (selectedFile != null){
				String fileName = selectedFile.getPath();
				MsgPrinter.instance.alert(
					AlertType.INFORMATION, 
					"開く",
					null,
					"ファイルを開けませんでした\n" + fileName);
			}
		});
	}
	
	/**
	 * ロード方法を確認する
	 * @retval true 既存のワークスペースをすべて削除
	 * @retval false 既存のワークスペースにロードしたワークスペースを追加
	 */
	private boolean  isOldWsCleared() {
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("ファイルのロード方法");
		alert.setHeaderText(null);
		alert.setContentText("既存のワークスペースに追加する場合は[はい].\n既存のワークスペースを全て削除する場合は[いいえ].");
		ButtonType buttonTypeYes = new ButtonType("はい");
		ButtonType buttonTypeNo = new ButtonType("いいえ");
		alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
		Optional<ButtonType> result = alert.showAndWait();
		return result.get() == buttonTypeNo;
	}
	
	/**
	 * ワークスペース追加ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setAddWorkspaceHandler(WorkspaceSet wss) {
		
		addWorkspaceBtn.setOnAction(action -> {
			String defaultWsName = "workspace" + (wss.getWorkspaceList().size()+1);
			TextInputDialog dialog = new TextInputDialog(defaultWsName);
			dialog.setTitle("ワークスペースの作成");
			dialog.setHeaderText(null);
			dialog.setContentText("ワークスペース名を入力してください");
			Optional<String> inputText = dialog.showAndWait();
			inputText.ifPresent(wsName -> {
				UserOperationCommand userOpeCmd = new UserOperationCommand();
				BunnyHop.instance().addNewWorkSpace(
					wsName, 
					BhParams.defaultWorkspaceWidth, 
					BhParams.defaultWorkspaceHeight,
					userOpeCmd);
				BunnyHop.instance().pushUserOpeCmd(userOpeCmd);
			});
		});
	}
	
	/**
	 * 実行ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	private void setExecuteHandler(WorkspaceSet wss) {
		
		executeBtn.setOnAction(action -> {
						
			//実行対象があるかどうかの確認
			Workspace currentWS = wss.getCurrentWorkspace();
			HashSet<BhNode> selectedNodeList = currentWS.getSelectedNodeList();
			if (selectedNodeList.size() != 1) {
				MsgPrinter.instance.alert(
					AlertType.ERROR,
					"実行対象の選択",
					null,
					"実行対象を一つ選択してください");
				return;
			}
			MsgPrinter.instance.MsgForUser("\n");
			
			// 実行対象以外を非選択に.
			BhNode nodeToExec = selectedNodeList.toArray(new BhNode[selectedNodeList.size()])[0];
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			currentWS.clearSelectedNodeList(userOpeCmd);
			currentWS.addSelectedNode(nodeToExec, userOpeCmd);
			BunnyHop.instance().pushUserOpeCmd(userOpeCmd);
			
			//コンパイル
			List<BhNode> compiledNode = new ArrayList<>();
			wss.getWorkspaceList().forEach(ws -> {
				ws.getRootNodeList().forEach(node -> {
					compiledNode.add(node);
				});
			});
			compiledNode.remove(nodeToExec.findRootNode());
			CompileOption option = new CompileOption(isLocalHost(), true, true, true);
			Optional<Path> execFilePath = BhCompiler.instance.compile(nodeToExec, compiledNode, option);
			execFilePath.ifPresent(filePath -> {
				bhProgramIsRunningLocally = isLocalHost();
				if (bhProgramIsRunningLocally)
					LocalBhProgramManager.instance.executeAsync(filePath, ipAddrTextField.getText());
				else
					RemoteBhProgramManager.instance.executeAsync(
						filePath, ipAddrTextField.getText(), unameTextField.getText(), passwordTextField.getText());
			});
		});
	}
	
	/**
	 * 終了ボタンのイベントハンドラを登録する
	 */
	private void setTerminateHandler() {
		terminateBtn.setOnAction(action -> {
			if (bhProgramIsRunningLocally)
				LocalBhProgramManager.instance.terminateAsync();
			else
				RemoteBhProgramManager.instance.terminateAsync();
		});
	}
	
	/**
	 * 切断ボタンのイベントハンドラを登録する
	 */
	private void setDisconnectHandler() {
		disconnectBtn.setOnAction(action -> {
			if (bhProgramIsRunningLocally)
				LocalBhProgramManager.instance.disconnectAsync();
			else
				RemoteBhProgramManager.instance.disconnectAsync();
		});
	}
	
	/**
	 * 接続ボタンのイベントハンドラを登録する
	 */
	private void setConnectHandler() {
		connectBtn.setOnAction(action -> {
			if (bhProgramIsRunningLocally)
				LocalBhProgramManager.instance.connectAsync();
			else
				RemoteBhProgramManager.instance.connectAsync();
		});
	}
	
	/**
	 * 送信ボタンのイベントハンドラを登録する
	 */
	private void setSendHandler() {
		sendBtn.setOnAction(action -> {
			BhProgramExecEnvError errCode;
			if (bhProgramIsRunningLocally)
				errCode = LocalBhProgramManager.instance.sendAsync(
					new BhProgramData(BhProgramData.TYPE.INPUT_STR, stdInTextField.getText()));
			else
				errCode = RemoteBhProgramManager.instance.sendAsync(
					new BhProgramData(BhProgramData.TYPE.INPUT_STR, stdInTextField.getText()));
			
			switch (errCode) {
				case SEND_QUEUE_FULL:
					MsgPrinter.instance.ErrMsgForUser("!! 送信失敗 (送信データ追加失敗) !!\n");
					break;
					
				case SEND_WHEN_DISCONNECTED:
					MsgPrinter.instance.ErrMsgForUser("!! 送信失敗 (未接続) --\n");
					break;
					
				case SUCCESS:
					MsgPrinter.instance.ErrMsgForUser("-- 送信完了 --\n");
					break;
			}
		});
	}
	
	/**
	 * IPアドレス入力欄にローカルホストが指定してある場合true を返す
	 */
	private boolean isLocalHost() {
		String ipAddr = ipAddrTextField.textProperty().get();
		return ipAddr.equals("127.0.0.1") || ipAddr.equals("localhost");
	}
	
	/**
	 * ファイル保存時の初期ディレクトリを返す
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
		return new File(Util.execPath);
	}
	
	/**
	 * ユーザメニュー操作のイベントを起こす
	 * @param op 基本操作を表す列挙子
	 */
	public void fireEvent(BASIC_OPERATION op) {
		
		switch (op) {
			case COPY:
				copyBtn.fireEvent(new ActionEvent());
				break;
				
			case CUT:
				cutBtn.fireEvent(new ActionEvent());
				break;
				
			case PASTE:
				pasteBtn.fireEvent(new ActionEvent());
				break;
				
			case DELETE:
				deleteBtn.fireEvent(new ActionEvent());
				break;
				
			case SAVE:
				saveBtn.fireEvent(new ActionEvent());
				break;
				
			case SAVE_AS:
				saveAsBtn.fireEvent(new ActionEvent());
				break;
				
			case UNDO:
				undoBtn.fireEvent(new ActionEvent());
				break;
				
			case REDO:
				redoBtn.fireEvent(new ActionEvent());
				break;
		}
	}
	
	public enum BASIC_OPERATION {
		COPY,
		CUT,
		PASTE,
		DELETE,
		SAVE,
		SAVE_AS,
		UNDO,
		REDO,
	}
}

















