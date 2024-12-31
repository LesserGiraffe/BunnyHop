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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeService;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.InputTextCmd;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.CompileNodeCollector;
import net.seapanda.bunnyhop.compiler.CompileOption;
import net.seapanda.bunnyhop.control.workspace.WorkspaceController;
import net.seapanda.bunnyhop.model.NodeGraphSnapshot;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * 画面上部のボタンのコントローラクラス.
 *
 * @author K.Koike
 */
public class MenuPanelController {

  /** ボタンの基底ペイン. */
  private @FXML VBox menuPanelBase;
  /** コピーボタン. */
  private @FXML Button copyBtn;
  /** カットボタン. */
  private @FXML Button cutBtn;
  /** ペーストボタン. */
  private @FXML Button pasteBtn;
  /** デリートボタン. */
  private @FXML Button deleteBtn;
  /** アンドゥボタン. */
  private @FXML Button undoBtn;
  /** リドゥボタン. */
  private @FXML Button redoBtn;
  /** ズームインボタン. */
  private @FXML Button zoomInBtn;
  /** ズームアウトボタン. */
  private @FXML Button zoomOutBtn;
  /** ワークスペース拡張ボタン. */
  private @FXML Button widenBtn;
  /** ワークスペース縮小ボタン. */
  private @FXML Button narrowBtn;
  /** ワークスペース追加ボタン. */
  private @FXML Button addWorkspaceBtn;
  /** リモート/ローカル選択ボタン. */
  private @FXML ToggleButton remotLocalSelectBtn;
  /** 実行ボタン. */
  private @FXML Button executeBtn;
  /** 終了ボタン. */
  private @FXML Button terminateBtn;
  /** 接続ボタン. */
  private @FXML Button connectBtn;
  /** 切断ボタン. */
  private @FXML Button disconnectBtn;
  /** シミュレータフォーカスボタン. */
  private @FXML Button focusSimBtn;
  /** ジャンプボタン. */
  private @FXML Button jumpBtn;
  /** IPアドレス入力欄. */
  private @FXML TextField ipAddrTextField;
  /** ユーザ名. */
  private @FXML TextField unameTextField;
  /** ログインパスワード. */
  private @FXML PasswordField passwordTextField;
  /** 送信ボタン. */
  private @FXML Button sendBtn;
  /** 標準入力テキストフィールド. */
  private @FXML TextField stdInTextField;

  /** 非同期でBhProgramの実行環境準備中の場合 true. */
  private final AtomicBoolean preparingForExecution = new AtomicBoolean(false);
  /** 非同期でBhProgramの実行環境終了中の場合 true. */
  private final AtomicBoolean preparingForTermination = new AtomicBoolean(false);
  /** 非同期で接続中の場合 true. */
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  /** 非同期で切断中の場合 true. */
  private final AtomicBoolean disconnecting = new AtomicBoolean(false);
  /** コンパイルを実行する Executor. */
  private final ExecutorService compileExec = Executors.newCachedThreadPool();
  /** 非同期処理完了待ちタスクを実行する. */
  private final ExecutorService waitTaskExec = Executors.newCachedThreadPool();
  
  /** ローカル環境で実行する BhProgram を生成するコンパイラ. */
  private BhCompiler locaCompiler;
  /** リモート環境で実行する BhProgram を生成するコンパイラ. */
  private BhCompiler remoteCompiler;

  /**
   * コントローラを初期化する.
   *
   * @param wss ワークスペースセット
   * @param workspaceSetTab ワークスペース表示用のタブペイン
   */
  public boolean initialize(WorkspaceSet wss, TabPane workspaceSetTab) {
    copyBtn.setOnAction(action -> copy(wss)); // コピー
    cutBtn.setOnAction(action -> cut(wss)); // カット
    pasteBtn.setOnAction(action -> paste(wss, workspaceSetTab)); // ペースト
    deleteBtn.setOnAction(action -> delete(wss));
    jumpBtn.setOnAction(action -> jump(wss)); // ジャンプ
    undoBtn.setOnAction(action -> undo(wss)); // アンドゥ
    redoBtn.setOnAction(action -> redo(wss)); // リドゥ
    zoomInBtn.setOnAction(action -> zoomIn(wss)); // ズームイン
    zoomOutBtn.setOnAction(action -> zoomOut(wss)); // ズームアウト
    widenBtn.setOnAction(action -> widen(wss)); // ワークスペースの領域拡大
    narrowBtn.setOnAction(action -> narrow(wss)); // ワークスペースの領域縮小
    addWorkspaceBtn.setOnAction(action -> addWorkspace(wss)); // ワークスペース追加
    executeBtn.setOnAction(action -> execute(wss)); // プログラム実行
    terminateBtn.setOnAction(action -> terminate()); // プログラム終了
    connectBtn.setOnAction(action -> connect()); // 接続
    disconnectBtn.setOnAction(action -> disconnect()); // 切断
    focusSimBtn.setOnAction(action -> focusSimulator(true)); // シミュレータにフォーカス
    sendBtn.setOnAction(action -> send()); // 送信
    remotLocalSelectBtn.selectedProperty()
        .addListener((observable, oldVal, newVal) -> switchRemoteLocal(newVal));
    setHandlersToChangeButtonEnable(wss);    
    locaCompiler = genCompiler(true).orElse(null);
    remoteCompiler = genCompiler(false).orElse(null);
    return (locaCompiler != null) && (remoteCompiler != null);
  }

  /** {@link BhCompiler} オブジェクトを作成する. */
  private Optional<BhCompiler> genCompiler(boolean isLocal) {
    var commonLibPath = Paths.get(
        Utility.execPath,
        BhConstants.Path.BH_DEF_DIR,
        BhConstants.Path.FUNCTIONS_DIR,
        BhConstants.Path.lib,
        BhConstants.Path.COMMON_CODE_JS);
    
    var localOrRemoteLibPath = Paths.get(
        Utility.execPath,
        BhConstants.Path.BH_DEF_DIR,
        BhConstants.Path.FUNCTIONS_DIR,
        BhConstants.Path.lib,
        isLocal ? BhConstants.Path.LOCAL_COMMON_CODE_JS : BhConstants.Path.REMOTE_COMMON_CODE_JS);
    try {
      return Optional.of(new BhCompiler(commonLibPath, localOrRemoteLibPath));
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Failed to initialize Compiler.\n%s".formatted(e));
    }
    return Optional.empty();
  }

  /** コピーボタン押下時の処理. */
  private void copy(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      wss.clearCopyList(userOpe);
      wss.clearCutList(userOpe);
      currentWs.getSelectedNodes().forEach(node -> wss.addNodeToCopyList(node, userOpe));
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** カットボタン押下時の処理. */
  private void cut(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      wss.clearCopyList(userOpe);
      wss.clearCutList(userOpe);
      currentWs.getSelectedNodes().forEach(node -> wss.addNodeToCutList(node, userOpe));
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** ペーストボタン押下時の処理. */
  private void paste(WorkspaceSet wss, TabPane workspaceSetTab) {
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      javafx.geometry.Point2D pos =
          workspaceSetTab.localToScene(0, workspaceSetTab.getHeight() / 3.0);
      var posOnScene = new Vec2D(pos.getX(), pos.getY());
      Vec2D localPos = currentWs.getViewProxy().sceneToWorkspace(posOnScene);
      double pastePosX = localPos.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      double pastePosY = localPos.y;
      UserOperation userOpe = new UserOperation();
      wss.paste(currentWs, new Vec2D(pastePosX, pastePosY), userOpe);
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** デリートボタン押下時の処理. */
  private void delete(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      var candidates = currentWs.getSelectedNodes();
      var nodesToDelete = candidates.stream()
          .filter(node -> node.getEventAgent().execOnDeletionRequested(
              candidates, CauseOfDeletion.SELECTED_FOR_DELETION, userOpe))
          .collect(Collectors.toCollection(ArrayList::new));
      List<Swapped> swappedNodes = BhService.bhNodePlacer().deleteNodes(nodesToDelete, userOpe);
      for (var swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            userOpe);
      }
      BhService.compileErrNodeManager().updateErrorNodeIndicator(userOpe);
      BhService.compileErrNodeManager().unmanageNonErrorNodes(userOpe);
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** ジャンプボタン押下時の処理. */
  private void jump(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      findNodeToJumpTo(wss).ifPresent(node -> {
        node.getViewProxy().lookAt();
        var userOpe = new UserOperation();
        node.getWorkspace().getSelectedNodes().forEach(selected -> selected.deselect(userOpe));
        node.select(userOpe);
        BhService.undoRedoAgent().pushUndoCommand(userOpe);
      });
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** アンドゥボタン押下時の処理. */
  private void undo(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      BhService.undoRedoAgent().undo();
      wss.setDirty();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** リドゥボタン押下時の処理. */
  private void redo(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      BhService.undoRedoAgent().redo();
      wss.setDirty();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** ズームインボタン押下時の処理. */
  private void zoomIn(WorkspaceSet wss) {
    BhNodeSelectionViewProxy selectionViewProxy = wss.getAppRoot().getNodeSelectionViewProxy();
    if (selectionViewProxy.isAnyShowed()) {
      selectionViewProxy.zoom(true);
      return;
    }
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    currentWs.getViewProxy().zoom(true);
  }

  /** ズームアウトボタン押下時の処理. */
  private void zoomOut(WorkspaceSet wss) {
    BhNodeSelectionViewProxy selectionViewProxy = wss.getAppRoot().getNodeSelectionViewProxy();
    if (selectionViewProxy.isAnyShowed()) {
      selectionViewProxy.zoom(false);
      return;
    }
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    currentWs.getViewProxy().zoom(false);
  }

  /** ワークスペース拡大ボタン押下時の処理. */
  private void widen(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    currentWs.getViewProxy().changeViewSize(true);
  }

  /** ワークスペース縮小ボタン押下時の処理. */
  private void narrow(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    currentWs.getViewProxy().changeViewSize(false);
  }

  /** ワークスペース追加ボタン押下時の処理. */
  private void addWorkspace(WorkspaceSet wss) {
    ModelExclusiveControl.lockForModification();
    try {
      String wsName =
          TextDefs.Workspace.defaultWsName.get(wss.getWorkspaces().size() + 1);
      TextInputDialog dialog = new TextInputDialog(wsName);
      dialog.setTitle(TextDefs.Workspace.PromptToNameWs.title.get());
      dialog.setHeaderText(null);
      dialog.setContentText(TextDefs.Workspace.PromptToNameWs.body.get());
      dialog.getDialogPane().getStylesheets().addAll(menuPanelBase.getScene().getStylesheets());
      wsName = dialog.showAndWait().orElse(null);
      if (wsName == null) {
        return;
      }
      var userOpe = new UserOperation();
      createWorkspace(wsName).ifPresent(ws -> wss.addWorkspace(ws, userOpe));
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /** {@link Workspace} とその MVC 構造を作成する. */
  private Optional<Workspace> createWorkspace(String name) {
    Workspace ws = new Workspace(name);
    WorkspaceView wsView;
    try {
      wsView = new WorkspaceView(
          ws, BhConstants.LnF.DEFAULT_WORKSPACE_WIDTH, BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT);
      new WorkspaceController(ws, wsView, new MultiNodeShifterView());
    } catch (ViewInitializationException e) {
      BhService.msgPrinter().errForDebug(e.toString());
      return Optional.empty();
    }
    return Optional.of(ws);
  }


  /** 実行ボタン押下時の処理. */
  private void execute(WorkspaceSet wss) {
    ModelExclusiveControl.lockForRead();
    Optional<Pair<NodeGraphSnapshot, BhNode>> snapshotAndNodeToExecOpt = Optional.empty();
    try {
      if (preparingForExecution.get()) {
        BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.AlreadyDoing.preparation.get());
        return;
      }
      var userOpe = new UserOperation();
      snapshotAndNodeToExecOpt = CompileNodeCollector.collect(wss, userOpe);
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
    } finally {
      ModelExclusiveControl.unlockForRead();
    }
    if (snapshotAndNodeToExecOpt.isEmpty()) {
      return;
    }
    preparingForExecution.set(true);
    var snapshotAndNodeToExec = snapshotAndNodeToExecOpt.get();
    compileExec.submit(() -> {
      compile(snapshotAndNodeToExec.v1, snapshotAndNodeToExec.v2)
          .ifPresentOrElse(
              this::execute,
              () -> preparingForExecution.set(false));
    });
  }

  /**
   * 引数で指定したソースファイルのコードを実行する.
   *
   * @param srcPath 実行するソースファイルのパス
   */
  private void execute(Path srcPath) {
    Future<Boolean> future = startProgram(srcPath);
    boolean success = waitForTaskToComplete(future, "Execute");
    if (BhSettings.BhSimulator.focusOnStartBhProgram.get() && success) {
      focusSimulator(false);
    }
    preparingForExecution.set(false);
  }

  /**
   * ノードをコンパイルする.
   *
   * @param snapshot コンパイル対象のノードのスナップショット
   * @param nodeToExec 実行対象のノード
   * @return ノードをコンパイルしてできたソースファイルのパス
   */
  private Optional<Path> compile(NodeGraphSnapshot snapshot, BhNode nodeToExec) {
    CompileOption option = new CompileOption.Builder().build();
    Collection<BhNode> nodesToCompile = snapshot.getRootNodeList();
    BhCompiler compiler = isLocalHost() ? locaCompiler : remoteCompiler;
    Optional<Path> execFilePath = compiler.compile(nodeToExec, nodesToCompile, option);
    return execFilePath;
  }

  /**
   * コンパイルしたプログラムを実行する.
   *
   * @param filePath 実行するプログラムのファイルパス
   * @return プログラム起動タスクの Future オブジェクト
   */
  private Future<Boolean> startProgram(Path filePath) {
    if (isLocalHost()) {
      return BhRuntimeService.local().executeAsync(
          filePath, BhConstants.BhRuntime.LOLCAL_HOST);
    }
    return BhRuntimeService.remote().executeAsync(
        filePath,
        ipAddrTextField.getText(),
        unameTextField.getText(),
        passwordTextField.getText());
  }

  /** 終了ボタン押下時の処理. */
  private void terminate() {
    if (preparingForTermination.get()) {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.AlreadyDoing.termination.get());
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhRuntimeService.local().terminateAsync();
    } else {
      future = BhRuntimeService.remote().terminateAsync();
    }
    preparingForTermination.set(true);
    waitTaskExec.submit(() -> {
      waitForTaskToComplete(future, "Terminate");
      preparingForTermination.set(false);
    });
  }

  /** 切断ボタン押下時の処理. */
  private void disconnect() {
    if (disconnecting.get()) {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.AlreadyDoing.disconnection.get());
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhRuntimeService.local().disconnectAsync();
    } else {
      future = BhRuntimeService.remote().disconnectAsync();
    }
    disconnecting.set(true);
    waitTaskExec.submit(() -> {
      waitForTaskToComplete(future, "Disconnect");
      disconnecting.set(false);
    });
  }

  /** 接続ボタン押下時の処理. */
  private void connect() {
    if (connecting.get()) {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.AlreadyDoing.connection.get());
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhRuntimeService.local().connectAsync();
    } else {
      future = BhRuntimeService.remote().connectAsync();
    }
    connecting.set(true);
    waitTaskExec.submit(() -> {
      waitForTaskToComplete(future, "Connect");
      connecting.set(false);
    });
  }

  /** シミュレータにフォーカスする. */
  private void focusSimulator(boolean doForcibly) {
    Lwjgl3Window window = ((Lwjgl3Graphics) Gdx.app.getGraphics()).getWindow();
    if (!window.isIconified() || doForcibly) {
      window.restoreWindow();
      window.focusWindow();
    }
  }

  /**
   * Future オブジェクトを使ってタスクの終了を待つ.
   *
   * @param future 完了を待つタスクの Future オブジェクト
   * @param taskName 完了を待つタスク名
   * @return 完了したタスクの実行結果. 完了待ちに失敗した場合は null.
   */
  private <T> T waitForTaskToComplete(Future<T> future, String taskName) {
    try {
      return future.get();
    } catch (Exception e) {
      BhService.msgPrinter().println("%s\n%s".formatted(taskName, e));
    }
    return null;
  }

  /** 送信ボタン押下時の処理. */
  private void send() throws AssertionError {
    BhRuntimeStatus status;
    var cmd = new InputTextCmd(stdInTextField.getText());
    if (isLocalHost()) {
      status = BhRuntimeService.local().sendAsync(cmd);
    } else {
      status = BhRuntimeService.remote().sendAsync(cmd);
    }
    switch (status) {
      case SEND_QUEUE_FULL:
        BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Communication.failedToPushText.get());
        break;

      case SEND_WHEN_DISCONNECTED:
        BhService.msgPrinter().errForUser(
            TextDefs.BhRuntime.Communication.failedToSendTextForNoConnection.get());
        break;

      case SUCCESS:
        BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Communication.hasSentText.get());
        break;

      default:
        throw new AssertionError("Invalid status code" + status);
    }
  }

  /** リモート/セレクトを切り替えた時の処理. */
  private void switchRemoteLocal(Boolean newVal) {
    if (newVal) {
      ipAddrTextField.setDisable(false);
      unameTextField.setDisable(false);
      passwordTextField.setDisable(false);
      remotLocalSelectBtn.setText(TextDefs.Gui.MenuPanel.remote.get());
    } else {
      ipAddrTextField.setDisable(true);
      unameTextField.setDisable(true);
      passwordTextField.setDisable(true);
      remotLocalSelectBtn.setText(TextDefs.Gui.MenuPanel.local.get());
    }
  }

  /** ボタンの有効/無効状態を変化させるイベントハンドラを設定する. */
  private void setHandlersToChangeButtonEnable(WorkspaceSet wss) {
    pasteBtn.setDisable(true);
    wss.addOnCopyNodeAdded(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss), true);
    wss.addOnCopyNodeRemoved(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss), true);
    wss.addOnCutNodeAdded(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss), true);
    wss.addOnCutNodeRemoved(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss), true);
    wss.addOnCurrentWorkspaceChanged(
        (oldWs, newWs) -> jumpBtn.setDisable(findNodeToJumpTo(wss).isEmpty()), true);
  }

  /** ペーストボタンの有効/無効を切り替える. */
  private void changePasteButtonEnable(WorkspaceSet wss) {
    boolean disable = wss.getCopyList().isEmpty() && wss.getCutList().isEmpty();
    pasteBtn.setDisable(disable);
  }

  /**
   * ジャンプ先のノードを探す.
   *
   * @param wss このワークスペースセットの中からジャンプ先ノードを探す
   * @retun ジャンプ先ノード
   */
  private Optional<BhNode> findNodeToJumpTo(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null || currentWs.getSelectedNodes().size() != 1) {
      jumpBtn.setDisable(true);
      return Optional.empty();
    }
    return Optional.ofNullable(currentWs.getSelectedNodes().getFirst().getOriginal());
  }

  /** IP アドレス入力欄にローカルホストが指定してある場合 true を返す. */
  public boolean isLocalHost() {
    return !remotLocalSelectBtn.isSelected();
  }

  /**
   * ユーザメニュー操作のイベントを起こす.
   *
   * @param op 基本操作を表す列挙子
   */
  public void fireEvent(MenuOperation op) {
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
        throw new AssertionError("Invalid menu operation " + op);
    }
  }

  /** ユーザメニューの操作. */
  public enum MenuOperation {
    COPY,
    CUT,
    PASTE,
    DELETE,
    UNDO,
    REDO,
  }
}

















