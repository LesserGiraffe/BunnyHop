package pflab.bunnyHop.control;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.compiler.BhCompiler;
import pflab.bunnyHop.compiler.CompileOption;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.model.WorkspaceSet;
import pflab.bunnyHop.remote.RemoteCommunicator;
import pflab.bunnyHop.root.BunnyHop;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.BhNodeSelectionView;

/**
 * 画面上部のボタンのコントローラクラス
 * @author K.Koike
 */
public class UserOperationController {

	private @FXML FlowPane userOpeViewBase;	//!< ボタンの基底ペイン
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
	private @FXML TextField ipAddrTextField;	//IPアドレス入力欄
	private File currentSaveFile;	//!< 現在保存対象になっているファイル
		
	/**
	 * イベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 * @param workspaceSetTab ワークスペース表示用のタブペイン
	 * @param nodeSelectionViewList ノード選択ビューのリスト
	 */
	public void init(
		WorkspaceSet wss, 
		TabPane workspaceSetTab, 
		List<BhNodeSelectionView> nodeSelectionViewList) {

		setCutHandler(wss);	//カット
		setCopyHandler(wss);	//コピー
		setPasteHandler(wss, workspaceSetTab);	//ペースト
		setDeleteHandler(wss);	//デリート
		setUndoHandler(wss);	//アンドゥ
		setRedoHandler(wss);	//リドゥ
		setZoomInHandler(wss, nodeSelectionViewList);	//ズームイン
		setZoomOutHandler(wss, nodeSelectionViewList);	//ズームアウト
		setWidenHandler(wss);	//ワークスペースの範囲拡大
		setNarrowHandler(wss);	//ワークスペースの範囲縮小
		setSaveAsHandler(wss);	//セーブ (名前をつけて保存)
		setSaveHandler(wss); //セーブ (上書き保存)
		setLoadHandler(wss);	//ロード		
		setAddWorkspaceHandler(wss);//ワークスペース追加
		setExecuteHandler(wss);
	}
	
	/**
	 * コピーボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	public void setCopyHandler(WorkspaceSet wss) {
		
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
	public void setCutHandler(WorkspaceSet wss) {
		
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
	public void setPasteHandler(WorkspaceSet wss, TabPane workspaceSetTab) {
		
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
	public void setDeleteHandler(WorkspaceSet wss) {
		
		deleteBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			currentWS.deleteNodes(currentWS.getSelectedNodeList());
		});
	}

	/**
	 * アンドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */	
	public void setUndoHandler(WorkspaceSet wss) {
		
		undoBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.UNDO, currentWS);
		});
	}

	/**
	 * リドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */		
	public void setRedoHandler(WorkspaceSet wss) {
		
		redoBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.instance().sendMessage(BhMsg.REDO, currentWS);
		});
	}
	
	/**
	 * ズームインボタン押下時のイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 * @param nodeSelectionViewList ノート選択ビューのリスト
	 */
	public void setZoomInHandler(WorkspaceSet wss, List<BhNodeSelectionView> nodeSelectionViewList) {
		
		zoonInBtn.setOnAction(action -> {
			for (BhNodeSelectionView view: nodeSelectionViewList) {
				if (view.visibleProperty().get()) {
					view.zoom(true);
					return;
				}
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
	 * @param nodeSelectionViewList ノート選択ビューのリスト
	 */
	public void setZoomOutHandler(WorkspaceSet wss, List<BhNodeSelectionView> nodeSelectionViewList) {
		
		zoonOutBtn.setOnAction(action -> {
			for (BhNodeSelectionView view: nodeSelectionViewList) {
				if (view.visibleProperty().get()) {
					view.zoom(false);
					return;
				}
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
	public void setWidenHandler(WorkspaceSet wss) {
		
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
	public void setNarrowHandler(WorkspaceSet wss) {
		
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
	public void setSaveAsHandler(WorkspaceSet wss) {
		
		saveAsBtn.setOnAction(action -> {
			if (wss.getWorkspaceList().isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("名前を付けて保存");
				alert.setHeaderText(null);
				alert.setContentText("保存すべきワークスペースがありません");
				alert.showAndWait();
				return;
			}
			
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("名前を付けて保存");
			fileChooser.setInitialDirectory(getInitDir());
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"), 
				new FileChooser.ExtensionFilter("All Files", "*.*"));
			File selectedFile = fileChooser.showSaveDialog(userOpeViewBase.getScene().getWindow());
			boolean success = false;
			if (selectedFile != null) {
				success = wss.save(selectedFile);
			}
			if (success) {
				currentSaveFile = selectedFile;
			}
			else if (selectedFile != null){
				String fileName = selectedFile.getPath();
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("名前を付けて保存");
				alert.setHeaderText(null);
				alert.setContentText("ファイルの保存に失敗しました\n" + fileName);
				alert.showAndWait();
			}
		});
	}

	/**
	 * セーブ(上書き保存)ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	public void setSaveHandler(WorkspaceSet wss) {
		
		saveBtn.setOnAction(action -> {
			if (wss.getWorkspaceList().isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("上書き保存");
				alert.setHeaderText(null);
				alert.setContentText("保存すべきワークスペースがありません");
				alert.showAndWait();
				return;
			}
						
			boolean fileExists = false;
			if (currentSaveFile != null)
				fileExists = currentSaveFile.exists();
			
			if (fileExists) {				
				boolean succes = wss.save(currentSaveFile);
				if (succes) {
					MsgPrinter.instance.MsgForUser("保存が完了しました." + currentSaveFile.getPath() + "\n");
				}
				else {
					MsgPrinter.instance.MsgForUser("保存に失敗しました. " + currentSaveFile.getPath() + "\n");
				}
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
	public void setLoadHandler(WorkspaceSet wss) {
		
		loadBtn.setOnAction(action -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("開く");
			fileChooser.setInitialDirectory(getInitDir());
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("BunnyHop Files", "*.bnh"), 
				new FileChooser.ExtensionFilter("All Files", "*.*"));
			File selectedFile = fileChooser.showOpenDialog(userOpeViewBase.getScene().getWindow());
			boolean success = false;
			if (selectedFile != null)
				success = wss.load(selectedFile);
			
			if (success) {
				currentSaveFile = selectedFile;
			}
			else if (selectedFile != null){
				String fileName = selectedFile.getPath();
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("開く");
				alert.setHeaderText(null);
				alert.setContentText("ファイルを開けませんでした\n" + fileName);
				alert.showAndWait();
			}
		});
	}
	
	/**
	 * ワークスペース追加ボタンのイベントハンドラを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	public void setAddWorkspaceHandler(WorkspaceSet wss) {
		
		addWorkspaceBtn.setOnAction(action -> {
			String defaultWsName = "workspace" + (wss.getWorkspaceList().size()+1);
			TextInputDialog dialog = new TextInputDialog(defaultWsName);
			dialog.setTitle("ワークスペースの作成");
			dialog.setHeaderText(null);
			dialog.setContentText("ワークスペース名を入力してください");
			Optional<String> inputText = dialog.showAndWait();
			inputText.ifPresent(wsName -> {
				BunnyHop.instance().addNewWorkSpace(wsName, BhParams.defaultWorkspaceWidth, BhParams.defaultWorkspaceHeight);
			});
		});
	}
	
	/**
	 * 実行ボタンのイベントハンドらを登録する
	 * @param wss イベント時に操作するワークスペースセット
	 */
	public void setExecuteHandler(WorkspaceSet wss) {
		
		executeBtn.setOnAction(action -> {
						
			//実行対象があるかどうかの確認
			Workspace currentWS = wss.getCurrentWorkspace();
			SortedSet<BhNode> selectedNodeList = currentWS.getSelectedNodeList();
			if (selectedNodeList.size() != 1) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("実行対象の選択");
				alert.setHeaderText(null);
				alert.setContentText("実行対象を一つ選択してください");
				alert.showAndWait();
				return;
			}
			
			// 実行対象以外を非選択に.
			BhNode executed = selectedNodeList.last();
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			currentWS.clearSelectedNodeList(userOpeCmd);
			currentWS.addSelectedNode(executed, userOpeCmd);
			MsgTransporter.instance().sendMessage(BhMsg.PUSH_USER_OPE_CMD, new MsgData(userOpeCmd), currentWS);
			executeBtn.setDisable(true);
			
			//コンパイル
			List<BhNode> compiledNode = new ArrayList<>();
			wss.getWorkspaceList().forEach(ws -> {
				ws.getRootNodeList().forEach(node -> {
					compiledNode.add(node);
				});
			});
			compiledNode.remove(executed);
			CompileOption option = new CompileOption(isLocalHost(), true, true, true);
			Optional<Path> execFilePath = BhCompiler.instance.compile(executed, compiledNode, option);
		
			if (execFilePath.isPresent())
				sendExecFile(execFilePath.get());
			else
				executeBtn.setDisable(false);
		});		
	}
	
	/**
	 * 実行ファイルを転送する
	 * @param filePath 送信する実行ファイルのパス
	 */
	private void sendExecFile(Path filePath) {
		if (isLocalHost()) {	//ローカルホストのときは実行ファイル転送しない
			executeBtn.setDisable(false);
			return;
		}
			
		//ファイル転送
		Consumer<Boolean> onTransferEnd = (tansferSuccessful) -> {
			Platform.runLater(() -> {executeBtn.setDisable(false);});
		};
		String ipAddr = ipAddrTextField.textProperty().get();
		RemoteCommunicator.instance.sendFile(filePath, onTransferEnd, ipAddr);
	}
	
	/**
	 * ローカルホストでコードを実行する場合true
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
}

















