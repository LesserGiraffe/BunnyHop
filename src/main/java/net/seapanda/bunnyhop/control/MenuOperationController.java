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
package net.seapanda.bunnyhop.control;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.BhProgramExecEnvError;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramManager;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramManager;
import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.CompileNodeCollector;
import net.seapanda.bunnyhop.compiler.CompileOption;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.NodeGraphSnapshot;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.CauseOfDeletion;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.DelayedDeleter;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.modelservice.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.BhNodeCategoryListView;

/**
 * 画面上部のボタンのコントローラクラス
 * @author K.Koike
 */
public class MenuOperationController {

	private @FXML VBox menuOpViewBase;	//!< ボタンの基底ペイン
	private @FXML Button copyBtn;	 //!< コピーボタン
	private @FXML Button cutBtn;	 //!< カットボタン
	private @FXML Button pasteBtn;	 //!< ペーストボタン
	private @FXML Button deleteBtn; //!< デリートボタン
	private @FXML Button undoBtn;	 //!< アンドゥボタン
	private @FXML Button redoBtn;	 //!< リドゥボタン
	private @FXML Button zoomInBtn;	 //!< ズームインボタン
	private @FXML Button zoomOutBtn; //!< ズームアウトボタン
    private @FXML Button widenBtn;	//!< ワークスペース拡張ボタン
	private @FXML Button narrowBtn;	//!< ワークスペース縮小ボタン
	private @FXML Button addWorkspaceBtn; //!< ワークスペース追加ボタン
	private @FXML ToggleButton remotLocalSelectBtn;	//!< リモート/ローカル選択ボタン
	private @FXML Button executeBtn;	//!< 実行ボタン
	private @FXML Button terminateBtn;	//!< 終了ボタン
	private @FXML Button connectBtn;	//!< 接続ボタン
	private @FXML Button disconnectBtn;	//!< 切断ボタン
	private @FXML Button jumpBtn;	//!< ジャンプボタン
	private @FXML TextField ipAddrTextField;	//IPアドレス入力欄
	private @FXML TextField unameTextField; //!< ユーザ名
	private @FXML PasswordField passwordTextField;	//!< ログインパスワード
	private @FXML Button sendBtn;	//!< 送信ボタン
	private @FXML TextField stdInTextField;	//!< 標準入力テキストフィールド

	private final AtomicBoolean preparingForExecution = new AtomicBoolean(false);	//非同期でBhProgramの実行環境準備中の場合true
	private final AtomicBoolean preparingForTermination = new AtomicBoolean(false);	//非同期でBhProgramの実行環境終了中の場合true
	private final AtomicBoolean connecting = new AtomicBoolean(false);	//非同期で接続中の場合true
	private final AtomicBoolean disconnecting = new AtomicBoolean(false);	//非同期で切断中の場合true
	private final ExecutorService compileExec = Executors.newCachedThreadPool();	//コンパイルを実行する Executor
	private final ExecutorService waitTaskExec = Executors.newCachedThreadPool();	//非同期処理完了待ちタスクを実行する

	/**
	 * イベントハンドラをセットする
	 * @param wss ワークスペースセット
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
		setJumpHandler(wss);	//ジャンプ
		setUndoHandler(wss);	//アンドゥ
		setRedoHandler(wss);	//リドゥ
		setZoomInHandler(wss, bhNodeCategoryListView);	//ズームイン
		setZoomOutHandler(wss, bhNodeCategoryListView);	//ズームアウト
		setWidenHandler(wss);	//ワークスペースの範囲拡大
		setNarrowHandler(wss);	//ワークスペースの範囲縮小
		setAddWorkspaceHandler(wss);//ワークスペース追加
		setExecuteHandler(wss);
		setTerminateHandler();
		setConnectHandler();
		setDisconnectHandler();
		setSendHandler();
		setRemoteLocalSelectHandler();
		setHandlersToChangeButtonEnable(wss);
	}

	/**
	 * コピーボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setCopyHandler(WorkspaceSet wss) {

		copyBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					Workspace currentWS = wss.getCurrentWorkspace();
						if (currentWS == null)
							return;

						UserOperationCommand userOpeCmd = new UserOperationCommand();
						wss.addNodesToCopyList(currentWS.getSelectedNodeList(), userOpeCmd);
						BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * カットボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setCutHandler(WorkspaceSet wss) {

		cutBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					Workspace currentWS = wss.getCurrentWorkspace();
					if (currentWS == null)
						return;

					UserOperationCommand userOpeCmd = new UserOperationCommand();
					wss.addNodesToCutList(currentWS.getSelectedNodeList(), userOpeCmd);
					BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * ペーストボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 * @param workspaceSetTab ワークスペースセットに対応するタブペイン
	 */
	private void setPasteHandler(WorkspaceSet wss, TabPane workspaceSetTab) {

		pasteBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					Workspace currentWS = wss.getCurrentWorkspace();
					if (currentWS == null)
						return;

					javafx.geometry.Point2D pos = workspaceSetTab.localToScene(0, workspaceSetTab.getHeight() / 3.0);
					Vec2D localPos = MsgService.INSTANCE.sceneToWorkspace(pos.getX(), pos.getY(), currentWS);
					double pastePosX = localPos.x + BhParams.LnF.REPLACED_NODE_SHIFT * 2;
					double pastePosY = localPos.y;
					wss.paste(currentWS, new Vec2D(pastePosX, pastePosY));
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * デリートボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setDeleteHandler(WorkspaceSet wss) {

		deleteBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					Workspace currentWS = wss.getCurrentWorkspace();
					if (currentWS == null)
						return;

					UserOperationCommand userOpeCmd = new UserOperationCommand();

					var nodesToDelete = currentWS.getSelectedNodeList();
					nodesToDelete.forEach(node ->
						node.execScriptOnDeletionRequested(nodesToDelete, CauseOfDeletion.SELECTED_FOR_DELETION ,userOpeCmd));

					BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpeCmd)
					.forEach(oldAndNewNode -> {
						BhNode oldNode = oldAndNewNode._1;
						BhNode newNode = oldAndNewNode._2;
						newNode.findParentNode().execScriptOnChildReplaced(oldNode, newNode, newNode.getParentConnector(), userOpeCmd);
					});

					DelayedDeleter.INSTANCE.deleteCandidates(userOpeCmd);
					SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
					SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(userOpeCmd);
					BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * ジャンプボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setJumpHandler(WorkspaceSet wss) {

		jumpBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					findNodeToJumpTo(wss).ifPresent(node -> {
						MsgService.INSTANCE.lookAt(node);
						UserOperationCommand userOpeCmd = new UserOperationCommand();
						node.getWorkspace().clearSelectedNodeList(userOpeCmd);
						node.getWorkspace().addSelectedNode(node, userOpeCmd);
						BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
					});
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * アンドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setUndoHandler(WorkspaceSet wss) {

		undoBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					MsgTransporter.INSTANCE.sendMessage(BhMsg.UNDO, wss);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * リドゥボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setRedoHandler(WorkspaceSet wss) {

		redoBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					MsgTransporter.INSTANCE.sendMessage(BhMsg.REDO, wss);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * ズームインボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 * @param bhNodeCategoryListView ノードカテゴリ選択ビュー
	 */
	private void setZoomInHandler(WorkspaceSet wss, BhNodeCategoryListView bhNodeCategoryListView) {

		zoomInBtn.setOnAction(action -> {

			if (bhNodeCategoryListView.isAnyShowed()) {
				bhNodeCategoryListView.zoomAll(true);
				return;
			}

			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(true), currentWS);
		});
	}

	/**
	 * ズームインボタン押下時のイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 * @param bhNodeCategoryListView ノードカテゴリ選択ビュー
	 */
	private void setZoomOutHandler(WorkspaceSet wss, BhNodeCategoryListView bhNodeCategoryListView) {

		zoomOutBtn.setOnAction(action -> {

			if (bhNodeCategoryListView.isAnyShowed()) {
				bhNodeCategoryListView.zoomAll(false);
				return;
			}

			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(false), currentWS);
		});
	}

	/**
	 * ワークスペース拡大ボタンのイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setWidenHandler(WorkspaceSet wss) {

		widenBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.INSTANCE.sendMessage(BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(true), currentWS);
		});
	}

	/**
	 * ワークスペース縮小ボタンのイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setNarrowHandler(WorkspaceSet wss) {

		narrowBtn.setOnAction(action -> {
			Workspace currentWS = wss.getCurrentWorkspace();
			if (currentWS == null)
				return;
			MsgTransporter.INSTANCE.sendMessage(BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(false), currentWS);
		});
	}

	/**
	 * ワークスペース追加ボタンのイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setAddWorkspaceHandler(WorkspaceSet wss) {

		addWorkspaceBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForModification();
				try {
					String defaultWsName = "ワークスペース" + (wss.getWorkspaceList().size()+1);
					TextInputDialog dialog = new TextInputDialog(defaultWsName);
					dialog.setTitle("ワークスペースの作成");
					dialog.setHeaderText(null);
					dialog.setContentText("ワークスペース名を入力してください");
					dialog.getDialogPane().getStylesheets().addAll(BunnyHop.INSTANCE.getAllStyles());
					Optional<String> inputText = dialog.showAndWait();
					inputText.ifPresent(wsName -> {
						UserOperationCommand userOpeCmd = new UserOperationCommand();
						BunnyHop.INSTANCE.addNewWorkSpace(
							wsName,
							BhParams.LnF.DEFAULT_WORKSPACE_WIDTH,
							BhParams.LnF.DEFAULT_WORKSPACE_HEIGHT,
							userOpeCmd);
						BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
					});
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForModification();
				}
			});
	}

	/**
	 * 実行ボタンのイベントハンドラを登録する
	 * @param wss イベント発生時に操作するワークスペースセット
	 */
	private void setExecuteHandler(WorkspaceSet wss) {

		executeBtn.setOnAction(
			action -> {
				ModelExclusiveControl.INSTANCE.lockForRead();
				Optional<Pair<NodeGraphSnapshot, BhNode>> snapshotAndNodeToExecOpt = Optional.empty();
				try {
					if (preparingForExecution.get()) {
						MsgPrinter.INSTANCE.errMsgForUser("!! 実行準備中 !!\n");
						return;
					}
					var userOpeCmd = new UserOperationCommand();
					snapshotAndNodeToExecOpt = CompileNodeCollector.collect(wss, userOpeCmd);
					BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
				}
				finally {
					ModelExclusiveControl.INSTANCE.unlockForRead();
				}

				if (!snapshotAndNodeToExecOpt.isPresent())
					return;

				preparingForExecution.set(true);
				var snapshotAndNodeToExec = snapshotAndNodeToExecOpt.get();
				compileExec.submit(() -> {
					Optional<Path> srcFilePath = compile(snapshotAndNodeToExec);
					srcFilePath.ifPresentOrElse(
						this::execute,
						() -> preparingForExecution.set(false));
				});
			});
	}

	/**
	 * ノードをコンパイルする
	 * @param snapshotAndNodeToExec コンパイル対象のノードのスナップショットと実行ノードのペア
	 * @return ノードをコンパイルしてできたソースファイルのパス
	 */
	private Optional<Path> compile(Pair<NodeGraphSnapshot, BhNode> snapshotAndNodeToExec) {

		CompileOption option = new CompileOption.Builder(isLocalHost()).build();
		Collection<BhNode> nodesToCompile = snapshotAndNodeToExec._1.getRootNodeList();
		BhNode nodeToExec = snapshotAndNodeToExec._2;
		Optional<Path> execFilePath = BhCompiler.INSTANCE.compile(nodeToExec, nodesToCompile, option);
		return execFilePath;
	}

	/**
	 * 引数で指定したソースファイルのコードを実行する
	 * @param srcPath 実行するソースファイルのパス
	 */
	private void execute(Path srcPath) {

		Future<Boolean> future = startProgram(srcPath);
		waitForTaskToComplete(future, "Execute");
		preparingForExecution.set(false);
	}

	/**
	 * コンパイルしたプログラムを実行する
	 * @param filePath 実行するプログラムのファイルパス
	 * @return プログラム起動タスクの Future オブジェクト
	 */
	private Future<Boolean> startProgram(Path filePath) {

		if (isLocalHost()) {
			return LocalBhProgramManager.INSTANCE.executeAsync(filePath, BhParams.ExternalApplication.LOLCAL_HOST);
		}

		return RemoteBhProgramManager.INSTANCE.executeAsync(
			filePath, ipAddrTextField.getText(), unameTextField.getText(), passwordTextField.getText());
	}

	/**
	 * 終了ボタンのイベントハンドラを登録する
	 */
	private void setTerminateHandler() {

		terminateBtn.setOnAction(action -> {
			if (preparingForTermination.get()) {
				MsgPrinter.INSTANCE.errMsgForUser("!! 終了準備中 !!\n");
				return;
			}

			Future<Boolean> future;
			if (isLocalHost())
				future = LocalBhProgramManager.INSTANCE.terminateAsync();
			else
				future = RemoteBhProgramManager.INSTANCE.terminateAsync();

			preparingForTermination.set(true);
			waitTaskExec.submit(() ->{
				waitForTaskToComplete(future, "Terminate");
				preparingForTermination.set(false);
			});
		});
	}

	/**
	 * 切断ボタンのイベントハンドラを登録する
	 */
	private void setDisconnectHandler() {

		disconnectBtn.setOnAction(action -> {
			if (disconnecting.get()) {
				MsgPrinter.INSTANCE.errMsgForUser("!! 切断準備中 !!\n");
				return;
			}

			Future<Boolean> future;
			if (isLocalHost())
				future = LocalBhProgramManager.INSTANCE.disconnectAsync();
			else
				future = RemoteBhProgramManager.INSTANCE.disconnectAsync();

			disconnecting.set(true);
			waitTaskExec.submit(() ->{
				waitForTaskToComplete(future, "Disconnect");
				disconnecting.set(false);
			});
		});
	}

	/**
	 * 接続ボタンのイベントハンドラを登録する
	 */
	private void setConnectHandler() {

		connectBtn.setOnAction(action -> {
			if (connecting.get()) {
				MsgPrinter.INSTANCE.errMsgForUser("!! 接続準備中 !!\n");
				return;
			}

			Future<Boolean> future;
			if (isLocalHost())
				future = LocalBhProgramManager.INSTANCE.connectAsync();
			else
				future = RemoteBhProgramManager.INSTANCE.connectAsync();

			connecting.set(true);
			waitTaskExec.submit(() -> {
				waitForTaskToComplete(future, "Connect");
				connecting.set(false);
			});
		});
	}

	/**
	 * Future オブジェクトを使ってタスクの終了を待つ
	 * @param future 完了を待つタスクの Future オブジェクト
	 * @param taskName 完了を待つタスク名
	 * @return 完了したタスクの実行結果. 完了待ちに失敗した場合は null.
	 */
	private <T> T waitForTaskToComplete(Future<T> future, String taskName) {

		try {
			return future.get();
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.msgForDebug(getClass().getSimpleName() + "  " + taskName + "  " + e);
		}

		return null;
	}

	/**
	 * 送信ボタンのイベントハンドラを登録する
	 */
	private void setSendHandler() {

		sendBtn.setOnAction(action -> {
			BhProgramExecEnvError errCode;
			if (isLocalHost())
				errCode = LocalBhProgramManager.INSTANCE.sendAsync(
					new BhProgramData(BhProgramData.TYPE.INPUT_STR, stdInTextField.getText()));
			else
				errCode = RemoteBhProgramManager.INSTANCE.sendAsync(
					new BhProgramData(BhProgramData.TYPE.INPUT_STR, stdInTextField.getText()));

			switch (errCode) {
				case SEND_QUEUE_FULL:
					MsgPrinter.INSTANCE.errMsgForUser("!! 送信失敗 (送信データ追加失敗) !!\n");
					break;

				case SEND_WHEN_DISCONNECTED:
					MsgPrinter.INSTANCE.errMsgForUser("!! 送信失敗 (未接続) !!\n");
					break;

				case SUCCESS:
					MsgPrinter.INSTANCE.errMsgForUser("-- 送信完了 --\n");
					break;

				default:
					throw new AssertionError("invalid " + BhProgramExecEnvError.class.getSimpleName() + " " + errCode);
			}
		});
	}

	/**
	 * リモート/セレクトを切り替えた時のイベントハンドラを登録する
	 */
	private void setRemoteLocalSelectHandler() {

		remotLocalSelectBtn.selectedProperty().addListener((observable, oldVal, newVal) -> {
			if (newVal) {
				ipAddrTextField.setDisable(false);
				unameTextField.setDisable(false);
				passwordTextField.setDisable(false);
				remotLocalSelectBtn.setText("リモート");
			}
			else {
				ipAddrTextField.setDisable(true);
				unameTextField.setDisable(true);
				passwordTextField.setDisable(true);
				remotLocalSelectBtn.setText("ローカル");
			}
		});
	}

	/**
	 * ボタンの有効/無効状態を変化させるイベントハンドラを設定する
	 */
	private void setHandlersToChangeButtonEnable(WorkspaceSet wss) {

		pasteBtn.setDisable(true);
		wss.addOnCopyListChanged(change -> changePasteButtonEnable(wss), true);
		wss.addOnCutListChanged(change -> changePasteButtonEnable(wss), true);
		wss.addOnSelectedNodeListChanged(
			(ws, list) -> jumpBtn.setDisable(!findNodeToJumpTo(wss).isPresent()), true);
		wss.addOnCurrentWorkspaceChanged(
			(oldWs, newWs) -> jumpBtn.setDisable(!findNodeToJumpTo(wss).isPresent()), true);
	}

	/**
	 * ペーストボタンの有効/無効を切り替える
	 */
	private void changePasteButtonEnable(WorkspaceSet wss) {

		boolean disable = wss.isCopyListEmpty() && wss.isCutListEmpty();
		pasteBtn.setDisable(disable);
	}

	/**
	 * ジャンプ先のノードを探す
	 * @param wss このワークスペースセットの中からジャンプ先ノードを探す
	 * @retun ジャンプ先ノード
	 */
	private Optional<BhNode> findNodeToJumpTo(WorkspaceSet wss) {

		Workspace currentWs = wss.getCurrentWorkspace();
		if (currentWs == null || currentWs.getSelectedNodeList().size() != 1) {
			jumpBtn.setDisable(true);
			return Optional.empty();
		}

		return Optional.ofNullable(currentWs.getSelectedNodeList().get(0).getOriginal());
	}

	/**
	 * IPアドレス入力欄にローカルホストが指定してある場合 true を返す
	 */
	public boolean isLocalHost() {
		return !remotLocalSelectBtn.isSelected();
	}

	/**
	 * ユーザメニュー操作のイベントを起こす
	 * @param op 基本操作を表す列挙子
	 */
	public void fireEvent(MENU_OPERATION op) {

		switch (op) {
			case COPY:
				copyBtn.fire();
				break;

			case CUT:
				cutBtn.fire();
				break;

			case PASTE:
				pasteBtn.fire();
				break;

			case DELETE:
				deleteBtn.fire();
				break;

			case UNDO:
				undoBtn.fire();
				break;

			case REDO:
				redoBtn.fire();
				break;

			default:
				throw new AssertionError("invalid menu operation " + op);
		}
	}

	public enum MENU_OPERATION {
		COPY,
		CUT,
		PASTE,
		DELETE,
		UNDO,
		REDO,
	}
}

















