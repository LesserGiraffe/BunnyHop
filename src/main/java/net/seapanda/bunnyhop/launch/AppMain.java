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

package net.seapanda.bunnyhop.launch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import java.awt.SplashScreen;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeService;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.simulator.BhSimulator;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * メインクラス.
 *
 * @author K.Koike
 */
public class AppMain extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  /** BhSimulator 実行用. */
  private final ExecutorService simExecutor = Executors.newSingleThreadExecutor();
  /** BhSimulator 終了待ち用. */
  private Future<?> simFuture;

  @Override
  public void start(Stage stage) throws Exception {
    var simulator = new BhSimulator();
    simFuture = simExecutor.submit(() -> startSimulator(simulator));

    WorkspaceSet wss = new WorkspaceSet();
    if (!BhService.initialize(wss)) {
      System.exit(-1);
    }
    boolean success = simulator.waitForInitialization(BhSettings.BhSimulator.initTimeout);
    success &= simulator.getCmdProcessor()
      .map(cmdProcesspr -> BhRuntimeService.init(cmdProcesspr))
      .orElse(false);
    success &= BhNodeViewStyle.genViewStyleTemplate();
    success &= BhNodeViewStyle.checkNodeIdAndNodeTemplate();        
    if (!success) {
      System.exit(-1);
    }
    SceneBuilder sceneBuilder = new SceneBuilder(wss);
    sceneBuilder.createWindow(stage);
    BhService.msgPrinter().setMainMsgArea(sceneBuilder.wssCtrl.getMsgArea());
    BhService.msgPrinter().setWindowStyle(sceneBuilder.scene.getStylesheets());
    BhService.setTrashboxCtrl(sceneBuilder.trashboxCtrl);
    setOnCloseHandler(
        stage,
        () -> wss.isDirty(),
        () -> sceneBuilder.menuBarCtrl.save(wss));
    simulator.setOnKeyPressed(keyCode -> onKeyPressed(
        keyCode,
        () -> sceneBuilder.foundationCtrl.isBhRuntimeLocal()));
    if (SplashScreen.getSplashScreen() != null) {
      SplashScreen.getSplashScreen().close();
    }
  }

  /** シミュレータがフォーカスを持っているときにキーが押されたときの処理. */
  private void onKeyPressed(int keyCode, Supplier<Boolean> fnCheckIfBhRuntimeIsLocal) {
    var eventName = KeyCodeConverter.toBhProgramEventName(keyCode).orElse(null);
    var bhEvent = new BhProgramEvent(eventName, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    if (fnCheckIfBhRuntimeIsLocal.get()) {
      BhRuntimeService.local().sendAsync(bhEvent);
    } else {
      BhRuntimeService.remote().sendAsync(bhEvent);
    }
  }

  /** 終了処理を登録する. */
  private void setOnCloseHandler(
      Stage stage, Supplier<Boolean> fnCheckIfProjectIsDirty, Supplier<Boolean> fnSave) {
    MutableBoolean killRemoteProcess = new MutableBoolean(true);
    stage.setOnCloseRequest(
        event -> onCloseRequest(event, killRemoteProcess, fnCheckIfProjectIsDirty, fnSave));
    stage.showingProperty().addListener((observable, oldValue, newValue) ->
        terminate(killRemoteProcess, oldValue, newValue));
  }

  /** BunnyHop を閉じる前の処理. */
  private void onCloseRequest(
      WindowEvent event,
      MutableBoolean teminate,
      Supplier<Boolean> fnCheckIfProjectIsDirty,
      Supplier<Boolean> fnSave) {
    teminate.setTrue();
    switch (BhRuntimeService.remote().askIfStopProgram()) {
      case NO -> teminate.setFalse();
      case CANCEL -> event.consume();
      default -> { }
    }
    if (fnCheckIfProjectIsDirty.get()) {
      return;
    }
    if (!askIfSaveProject(fnSave)) {
      event.consume();
    }
  }

  /**
   * アプリ終了の確認を行う.
   *
   * @return アプリの終了を許可する場合 true を返す.
   */
  public boolean askIfSaveProject(Supplier<Boolean> fnSave) {
    Optional<ButtonType> buttonType = BhService.msgPrinter().alert(
        Alert.AlertType.CONFIRMATION,
        TextDefs.Export.AskIfSave.title.get(),
        null,
        TextDefs.Export.AskIfSave.body.get(),
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

    return buttonType
      .map(btnType -> {
        if (btnType.equals(ButtonType.YES)) {
          return fnSave.get();
        }
        return btnType.equals(ButtonType.NO);
      })
      .orElse(false);
  }

  /** BunnyHop の終了処理. */
  private void terminate(MutableBoolean killRemoteRuntime, Boolean oldVal, Boolean newVal) {
    if (oldVal == true && newVal == false) {
      BhRuntimeService.local().end();
      BhRuntimeService.remote().end(killRemoteRuntime.getValue());
      BhService.msgPrinter().close();
      Gdx.app.exit();
      try {
        simFuture.get();
      } catch (InterruptedException | ExecutionException e) { /* do nothing. */ }
      System.exit(0);
    }
  }

  private void startSimulator(BhSimulator simulator) {
    Lwjgl3WindowListener windowListener = new Lwjgl3WindowAdapter() {
      @Override
      public boolean closeRequested() {
        return false;
      }
    };
    var config = new Lwjgl3ApplicationConfiguration();
    config.setWindowListener(windowListener);
    config.setWindowedMode(800, 600);
    new Lwjgl3Application(simulator, config);
  }
}
