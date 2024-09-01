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

import java.awt.SplashScreen;
import java.nio.file.Paths;
import java.util.Optional;
import javafx.application.Application;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramManager;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramManager;
import net.seapanda.bunnyhop.common.constant.BhParams.Path;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.configfilereader.FxmlCollector;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
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

  @Override
  public void start(Stage stage) throws Exception {
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
    success &= BhNodeTemplates.INSTANCE.genTemplate();
    success &= BhNodeViewStyle.checkNodeIdAndNodeTemplate();
    if (!success) {
      System.exit(-1);
    }
    if (!BunnyHop.INSTANCE.createWindow(stage)) {
      System.exit(-1);
    }
    if (!LocalBhProgramManager.INSTANCE.init()) {
      System.exit(-1);
    }
    if (!RemoteBhProgramManager.INSTANCE.init()) {
      System.exit(-1);
    }
    Optional.ofNullable(SplashScreen.getSplashScreen()).ifPresent(SplashScreen::close);
  }

  /** 終了処理を登録する. */
  private void setOnCloseHandler(Stage stage) {
    MutableBoolean teminate = new MutableBoolean(true);

    stage.setOnCloseRequest(event -> {
      teminate.setTrue();
      switch (RemoteBhProgramManager.INSTANCE.askIfStopProgram()) {
        case NO:
          teminate.setFalse();
          break;

        case CANCEL:
          event.consume();
          return;

        default:
          break;
      }
      if (!BunnyHop.INSTANCE.processCloseRequest()) {
        event.consume();
      }
    });

    stage.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (oldValue == true && newValue == false) {
        LocalBhProgramManager.INSTANCE.end();
        RemoteBhProgramManager.INSTANCE.end(teminate.getValue());
        MsgPrinter.INSTANCE.end();
        System.exit(0);
      }
    });
  }
}
