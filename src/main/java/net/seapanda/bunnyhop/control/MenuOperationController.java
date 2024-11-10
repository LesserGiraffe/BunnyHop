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
import java.nio.file.Path;
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
import net.seapanda.bunnyhop.bhprogram.BhProgramService;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.InputTextCmd;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.BhSettings;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.CompileNodeCollector;
import net.seapanda.bunnyhop.compiler.CompileOption;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.NodeGraphSnapshot;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.modelservice.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionService;

/**
 * 画面上部のボタンのコントローラクラス.
 *
 * @author K.Koike
 */
public class MenuOperationController {

  /** ボタンの基底ペイン. */
  private @FXML VBox menuOpViewBase;
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

  /**
   * イベントハンドラをセットする.
   *
   * @param wss ワークスペースセット
   * @param workspaceSetTab ワークスペース表示用のタブペイン
   */
  public void init(WorkspaceSet wss, TabPane workspaceSetTab) {
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
  }

  /** コピーボタン押下時の処理. */
  private void copy(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      wss.addNodesToCopyList(currentWs.getSelectedNodeList(), userOpe);
      BunnyHop.INSTANCE.pushUserOperation(userOpe);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** カットボタン押下時の処理. */
  private void cut(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      wss.addNodesToCutList(currentWs.getSelectedNodeList(), userOpe);
      BunnyHop.INSTANCE.pushUserOperation(userOpe);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** ペーストボタン押下時の処理. */
  private void paste(WorkspaceSet wss, TabPane workspaceSetTab) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      javafx.geometry.Point2D pos =
          workspaceSetTab.localToScene(0, workspaceSetTab.getHeight() / 3.0);
      Vec2D localPos = MsgService.INSTANCE.sceneToWorkspace(pos.getX(), pos.getY(), currentWs);
      double pastePosX = localPos.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      double pastePosY = localPos.y;
      wss.paste(currentWs, new Vec2D(pastePosX, pastePosY));
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** デリートボタン押下時の処理. */
  private void delete(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      UserOperation userOpe = new UserOperation();
      var candidates = currentWs.getSelectedNodeList();
      var nodesToDelete = candidates.stream()
          .filter(node -> node.getEventAgent().execOnDeletionRequested(
              candidates, CauseOfDeletion.SELECTED_FOR_DELETION, userOpe))
          .collect(Collectors.toCollection(ArrayList::new));
      List<Swapped> swappedNodes =
          BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpe);
      for (var swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            userOpe);
      }
      SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpe);
      SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(userOpe);
      BunnyHop.INSTANCE.pushUserOperation(userOpe);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** ジャンプボタン押下時の処理. */
  private void jump(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      findNodeToJumpTo(wss).ifPresent(node -> {
        MsgService.INSTANCE.lookAt(node);
        UserOperation userOpe = new UserOperation();
        node.getWorkspace().clearSelectedNodeList(userOpe);
        node.getWorkspace().addSelectedNode(node, userOpe);
        BunnyHop.INSTANCE.pushUserOperation(userOpe);
      });
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** アンドゥボタン押下時の処理. */
  private void undo(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      MsgTransporter.INSTANCE.sendMessage(BhMsg.UNDO, wss);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** リドゥボタン押下時の処理. */
  private void redo(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      MsgTransporter.INSTANCE.sendMessage(BhMsg.REDO, wss);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** ズームインボタン押下時の処理. */
  private void zoomIn(WorkspaceSet wss) {
    if (BhNodeSelectionService.INSTANCE.isAnyShowed()) {
      BhNodeSelectionService.INSTANCE.zoomAll(true);
      return;
    }
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(true), currentWs);
  }

  /** ズームインボタン押下時の処理. */
  private void zoomOut(WorkspaceSet wss) {
    if (BhNodeSelectionService.INSTANCE.isAnyShowed()) {
      BhNodeSelectionService.INSTANCE.zoomAll(false);
      return;
    }
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(false), currentWs);
  }

  /** ワークスペース拡大ボタン押下時の処理. */
  private void widen(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(true), currentWs);
  }

  /** ワークスペース縮小ボタン押下時の処理. */
  private void narrow(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null) {
      return;
    }
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.CHANGE_WORKSPACE_VIEW_SIZE, new MsgData(false), currentWs);
  }

  /** ワークスペース追加ボタン押下時の処理. */
  private void addWorkspace(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      String defaultWsName = "ワークスペース" + (wss.getWorkspaceList().size() + 1);
      TextInputDialog dialog = new TextInputDialog(defaultWsName);
      dialog.setTitle("ワークスペースの作成");
      dialog.setHeaderText(null);
      dialog.setContentText("ワークスペース名を入力してください");
      dialog.getDialogPane().getStylesheets().addAll(BunnyHop.INSTANCE.getAllStyles());
      Optional<String> inputText = dialog.showAndWait();
      inputText.ifPresent(wsName -> {
        UserOperation userOpe = new UserOperation();
        BunnyHop.INSTANCE.addNewWorkSpace(
            wsName,
            BhConstants.LnF.DEFAULT_WORKSPACE_WIDTH,
            BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT,
            userOpe);
        BunnyHop.INSTANCE.pushUserOperation(userOpe);
      });
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** 実行ボタン押下時の処理. */
  private void execute(WorkspaceSet wss) {
    ModelExclusiveControl.INSTANCE.lockForRead();
    Optional<Pair<NodeGraphSnapshot, BhNode>> snapshotAndNodeToExecOpt = Optional.empty();
    try {
      if (preparingForExecution.get()) {
        MsgPrinter.INSTANCE.errMsgForUser("!! 実行準備中 !!\n");
        return;
      }
      BhNodeSelectionService.INSTANCE.hideAll();
      var userOpe = new UserOperation();
      snapshotAndNodeToExecOpt = CompileNodeCollector.collect(wss, userOpe);
      BunnyHop.INSTANCE.pushUserOperation(userOpe);
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForRead();
    }
    if (snapshotAndNodeToExecOpt.isEmpty()) {
      return;
    }
    preparingForExecution.set(true);
    var snapshotAndNodeToExec = snapshotAndNodeToExecOpt.get();
    compileExec.submit(() -> {
      Optional<Path> srcFilePath = compile(snapshotAndNodeToExec);
      srcFilePath.ifPresentOrElse(
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
   * @param snapshotAndNodeToExec コンパイル対象のノードのスナップショットと実行ノードのペア
   * @return ノードをコンパイルしてできたソースファイルのパス
   */
  private Optional<Path> compile(Pair<NodeGraphSnapshot, BhNode> snapshotAndNodeToExec) {
    CompileOption option = new CompileOption.Builder(isLocalHost()).build();
    Collection<BhNode> nodesToCompile = snapshotAndNodeToExec.v1.getRootNodeList();
    BhNode nodeToExec = snapshotAndNodeToExec.v2;
    Optional<Path> execFilePath = BhCompiler.INSTANCE.compile(nodeToExec, nodesToCompile, option);
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
      return BhProgramService.local().executeAsync(
          filePath, BhConstants.BhRuntime.LOLCAL_HOST);
    }
    return BhProgramService.remote().executeAsync(
        filePath,
        ipAddrTextField.getText(),
        unameTextField.getText(),
        passwordTextField.getText());
  }

  /** 終了ボタン押下時の処理. */
  private void terminate() {
    if (preparingForTermination.get()) {
      MsgPrinter.INSTANCE.errMsgForUser("!! 終了準備中 !!\n");
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhProgramService.local().terminateAsync();
    } else {
      future = BhProgramService.remote().terminateAsync();
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
      MsgPrinter.INSTANCE.errMsgForUser("!! 切断準備中 !!\n");
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhProgramService.local().disconnectAsync();
    } else {
      future = BhProgramService.remote().disconnectAsync();
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
      MsgPrinter.INSTANCE.errMsgForUser("!! 接続準備中 !!\n");
      return;
    }
    Future<Boolean> future;
    if (isLocalHost()) {
      future = BhProgramService.local().connectAsync();
    } else {
      future = BhProgramService.remote().connectAsync();
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
      MsgPrinter.INSTANCE.msgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + taskName + "  " + e);
    }
    return null;
  }

  /** 送信ボタン押下時の処理. */
  private void send() throws AssertionError {
    BhRuntimeStatus status;
    var cmd = new InputTextCmd(stdInTextField.getText());
    if (isLocalHost()) {
      status = BhProgramService.local().sendAsync(cmd);
    } else {
      status = BhProgramService.remote().sendAsync(cmd);
    }
    switch (status) {
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
        throw new AssertionError("invalid status code" + status);
    }
  }

  /** リモート/セレクトを切り替えた時の処理. */
  private void switchRemoteLocal(Boolean newVal) {
    if (newVal) {
      ipAddrTextField.setDisable(false);
      unameTextField.setDisable(false);
      passwordTextField.setDisable(false);
      remotLocalSelectBtn.setText("リモート");
    } else {
      ipAddrTextField.setDisable(true);
      unameTextField.setDisable(true);
      passwordTextField.setDisable(true);
      remotLocalSelectBtn.setText("ローカル");
    }
  }

  /** ボタンの有効/無効状態を変化させるイベントハンドラを設定する. */
  private void setHandlersToChangeButtonEnable(WorkspaceSet wss) {
    pasteBtn.setDisable(true);
    wss.addOnCopyListChanged(change -> changePasteButtonEnable(wss), true);
    wss.addOnCutListChanged(change -> changePasteButtonEnable(wss), true);
    wss.addOnSelectedNodeListChanged(
        (ws, list) -> jumpBtn.setDisable(findNodeToJumpTo(wss).isEmpty()), true);
    wss.addOnCurrentWorkspaceChanged(
        (oldWs, newWs) -> jumpBtn.setDisable(findNodeToJumpTo(wss).isEmpty()), true);
  }

  /** ペーストボタンの有効/無効を切り替える. */
  private void changePasteButtonEnable(WorkspaceSet wss) {
    boolean disable = wss.isCopyListEmpty() && wss.isCutListEmpty();
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
    if (currentWs == null || currentWs.getSelectedNodeList().size() != 1) {
      jumpBtn.setDisable(true);
      return Optional.empty();
    }
    return Optional.ofNullable(currentWs.getSelectedNodeList().get(0).getOriginal());
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
        throw new AssertionError("invalid menu operation " + op);
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

















