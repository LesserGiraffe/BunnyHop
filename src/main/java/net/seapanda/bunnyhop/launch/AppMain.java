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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.BhProgramControllerImpl;
import net.seapanda.bunnyhop.bhprogram.LocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageProcessorImpl;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.ExclusiveSelection;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.common.TextFetcher;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.BhCompilerImpl;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeSelectionViewProxyImpl;
import net.seapanda.bunnyhop.export.JsonProjectExporter;
import net.seapanda.bunnyhop.export.JsonProjectImporter;
import net.seapanda.bunnyhop.model.factory.BhNodeFactoryImpl;
import net.seapanda.bunnyhop.model.factory.BhNodeRepository;
import net.seapanda.bunnyhop.model.factory.ModelGenerator;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactoryImpl;
import net.seapanda.bunnyhop.model.factory.XmlBhNodeRepository;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeReplacerWithCache;
import net.seapanda.bunnyhop.model.node.event.CommonDataSupplier;
import net.seapanda.bunnyhop.model.node.event.ScriptConnectorEventInvokerImpl;
import net.seapanda.bunnyhop.model.node.event.ScriptNodeEventInvokerImpl;
import net.seapanda.bunnyhop.model.nodeselection.JsonBhNodeCategoryTree;
import net.seapanda.bunnyhop.model.workspace.CopyAndPaste;
import net.seapanda.bunnyhop.model.workspace.CutAndPaste;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhMessageService;
import net.seapanda.bunnyhop.service.BhScriptRepositoryImpl;
import net.seapanda.bunnyhop.service.CompileErrorReporter;
import net.seapanda.bunnyhop.service.DerivativeCache;
import net.seapanda.bunnyhop.service.FileCollector;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.service.ModelAccessMediator;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.simulator.BhSimulator;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.log.FileLogger;
import net.seapanda.bunnyhop.utility.textdb.JsonTextDatabase;
import net.seapanda.bunnyhop.view.factory.BhNodeViewFactoryImpl;
import net.seapanda.bunnyhop.view.factory.PrivateTemplateButtonFactoryImpl;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyleFactory;
import net.seapanda.bunnyhop.view.node.style.JsonBhNodeViewStyleFactory;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeShowcaseBuilderImpl;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

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
    var msgService = new BhMessageService();
    Path logDir = Paths.get(Utility.execPath, BhConstants.Path.LOG_DIR, BhConstants.APP_NAME);
    var logger = new FileLogger(
        logDir,
        BhConstants.Path.LOG_FILE_NAME,
        BhConstants.Message.LOG_FILE_SIZE_LIMIT,
        BhConstants.Message.MAX_LOG_FILE_NUM);
    try {
      Path textDbFile = Paths.get(
          Utility.execPath,
          BhConstants.Path.LANGUAGE_DIR,
          BhSettings.language,
          BhConstants.Path.LANGUAGE_FILE);
      final var textDb = new JsonTextDatabase(textDbFile);
      TextDefs.setTextDatabase(textDb);
      TextFetcher.setTextDatabase(textDb);
      LogManager.initialize(logger);

      final Path fxmlDir =
          Paths.get(Utility.execPath, BhConstants.Path.VIEW_DIR, BhConstants.Path.FXML_DIR);
      var fxmlCollector = new FileCollector(fxmlDir, "fxml");
      final Path guiDefFile = fxmlCollector.getFilePath(BhConstants.Path.FOUNDATION_FXML);
      final Path wsViewFile = fxmlCollector.getFilePath(BhConstants.Path.WORKSPACE_FXML);
      final Path btnFile = fxmlCollector.getFilePath(BhConstants.Path.PRIVATE_TEMPLATE_BUTTON_FXML);
      final Path nodeShifterViewFile =
          fxmlCollector.getFilePath(BhConstants.Path.NODE_SHIFTER_FXML);
      final Path nodeSelectionViewFile =
          fxmlCollector.getFilePath(BhConstants.Path.NODE_SELECTION_PANEL_FXML);
      final Path[] scriptDirs = new Path[] {
        Paths.get(Utility.execPath, BhConstants.Path.BH_DEF_DIR, BhConstants.Path.FUNCTIONS_DIR),
        Paths.get(
            Utility.execPath, BhConstants.Path.BH_DEF_DIR, BhConstants.Path.TEMPLATE_LIST_DIR),
        Paths.get(Utility.execPath, BhConstants.Path.REMOTE_DIR)};
      final Path viewStyleDir = Paths.get(
          Utility.execPath,
          BhConstants.Path.VIEW_DIR,
          BhConstants.Path.NODE_STYLE_DEF_DIR,
          BhSettings.language);
      final Path nodeTemplateListFile = Paths.get(
          Utility.execPath,
          BhConstants.Path.BH_DEF_DIR,
          BhConstants.Path.TEMPLATE_LIST_DIR,
          BhConstants.Path.NODE_TEMPLATE_LIST_JSON);
      final Path nodeDir = Paths.get(
          Utility.execPath,
          BhConstants.Path.BH_DEF_DIR,
          BhConstants.Path.NODE_DEF_DIR);
      final Path cnctrDir = Paths.get(
          Utility.execPath,
          BhConstants.Path.BH_DEF_DIR,
          BhConstants.Path.CONNECTOR_DEF_DIR);

      final var sceneBuilder = new SceneBuilder(guiDefFile);
      msgService.setMainMsgArea(sceneBuilder.wssCtrl.getMsgArea());
      msgService.setWindowStyle(sceneBuilder.scene.getStylesheets());

      final var simulator = createSimulator();
      final var nodeSelViewProxy =
          new BhNodeSelectionViewProxyImpl(sceneBuilder.wssCtrl::addNodeSelectionView);
      final var derivativeCache = new DerivativeCache();
      final var wss = new WorkspaceSet();
      final var undoRedoAgent = new UndoRedoAgent(wss);
      final var mediator = new ModelAccessMediator(
          new ModelExclusiveControl(),
          derivativeCache,
          new CompileErrorReporter(wss),
          undoRedoAgent);
      final var scriptRepository = new BhScriptRepositoryImpl(scriptDirs);
      final var viewStyleFactory = new JsonBhNodeViewStyleFactory(viewStyleDir);
      final var buttonFactory = new PrivateTemplateButtonFactoryImpl(
          btnFile, viewStyleFactory, mediator, nodeSelViewProxy);
      final var nodeViewFactory = new BhNodeViewFactoryImpl(viewStyleFactory, buttonFactory);
      final var nodeRepository = new XmlBhNodeRepository(scriptRepository);
      final var nodeFactory = new BhNodeFactoryImpl(
          nodeRepository,
          nodeViewFactory,
          mediator,
          sceneBuilder.trashboxCtrl,
          wss,
          nodeSelViewProxy);
      final var commonDataSupplier = new CommonDataSupplier(scriptRepository, nodeFactory, textDb);
      final var modelGenerator = new ModelGenerator(
          nodeFactory,
          new DerivativeReplacerWithCache(derivativeCache),
          new ScriptNodeEventInvokerImpl(scriptRepository, commonDataSupplier, nodeFactory, textDb),
          new ScriptConnectorEventInvokerImpl(scriptRepository, commonDataSupplier, textDb));
      nodeRepository.collect(nodeDir, cnctrDir, modelGenerator, textDb);
      final var categoryTree =
          new JsonBhNodeCategoryTree(nodeTemplateListFile, nodeFactory, textDb);
      final var nodeShowcaseBuilder = new BhNodeShowcaseBuilderImpl(
          nodeFactory, nodeSelViewProxy, nodeSelectionViewFile);
      final var wsFactory = new WorkspaceFactoryImpl(
          wsViewFile, nodeShifterViewFile, mediator, nodeSelViewProxy, msgService);
      final var localCompiler = genCompiler(true);
      final var remoteCompiler = genCompiler(false);
      final var msgProcessor = new BhProgramMessageProcessorImpl(msgService);
      final var localRuntimeCtrl = new LocalBhRuntimeController(
          msgProcessor, simulator.getCmdProcessor().get(), msgService);
      final var remoteRuntimeCtrl = new RemoteBhRuntimeController(
          msgProcessor, simulator.getCmdProcessor().get(), msgService, scriptRepository);
      final var localBhProgramCtrl =
          new BhProgramControllerImpl(localCompiler, localRuntimeCtrl, msgService);
      final var remoteBhProgramCtrl =
          new BhProgramControllerImpl(remoteCompiler, remoteRuntimeCtrl, msgService);
      final var pastePosOffsetCount = new MutableInt(-2);
      final var copyAndPaste = new CopyAndPaste(nodeFactory, pastePosOffsetCount);
      final var cutAndPaste = new CutAndPaste(pastePosOffsetCount);
      final var projImporter = new JsonProjectImporter(nodeFactory, wsFactory, msgService);
      final var projExporter = new JsonProjectExporter(msgService);
      if (!checkNodeIdAndNodeTemplate(nodeRepository, viewStyleFactory)) {
        return;
      }
      setOnCloseHandler(
          stage,
          localRuntimeCtrl,
          remoteRuntimeCtrl,
          msgService,
          () -> remoteRuntimeCtrl.isProgramRunning(),
          () -> wss.isDirty(),
          () -> sceneBuilder.menuBarCtrl.save(wss));
      simulator.setOnKeyPressed(keyCode -> onKeyPressed(
          keyCode,
          localRuntimeCtrl,
          remoteRuntimeCtrl,
          () -> sceneBuilder.foundationCtrl.isBhRuntimeLocal()));     
      sceneBuilder.initialze(
          wss,
          categoryTree,
          nodeShowcaseBuilder,
          mediator,
          wsFactory,
          undoRedoAgent,
          nodeSelViewProxy,
          localBhProgramCtrl,
          remoteBhProgramCtrl,
          projImporter,
          projExporter,
          copyAndPaste,
          cutAndPaste,
          msgService);
      sceneBuilder.createWindow(stage, wsFactory);
      if (SplashScreen.getSplashScreen() != null) {
        SplashScreen.getSplashScreen().close();
      }
      undoRedoAgent.deleteCommands();
    } catch (Throwable e) {
      logger.error("Application Start Error.\n%s".formatted(e.toString()));
      msgService.alert(
          AlertType.ERROR,
          "Application Start Error",
          null,
          e.toString(),
          ButtonType.OK);
      System.exit(-1);
    }
  }

  private BhSimulator createSimulator() throws AppInitializationException {
    var simulator = new BhSimulator();
    simFuture = simExecutor.submit(() -> startSimulator(simulator));
    if (!simulator.waitForInitialization(BhSettings.BhSimulator.initTimeout)
        || simulator.getCmdProcessor().isEmpty()) {
      throw new AppInitializationException("Failed to initialize the simulator");  
    }
    return simulator;
  }

  /** シミュレータがフォーカスを持っているときにキーが押されたときの処理. */
  private void onKeyPressed(
      int keyCode,
      LocalBhRuntimeController localCtrl,
      RemoteBhRuntimeController remoteCtrl,
      Supplier<Boolean> fnCheckIfBhRuntimeIsLocal) {
    var eventName = KeyCodeConverter.toBhProgramEventName(keyCode).orElse(null);
    var bhEvent = new BhProgramEvent(eventName, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    if (fnCheckIfBhRuntimeIsLocal.get()) {
      localCtrl.send(bhEvent);
    } else {
      remoteCtrl.send(bhEvent);
    }
  }

  /** 終了処理を登録する. */
  private void setOnCloseHandler(
      Stage stage,
      LocalBhRuntimeController localCtrl,
      RemoteBhRuntimeController remoteCtrl,
      BhMessageService msgService,
      Supplier<Boolean> fnIsProgramRunning,
      Supplier<Boolean> fnCheckIfProjectIsDirty,
      Supplier<Boolean> fnSave) {
    MutableBoolean killRemoteProcess = new MutableBoolean(true);
    stage.setOnCloseRequest(event -> onCloseRequest(
        event,
        killRemoteProcess,
        msgService,
        fnIsProgramRunning,
        fnCheckIfProjectIsDirty,
        fnSave));
    stage.showingProperty().addListener((observable, oldValue, newValue) -> terminate(
        localCtrl,
        remoteCtrl,
        msgService,
        killRemoteProcess.getValue(),
        oldValue,
        newValue));
  }

  /** BunnyHop を閉じる前の処理. */
  private void onCloseRequest(
      WindowEvent event,
      MutableBoolean teminate,
      MessageService msgService, 
      Supplier<Boolean> fnIsProgramRunning,
      Supplier<Boolean> fnCheckIfProjectIsDirty,
      Supplier<Boolean> fnSave) {
    teminate.setTrue();
    switch (askIfStopProgram(msgService, fnIsProgramRunning)) {
      case NO -> teminate.setFalse();
      case CANCEL -> event.consume();
      default -> { }
    }
    if (!fnCheckIfProjectIsDirty.get()) {
      return;
    }
    if (!askIfSaveProject(msgService, fnSave)) {
      event.consume();
    }
  }

  /**
   * アプリ終了の確認を行う.
   *
   * @return アプリの終了を許可する場合 true を返す.
   */
  public boolean askIfSaveProject(MessageService messageService, Supplier<Boolean> fnSave) {
    Optional<ButtonType> buttonType = messageService.alert(
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
  private void terminate(
      LocalBhRuntimeController localCtrl,
      RemoteBhRuntimeController remoteCtrl,
      BhMessageService msgService,
      boolean killRemoteRuntime,
      Boolean oldVal,
      Boolean newVal) {
    if (oldVal == true && newVal == false) {
      localCtrl.end();
      remoteCtrl.end(killRemoteRuntime);
      msgService.close();
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

  /** {@link BhCompiler} オブジェクトを作成する. */
  private BhCompiler genCompiler(boolean isLocal) throws IOException {
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
      return new BhCompilerImpl(commonLibPath, localOrRemoteLibPath);
    } catch (IOException e) {
      LogManager.logger().error("Failed to initialize Compiler.\n%s".formatted(e));
      throw e;
    }
  }

  private boolean checkNodeIdAndNodeTemplate(
      BhNodeRepository repository, BhNodeViewStyleFactory factory) {
    boolean allExist = true;
    for (BhNode node : repository.getAll()) {
      if (!factory.canCreateStyleOf(node.getStyleId())) {
        allExist = false;
        LogManager.logger().error(
            "A node style (%s) is not found among *.json files.".formatted(node.getStyleId()));
      }
    }
    return allExist;
  }

  /**
   * 現在実行中のプログラムを止めるかどうかを訪ねる.
   * プログラムを実行中でない場合は何も尋ねない.
   *
   * @retval YES プログラムを止める
   * @retval NO プログラムを止めない
   * @retval CANCEL キャンセルを選択
   * @retval NONE_OF_THEM 何も尋ねなかった場合
   */
  public ExclusiveSelection askIfStopProgram(
      MessageService msgService, Supplier<Boolean> fnIsProgramRunning) {
    if (fnIsProgramRunning.get()) {
      Optional<ButtonType> selected = msgService.alert(
          AlertType.CONFIRMATION,
          TextDefs.BhRuntime.Remote.AskIfStop.title.get(),
          null,
          TextDefs.BhRuntime.Remote.AskIfStop.body.get(),
          ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

      return selected.map((btnType) -> {
        if (btnType.equals(ButtonType.YES)) {
          return ExclusiveSelection.YES;
        } else if (btnType.equals(ButtonType.NO)) {
          return ExclusiveSelection.NO;
        } else {
          return ExclusiveSelection.CANCEL;
        }
      }).orElse(ExclusiveSelection.YES);
    }
    return ExclusiveSelection.NONE_OF_THEM;
  }  
}
