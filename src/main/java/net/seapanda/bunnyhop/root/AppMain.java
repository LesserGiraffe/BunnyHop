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

package net.seapanda.bunnyhop.root;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import java.awt.SplashScreen;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.BhProgramService;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.common.constant.BhConstants.Path;
import net.seapanda.bunnyhop.common.constant.BhSettings;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.FxmlCollector;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.service.MsgPrinter;
import net.seapanda.bunnyhop.service.Util;
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
    
    setOnCloseHandler(stage);
    if (!MsgPrinter.INSTANCE.init()) {
      System.exit(-1);
    }
    if (!FxmlCollector.INSTANCE.collectFxmlFiles()) {
      System.exit(-1);
    }
    boolean success = BhScriptManager.INSTANCE.genCompiledCode(
        Paths.get(Util.INSTANCE.execPath, Path.BH_DEF_DIR, Path.FUNCTIONS_DIR),
        Paths.get(Util.INSTANCE.execPath, Path.BH_DEF_DIR, Path.TEMPLATE_LIST_DIR),
        Paths.get(Util.INSTANCE.execPath, Path.REMOTE_DIR));

    if (!success) {
      System.exit(-1);
    }
    if (!BhCompiler.INSTANCE.init()) {
      System.exit(-1);
    }
    success = BhNodeViewStyle.genViewStyleTemplate();
    success &= BhNodeFactory.INSTANCE.initialize();
    success &= BhNodeViewStyle.checkNodeIdAndNodeTemplate();
    success &= simulator.waitForInitialization(BhSettings.BhSimulator.initTimeout);
    success &= simulator.getCmdProcessor()
        .map(cmdProcesspr -> BhProgramService.init(cmdProcesspr)).orElse(false);
    if (!success) {
      System.exit(-1);
    }
    if (!BunnyHop.INSTANCE.createWindow(stage)) {
      System.exit(-1);
    }
    simulator.setOnKeyPressed(keyCode -> onKeyPressed(keyCode));
    Optional.ofNullable(SplashScreen.getSplashScreen()).ifPresent(SplashScreen::close);
  }

  /** シミュレータがフォーカスを持っているときにキーが押されたときの処理. */
  private void onKeyPressed(int keyCode) {
    var eventName = KeyCodeConverter.toBhProgramEventName(keyCode).orElse(null);
    var bhEvent = new BhProgramEvent(eventName, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    if (BunnyHop.INSTANCE.isBhRuntimeLocal()) {
      BhProgramService.local().sendAsync(bhEvent);
    } else {
      BhProgramService.remote().sendAsync(bhEvent);
    }
  }

  /** 終了処理を登録する. */
  private void setOnCloseHandler(Stage stage) {
    MutableBoolean killRemoteProcess = new MutableBoolean(true);
    stage.setOnCloseRequest(event -> onCloseRequest(killRemoteProcess, event));
    stage.showingProperty().addListener((observable, oldValue, newValue) ->
        terminate(killRemoteProcess, oldValue, newValue));
  }

  /** BunnyHop を閉じる前の処理. */
  private void onCloseRequest(MutableBoolean teminate, WindowEvent event) {
    teminate.setTrue();
    switch (BhProgramService.remote().askIfStopProgram()) {
      case NO -> teminate.setFalse();
      case CANCEL -> event.consume();
      default -> { }
    }
    if (!BunnyHop.INSTANCE.processCloseRequest()) {
      event.consume();
    }
  }

  /** BunnyHop の終了処理. */
  private void terminate(MutableBoolean killRemoteRuntime, Boolean oldVal, Boolean newVal) {
    if (oldVal == true && newVal == false) {
      BhProgramService.local().end();
      BhProgramService.remote().end(killRemoteRuntime.getValue());
      MsgPrinter.INSTANCE.end();
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
