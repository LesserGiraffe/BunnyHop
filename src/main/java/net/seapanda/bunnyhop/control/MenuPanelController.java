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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.BhProgramController;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.ExecutableNodeSet;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.InputTextCmd;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.ExecutableNodeCollector;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactory;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.workspace.CopyAndPaste;
import net.seapanda.bunnyhop.model.workspace.CutAndPaste;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;

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

  /** BhProgramの実行環境準備中の場合 true. */
  private final AtomicBoolean executing = new AtomicBoolean(false);
  /** BhProgramの実行環境終了中の場合 true. */
  private final AtomicBoolean terminating = new AtomicBoolean(false);
  /** 接続中の場合 true. */
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  /** 切断中の場合 true. */
  private final AtomicBoolean disconnecting = new AtomicBoolean(false);
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private ModelAccessNotificationService notificationService;
  /** ワークスペース作成用オブジェクト. */
  private WorkspaceFactory wsFactory;
  /** Undo / Redo の実行に使用するオブジェクト. */
  private UndoRedoAgent undoRedoAgent;
  /** ノード選択ビューを操作するときに使用するオブジェクト. */
  private BhNodeSelectionViewProxy proxy;
  /** ローカルマシン上での BhProgram の実行を制御するオブジェクト. */
  private BhProgramController localCtrl;
  /** リモートマシン上での BhProgram の実行を制御するオブジェクト. */
  private BhProgramController remoteCtrl;
  /** コピー & ペーストの処理に使用するオブジェクト. */
  private CopyAndPaste copyAndPaste;
  /** カット & ペーストの処理に使用するオブジェクト. */
  private CutAndPaste cutAndPaste;
  /** アプリケーションユーザにメッセージを出力するためのオブジェクト. */
  private MessageService msgService;

  /**
   * コントローラを初期化する.
   *
   * @param wssCtrl ワークスペースセットのコントローラオブジェクト
   * @param notificationService モデルへのアクセスの通知先となるオブジェクト
   * @param wsFactory このオブジェクトを使ってワークスペースを作成する
   * @param undoRedoAgent Undo / Redo の実行に使用するオブジェクト
   * @param proxy このオブジェクトを使ってノード選択ビューを操作する
   * @param localCtrl ローカルマシン上での BhProgram の実行を制御するオブジェクト
   * @param remoteCtrl リモートマシン上での BhProgram の実行を制御するオブジェクト
   * @param copyAndPaste コピー & ペーストの処理に使用するオブジェクト
   * @param cutAndPaste カット & ペーストの処理に使用するオブジェクト
   * @return 成功した場合 true
   */
  public boolean initialize(
      WorkspaceSetController wssCtrl,
      ModelAccessNotificationService notificationService,
      WorkspaceFactory wsFactory,
      UndoRedoAgent undoRedoAgent,
      BhNodeSelectionViewProxy proxy,
      BhProgramController localCtrl,
      BhProgramController remoteCtrl,
      CopyAndPaste copyAndPaste,
      CutAndPaste cutAndPaste,
      MessageService msgService) {
    this.notificationService = notificationService;
    this.wsFactory = wsFactory;
    this.undoRedoAgent = undoRedoAgent;
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
    this.copyAndPaste = copyAndPaste;
    this.cutAndPaste = cutAndPaste;
    this.msgService = msgService;
    WorkspaceSet wss = wssCtrl.getWorkspaceSet();
    copyBtn.setOnAction(action -> copy(wss)); // コピー
    cutBtn.setOnAction(action -> cut(wss)); // カット
    pasteBtn.setOnAction(action -> paste(wss, wssCtrl.getTabPane())); // ペースト
    deleteBtn.setOnAction(action -> delete(wss));
    jumpBtn.setOnAction(action -> jump(wss)); // ジャンプ
    undoBtn.setOnAction(action -> undo()); // アンドゥ
    redoBtn.setOnAction(action -> redo()); // リドゥ
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
    return true;
  }

  /** コピーボタン押下時の処理. */
  private void copy(WorkspaceSet wss) {
    Context context = notificationService.begin();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      copyAndPaste.clearList(context.userOpe());
      cutAndPaste.clearList(context.userOpe());
      currentWs.getSelectedNodes().forEach(
          node -> copyAndPaste.addNodeToList(node, context.userOpe()));
    } finally {
      notificationService.end();
    }
  }

  /** カットボタン押下時の処理. */
  private void cut(WorkspaceSet wss) {
    Context context = notificationService.begin();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      copyAndPaste.clearList(context.userOpe());
      cutAndPaste.clearList(context.userOpe());
      currentWs.getSelectedNodes().forEach(
          node -> cutAndPaste.addNodeToList(node, context.userOpe()));
    } finally {
      notificationService.end();
    }
  }

  /** ペーストボタン押下時の処理. */
  private void paste(WorkspaceSet wss, TabPane workspaceSetTab) {
    Context context = notificationService.begin();
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
      Vec2D pastePos = new Vec2D(pastePosX, pastePosY);
      copyAndPaste.paste(currentWs, pastePos, context.userOpe());
      cutAndPaste.paste(currentWs, pastePos, context.userOpe());
    } finally {
      notificationService.end();
    }
  }

  /** デリートボタン押下時の処理. */
  private void delete(WorkspaceSet wss) {
    Context context = notificationService.begin();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      var candidates = currentWs.getSelectedNodes();
      var nodesToDelete = candidates.stream()
          .filter(node -> node.getEventInvoker().onDeletionRequested(
              candidates, CauseOfDeletion.SELECTED_FOR_DELETION, context.userOpe()))
          .collect(Collectors.toCollection(ArrayList::new));
      List<Swapped> swappedNodes = BhNodePlacer.deleteNodes(nodesToDelete, context.userOpe());
      for (var swapped : swappedNodes) {
        swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
            swapped.oldNode(),
            swapped.newNode(),
            swapped.newNode().getParentConnector(),
            context.userOpe());
      }
    } finally {
      notificationService.end();
    }
  }

  /** ジャンプボタン押下時の処理. */
  private void jump(WorkspaceSet wss) {
    Context context = notificationService.begin();
    try {
      findNodeToJumpTo(wss).ifPresent(node -> {
        node.getViewProxy().lookAt();
        node.getWorkspace().getSelectedNodes().forEach(
            selected -> selected.deselect(context.userOpe()));
        node.select(context.userOpe());
      });
    } finally {
      notificationService.end();
    }
  }

  /** アンドゥボタン押下時の処理. */
  private void undo() {
    notificationService.begin();
    try {
      undoRedoAgent.undo();
    } finally {
      notificationService.end();
    }
  }

  /** リドゥボタン押下時の処理. */
  private void redo() {
    notificationService.begin();
    try {
      undoRedoAgent.redo();
    } finally {
      notificationService.end();
    }
  }

  /** ズームインボタン押下時の処理. */
  private void zoomIn(WorkspaceSet wss) {
    notificationService.begin();
    try {
      if (proxy.isAnyShowed()) {
        proxy.zoom(true);
        return;
      }
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      currentWs.getViewProxy().zoom(true);
    } finally {
      notificationService.end();
    }
  }

  /** ズームアウトボタン押下時の処理. */
  private void zoomOut(WorkspaceSet wss) {
    notificationService.begin();
    try {
      if (proxy.isAnyShowed()) {
        proxy.zoom(false);
        return;
      }
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      currentWs.getViewProxy().zoom(false);
    } finally {
      notificationService.end();
    }
  }

  /** ワークスペース拡大ボタン押下時の処理. */
  private void widen(WorkspaceSet wss) {
    notificationService.begin();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      currentWs.getViewProxy().changeViewSize(true);
    } finally {
      notificationService.end();
    }
  }

  /** ワークスペース縮小ボタン押下時の処理. */
  private void narrow(WorkspaceSet wss) {
    notificationService.begin();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      currentWs.getViewProxy().changeViewSize(false);
    } finally {
      notificationService.end();
    }
  }

  /** ワークスペース追加ボタン押下時の処理. */
  private void addWorkspace(WorkspaceSet wss) {
    Context context = notificationService.begin();
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
      createWorkspace(wsName).ifPresent(ws -> wss.addWorkspace(ws, context.userOpe()));
    } finally {
      notificationService.end();
    }
  }

  /** {@link Workspace} とその MVC 構造を作成する. */
  private Optional<Workspace> createWorkspace(String name) {    
    Workspace ws = wsFactory.create(name);
    try {
      Vec2D size = new Vec2D(
          BhConstants.LnF.DEFAULT_WORKSPACE_WIDTH, BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT);
      wsFactory.setMvc(ws, size);
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
      return Optional.empty();
    }
    return Optional.of(ws);
  }

  /** 実行ボタン押下時の処理. */
  private void execute(WorkspaceSet wss) {
    if (executing.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.execution.get());
      return;
    }
    ExecutableNodeSet nodeSet = collectExecutableNodes(wss);
    if (nodeSet == null) {
      return;
    }
    executing.set(true);
    Supplier<Boolean> exec = () -> isLocalHost()
        ? localCtrl.execute(nodeSet)
        : remoteCtrl.execute(
              nodeSet,
              ipAddrTextField.getText(),
              unameTextField.getText(),
              passwordTextField.getText());
    CompletableFuture
        .supplyAsync(exec)
        .thenAccept(success -> {
          if (BhSettings.BhSimulator.focusOnStartBhProgram.get() && success) {
            focusSimulator(false);
          }
          executing.set(false);
        });
  }

  /** {@code wss} から実行可能なノードを集める. */
  private ExecutableNodeSet collectExecutableNodes(WorkspaceSet wss) {
    Context context = notificationService.begin();
    Optional<ExecutableNodeSet> nodeSet = Optional.empty();
    try {
      nodeSet = ExecutableNodeCollector.collect(wss, msgService, context.userOpe());
    } catch (Exception e) {
      LogManager.logger().error(e.toString());
    } finally {
      notificationService.end();
    }
    return nodeSet.orElse(null);
  }

  /** 終了ボタン押下時の処理. */
  private void terminate() {
    if (terminating.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.termination.get());
      return;
    }
    terminating.set(true);
    BhProgramController ctrl = isLocalHost() ? localCtrl : remoteCtrl;
    CompletableFuture
        .supplyAsync(() -> ctrl.terminate())
        .thenAccept(success -> terminating.set(false));
  }

  /** 切断ボタン押下時の処理. */
  private void disconnect() {
    if (disconnecting.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.disconnection.get());
      return;
    }
    disconnecting.set(true);
    BhProgramController ctrl = isLocalHost() ? localCtrl : remoteCtrl;
    CompletableFuture
        .supplyAsync(() -> ctrl.disableCommunication())
        .thenAccept(success -> disconnecting.set(false));    
  }

  /** 接続ボタン押下時の処理. */
  private void connect() {
    if (connecting.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.connection.get());
      return;
    }
    connecting.set(true);
    BhProgramController ctrl = isLocalHost() ? localCtrl : remoteCtrl;
    CompletableFuture
        .supplyAsync(() -> ctrl.enableCommunication())
        .thenAccept(success -> connecting.set(false));    
  }

  /** シミュレータにフォーカスする. */
  private void focusSimulator(boolean doForcibly) {
    Lwjgl3Window window = ((Lwjgl3Graphics) Gdx.app.getGraphics()).getWindow();
    if (!window.isIconified() || doForcibly) {
      window.restoreWindow();
      window.focusWindow();
    }
  }

  /** 送信ボタン押下時の処理. */
  private void send() throws AssertionError {
    var cmd = new InputTextCmd(stdInTextField.getText());
    BhProgramController ctrl = isLocalHost() ? localCtrl : remoteCtrl;
    BhRuntimeStatus status = ctrl.send(cmd);
    switch (status) {
      case SEND_QUEUE_FULL:
        msgService.error(TextDefs.BhRuntime.Communication.failedToPushText.get());
        break;

      case SEND_WHEN_DISCONNECTED:
        msgService.error(TextDefs.BhRuntime.Communication.failedToSendTextForNoConnection.get());
        break;

      case SUCCESS:
        msgService.info(TextDefs.BhRuntime.Communication.hasSentText.get());
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
    copyAndPaste.getEventManager().addOnCopyNodeAdded(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss));
    copyAndPaste.getEventManager().addOnCopyNodeRemoved(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss));
    cutAndPaste.getEventManager().addOnCutNodeAdded(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss));
    cutAndPaste.getEventManager().addOnCutNodeRemoved(
        (workspaceSet, node, userOpe) -> changePasteButtonEnable(wss));
    wss.getEventManager().addOnNodeSelectionStateChanged(
        (node, isSelected, userOpe) -> jumpBtn.setDisable(findNodeToJumpTo(wss).isEmpty()));
  }

  /** ペーストボタンの有効/無効を切り替える. */
  private void changePasteButtonEnable(WorkspaceSet wss) {
    boolean disable = copyAndPaste.getList().isEmpty() && cutAndPaste.getList().isEmpty();
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
    if (currentWs == null || currentWs.getSelectedNodes().size() == 0) {
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

















