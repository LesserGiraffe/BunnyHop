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

package net.seapanda.bunnyhop.ui.control;

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
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.seapanda.bunnyhop.bhprogram.ExecutableNodeCollector;
import net.seapanda.bunnyhop.bhprogram.ExecutableNodeSet;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncher;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramController;
import net.seapanda.bunnyhop.bhprogram.common.message.io.InputTextCmd;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.control.DebugWindowController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.view.BhNodeView.LookManager.EffectTarget;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSetController;
import net.seapanda.bunnyhop.workspace.model.CopyAndPaste;
import net.seapanda.bunnyhop.workspace.model.CutAndPaste;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.model.factory.WorkspaceFactory;

/**
 * 画面上部のボタンのコントローラクラス.
 *
 * @author K.Koike
 */
public class MenuViewController {

  /** ボタンの基底ペイン. */
  @FXML private VBox menuViewBase;
  /** コピーボタン. */
  @FXML private Button copyBtn;
  /** カットボタン. */
  @FXML private Button cutBtn;
  /** ペーストボタン. */
  @FXML private Button pasteBtn;
  /** デリートボタン. */
  @FXML private Button deleteBtn;
  /** アンドゥボタン. */
  @FXML private Button undoBtn;
  /** リドゥボタン. */
  @FXML private Button redoBtn;
  /** ズームインボタン. */
  @FXML private Button zoomInBtn;
  /** ズームアウトボタン. */
  @FXML private Button zoomOutBtn;
  /** ワークスペース拡張ボタン. */
  @FXML private Button widenBtn;
  /** ワークスペース縮小ボタン. */
  @FXML private Button narrowBtn;
  /** ワークスペース追加ボタン. */
  @FXML private Button addWorkspaceBtn;
  /** リモート/ローカル選択ボタン. */
  @FXML private ToggleButton bhRuntimeSelBtn;
  /** 実行ボタン. */
  @FXML private Button executeBtn;
  /** 終了ボタン. */
  @FXML private Button terminateBtn;
  /** 接続ボタン. */
  @FXML private Button connectBtn;
  /** 切断ボタン. */
  @FXML private Button disconnectBtn;
  /** シミュレータフォーカスボタン. */
  @FXML private ToggleButton focusSimBtn;
  /** ブレークポイントの設定の有効 / 無効切り替えボタン. */
  @FXML private ToggleButton breakpointBtn;
  /** デバッグウィンドウ表示ボタン. */
  @FXML private ToggleButton debugBtn;
  /** ジャンプボタン. */
  @FXML private Button jumpBtn;
  /** ホスト名入力欄. */
  @FXML private TextField hostNameTextField;
  /** ユーザ名. */
  @FXML private TextField unameTextField;
  /** ログインパスワード. */
  @FXML private PasswordField passwordTextField;
  /** 送信ボタン. */
  @FXML private Button sendBtn;
  /** 標準入力テキストフィールド. */
  @FXML private TextField stdInTextField;
  

  /** BhProgramの実行環境準備中の場合 true. */
  private final AtomicBoolean executing = new AtomicBoolean(false);
  /** BhProgramの実行環境終了中の場合 true. */
  private final AtomicBoolean terminating = new AtomicBoolean(false);
  /** 接続中の場合 true. */
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  /** 切断中の場合 true. */
  private final AtomicBoolean disconnecting = new AtomicBoolean(false);
  /** {@link WorkspaceSet} のコントローラオブジェクト. */
  private final WorkspaceSetController wssCtrl;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final ModelAccessNotificationService notifService;
  /** ワークスペース作成用オブジェクト. */
  private final WorkspaceFactory wsFactory;
  /** Undo / Redo の実行に使用するオブジェクト. */
  private final UndoRedoAgent undoRedoAgent;
  /** ノード選択ビューを操作するときに使用するオブジェクト. */
  private final BhNodeSelectionViewProxy proxy;
  /** ローカルマシン上での BhProgram の実行を制御するオブジェクト. */
  private final LocalBhProgramLauncher localCtrl;
  /** リモートマシン上での BhProgram の実行を制御するオブジェクト. */
  private final RemoteBhProgramController remoteCtrl;
  /** コピー & ペーストの処理に使用するオブジェクト. */
  private final CopyAndPaste copyAndPaste;
  /** カット & ペーストの処理に使用するオブジェクト. */
  private final CutAndPaste cutAndPaste;
  /** アプリケーションユーザにメッセージを出力するためのオブジェクト. */
  private final MessageService msgService;
  /** デバッグウィンドウのコントローラ. */
  private final DebugWindowController debugWindowCtrl;

  /** コンストラクタ. */
  public MenuViewController(
      WorkspaceSetController wssCtrl,
      ModelAccessNotificationService notifService,
      WorkspaceFactory wsFactory,
      UndoRedoAgent undoRedoAgent,
      BhNodeSelectionViewProxy proxy,
      LocalBhProgramLauncher localCtrl,
      RemoteBhProgramController remoteCtrl,
      CopyAndPaste copyAndPaste,
      CutAndPaste cutAndPaste,
      MessageService msgService,
      DebugWindowController debugWindowCtrl) {
    this.wssCtrl = wssCtrl;
    this.notifService = notifService;
    this.wsFactory = wsFactory;
    this.undoRedoAgent = undoRedoAgent;
    this.proxy = proxy;
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
    this.copyAndPaste = copyAndPaste;
    this.cutAndPaste = cutAndPaste;
    this.msgService = msgService;
    this.debugWindowCtrl = debugWindowCtrl;
  }

  /** このコントローラを初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    switchBhRuntime(BhSettings.BhRuntime.currentBhRuntimeType == BhRuntimeType.REMOTE);
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    WorkspaceSet wss = wssCtrl.getWorkspaceSet();
    copyBtn.setOnAction(action -> copy(wss)); // コピー
    cutBtn.setOnAction(action -> cut(wss)); // カット
    pasteBtn.setDisable(true);
    pasteBtn.setOnAction(action -> paste(wss, wssCtrl.getTabPane())); // ペースト
    deleteBtn.setOnAction(action -> delete(wss));
    jumpBtn.setOnAction(action -> jump(wss)); // ジャンプ
    undoBtn.setOnAction(action -> undo()); // アンドゥ
    redoBtn.setOnAction(action -> redo()); // リドゥ
    setZoomEventHandler(zoomInBtn, () -> zoomIn(wss));
    setZoomEventHandler(zoomOutBtn, () -> zoomOut(wss));
    widenBtn.setOnAction(action -> widen(wss)); // ワークスペースの領域拡大
    narrowBtn.setOnAction(action -> narrow(wss)); // ワークスペースの領域縮小
    addWorkspaceBtn.setOnAction(action -> addWorkspace(wss)); // ワークスペース追加
    executeBtn.setOnAction(action -> execute(wss)); // プログラム実行
    terminateBtn.setOnAction(action -> terminate()); // プログラム終了
    connectBtn.setOnAction(action -> connect()); // 接続
    disconnectBtn.setOnAction(action -> disconnect()); // 切断
    focusSimBtn.setOnAction(
        action -> BhSettings.BhSimulator.focusOnChanged = focusSimBtn.isSelected());
    breakpointBtn.setOnAction(
        action -> BhSettings.Debug.isBreakpointSettingEnabled  = breakpointBtn.isSelected() );
    debugBtn.setOnAction(action -> debugWindowCtrl.setVisibility(debugBtn.isSelected()));
    sendBtn.setOnAction(action -> send()); // 送信
    bhRuntimeSelBtn.selectedProperty()
        .addListener((observable, oldVal, newVal) -> switchBhRuntime(newVal));
    copyAndPaste.getCallbackRegistry().getOnNodeAdded().add(event -> changePasteButtonState());
    copyAndPaste.getCallbackRegistry().getOnNodeRemoved().add(event -> changePasteButtonState());
    cutAndPaste.getCallbackRegistry().getOnNodeAdded().add(event -> changePasteButtonState());
    cutAndPaste.getCallbackRegistry().getOnNodeRemoved().add(event -> changePasteButtonState());
    wss.getCallbackRegistry().getOnNodeSelectionStateChanged()
        .add(event -> jumpBtn.setDisable(findNodeToJumpTo(wss).isEmpty()));
  }

  /** コピーボタン押下時の処理. */
  private void copy(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
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
      notifService.endWrite();
    }
  }

  /** カットボタン押下時の処理. */
  private void cut(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
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
      notifService.endWrite();
    }
  }

  /** ペーストボタン押下時の処理. */
  private void paste(WorkspaceSet wss, TabPane workspaceSetTab) {
    Context context = notifService.beginWrite();
    try {
      Workspace currentWs = wss.getCurrentWorkspace();
      if (currentWs == null) {
        return;
      }
      javafx.geometry.Point2D pos =
          workspaceSetTab.localToScene(0, workspaceSetTab.getHeight() / 3.0);
      var posOnScene = new Vec2D(pos.getX(), pos.getY());
      Vec2D pastePos =
          currentWs.getView().map(wsv -> wsv.sceneToWorkspace(posOnScene))
          .orElse(new Vec2D());
      pastePos.add(BhConstants.LnF.REPLACED_NODE_SHIFT * 2, 0);
      copyAndPaste.paste(currentWs, pastePos, context.userOpe());
      cutAndPaste.paste(currentWs, pastePos, context.userOpe());
    } finally {
      notifService.endWrite();
    }
  }

  /** デリートボタン押下時の処理. */
  private void delete(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
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
      notifService.endWrite();
    }
  }

  /** ジャンプボタン押下時の処理. */
  private void jump(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
    try {
      findNodeToJumpTo(wss).ifPresent(node -> {
        node.getView().ifPresent(view -> ViewUtil.jump(view, true, EffectTarget.SELF));
        node.getWorkspace().getSelectedNodes().forEach(
            selected -> selected.deselect(context.userOpe()));
        node.select(context.userOpe());
      });
    } finally {
      notifService.endWrite();
    }
  }

  /** アンドゥボタン押下時の処理. */
  private void undo() {
    Context contex = notifService.beginWrite();
    try {
      undoRedoAgent.undo();
    } finally {
      // Undo 中に context の UserOperation に追加されたサブコマンドは Undo の対象にしない.
      // 一部の操作 (ワークスペースタブの切り替えなど) は Undo の実行中にサブコマンドをこの context の UserOperation に
      // 追加するが, Undo 中の当該操作の逆操作はサブコマンドの実行中に自動的に作られるので,
      // この context の UserOperation を Undo の対象にする必要はない.
      contex.userOpe().clearCmds();
      notifService.endWrite();
    }
  }

  /** リドゥボタン押下時の処理. */
  private void redo() {
    Context contex = notifService.beginWrite();
    try {
      undoRedoAgent.redo();
    } finally {
      contex.userOpe().clearCmds();
      notifService.endWrite();
    }
  }

  /** ズームインボタン押下時の処理. */
  private void zoomIn(WorkspaceSet wss) {
    notifService.beginWrite();
    try {
      if (proxy.getCurrentCategoryName().isPresent()) {
        proxy.zoom(true);
        return;
      }
      Optional.ofNullable(wss.getCurrentWorkspace())
          .flatMap(Workspace::getView)
          .ifPresent(wsv -> wsv.zoom(true));
    } finally {
      notifService.endWrite();
    }
  }

  /** ズームアウトボタン押下時の処理. */
  private void zoomOut(WorkspaceSet wss) {
    notifService.beginWrite();
    try {
      if (proxy.getCurrentCategoryName().isPresent()) {
        proxy.zoom(false);
        return;
      }
      Optional.ofNullable(wss.getCurrentWorkspace())
          .flatMap(Workspace::getView)
          .ifPresent(wsv -> wsv.zoom(false));
    } finally {
      notifService.endWrite();
    }
  }

  /** {@code zoomBtn} の押下した時と押しっぱなしにした時に {@code fnZoom} を実行するようにイベントハンドラを設定する. */
  private static void setZoomEventHandler(Button zoomBtn, Runnable fnZoom) {
    var pause = new PauseTransition(Duration.millis(50));
    pause.setOnFinished(event -> fnZoom.run());
    var seqTransition = new SequentialTransition(pause);
    seqTransition.setCycleCount(Animation.INDEFINITE);
    seqTransition.setDelay(Duration.millis(500));
    zoomBtn.setOnMousePressed(event -> seqTransition.play());
    zoomBtn.addEventHandler(MouseEvent.ANY, event -> {
      var eventType = event.getEventType();
      if (eventType == MouseEvent.MOUSE_PRESSED) {
        fnZoom.run();
        seqTransition.play();
      } else if (eventType == MouseEvent.MOUSE_RELEASED || eventType == MouseEvent.MOUSE_EXITED) {
        seqTransition.stop();
      }
    });
  }

  /** ワークスペース拡大ボタン押下時の処理. */
  private void widen(WorkspaceSet wss) {
    notifService.beginWrite();
    try {
      Optional.ofNullable(wss.getCurrentWorkspace())
          .flatMap(Workspace::getView)
          .ifPresent(wsv -> wsv.changeViewSize(true));
    } finally {
      notifService.endWrite();
    }
  }

  /** ワークスペース縮小ボタン押下時の処理. */
  private void narrow(WorkspaceSet wss) {
    notifService.beginWrite();
    try {
      Optional.ofNullable(wss.getCurrentWorkspace())
          .flatMap(Workspace::getView)
          .ifPresent(wsv -> wsv.changeViewSize(false));
    } finally {
      notifService.endWrite();
    }
  }

  /** ワークスペース追加ボタン押下時の処理. */
  private void addWorkspace(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
    try {
      String wsName =
          TextDefs.Workspace.defaultWsName.get(wss.getWorkspaces().size() + 1);
      TextInputDialog dialog = new TextInputDialog(wsName);
      dialog.setTitle(TextDefs.Workspace.PromptToNameWs.title.get());
      dialog.setHeaderText(null);
      dialog.setContentText(TextDefs.Workspace.PromptToNameWs.body.get());
      dialog.getDialogPane().getStylesheets().addAll(menuViewBase.getScene().getStylesheets());
      wsName = dialog.showAndWait().orElse(null);
      if (wsName == null) {
        return;
      }
      createWorkspace(wsName).ifPresent(ws -> wss.addWorkspace(ws, context.userOpe()));
    } finally {
      notifService.endWrite();
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
        ? localCtrl.launch(nodeSet)
        : remoteCtrl.launch(
              nodeSet,
              hostNameTextField.getText(),
              unameTextField.getText(),
              passwordTextField.getText());
    CompletableFuture
        .supplyAsync(exec)
        .thenAccept(success -> {
          if (BhSettings.BhSimulator.focusOnStartBhProgram && success && isLocalHost()) {
            focusSimulator(false);
          }
          executing.set(false);
        });
  }

  /** {@code wss} から実行可能なノードを集める. */
  private ExecutableNodeSet collectExecutableNodes(WorkspaceSet wss) {
    Context context = notifService.beginWrite();
    Optional<ExecutableNodeSet> nodeSet = Optional.empty();
    try {
      nodeSet = ExecutableNodeCollector.collect(wss, msgService, context.userOpe());
    } catch (Exception e) {
      LogManager.logger().error(e.toString());
    } finally {
      notifService.endWrite();
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
    Supplier<Boolean> terminate = () -> isLocalHost()
        ? localCtrl.getBhRuntimeCtrl().terminate()
        : remoteCtrl.getBhRuntimeCtrl().terminate(
              hostNameTextField.getText(),
              unameTextField.getText(),
              passwordTextField.getText());
    CompletableFuture
        .supplyAsync(terminate)
        .thenAccept(success -> terminating.set(false));
  }

  /** 切断ボタン押下時の処理. */
  private void disconnect() {
    if (disconnecting.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.disconnection.get());
      return;
    }
    disconnecting.set(true);
    Supplier<Boolean> discnonect = () -> isLocalHost()
        ? localCtrl.getBhRuntimeCtrl().disconnect()
        : remoteCtrl.getBhRuntimeCtrl().disconnect();
    CompletableFuture
        .supplyAsync(discnonect)
        .thenAccept(success -> disconnecting.set(false));
  }

  /** 接続ボタン押下時の処理. */
  private void connect() {
    if (connecting.get()) {
      msgService.info(TextDefs.BhRuntime.AlreadyDoing.connection.get());
      return;
    }
    connecting.set(true);
    Supplier<Boolean> connect = () -> isLocalHost()
        ? localCtrl.getBhRuntimeCtrl().connect()
        : remoteCtrl.getBhRuntimeCtrl().connect(
              hostNameTextField.getText(),
              unameTextField.getText(),
              passwordTextField.getText());
    CompletableFuture
        .supplyAsync(connect)
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
    BhRuntimeStatus status = isLocalHost()
        ? localCtrl.getBhRuntimeCtrl().send(cmd) : remoteCtrl.getBhRuntimeCtrl().send(cmd);
    switch (status) {
      case SEND_QUEUE_FULL -> 
          msgService.error(TextDefs.BhRuntime.Communication.failedToPushText.get());
        
      case SEND_WHEN_DISCONNECTED ->
          msgService.error(TextDefs.BhRuntime.Communication.failedToSendTextForNoConnection.get());

      case SUCCESS ->
          msgService.info(TextDefs.BhRuntime.Communication.hasSentText.get());

      case BUSY ->
          msgService.info(TextDefs.BhRuntime.Communication.otherOpsAreInProgress.get());

      default ->
        throw new AssertionError("Invalid status code" + status);
    }
  }

  /** BhRuntime (ローカル or リモート) を切り替えた時の処理. */
  private void switchBhRuntime(boolean isRemote) {
    if (isRemote) {
      hostNameTextField.setDisable(false);
      unameTextField.setDisable(false);
      passwordTextField.setDisable(false);
      bhRuntimeSelBtn.setText(TextDefs.MenuView.remote.get());
      BhSettings.BhRuntime.currentBhRuntimeType = BhRuntimeType.REMOTE;
    } else {
      hostNameTextField.setDisable(true);
      unameTextField.setDisable(true);
      passwordTextField.setDisable(true);
      bhRuntimeSelBtn.setText(TextDefs.MenuView.local.get());
      BhSettings.BhRuntime.currentBhRuntimeType = BhRuntimeType.LOCAL;
    }
  }

  /** ペーストボタンの有効/無効を切り替える. */
  private void changePasteButtonState() {
    boolean disable = copyAndPaste.getList().isEmpty() && cutAndPaste.getList().isEmpty();
    pasteBtn.setDisable(disable);
  }

  /**
   * ジャンプ先のノードを探す.
   *
   * @param wss このワークスペースセットの中からジャンプ先ノードを探す
   * @return ジャンプ先ノード
   */
  private Optional<BhNode> findNodeToJumpTo(WorkspaceSet wss) {
    Workspace currentWs = wss.getCurrentWorkspace();
    if (currentWs == null || currentWs.getSelectedNodes().isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(currentWs.getSelectedNodes().getFirst().getOriginal());
  }

  /** 現在制御対象になっている BhRuntime の種類を返す. */
  private boolean isLocalHost() {
    return BhSettings.BhRuntime.currentBhRuntimeType == BhRuntimeType.LOCAL;
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
