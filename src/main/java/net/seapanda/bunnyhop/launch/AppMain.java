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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Message.LOG_FILE_SIZE_LIMIT;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Message.MAX_LOG_FILE_NUM;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.BH_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.COMPILE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.CONNECTOR_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.EVENT_HANDLERS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.LANGUAGE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.LIBS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.LOG;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.NODE_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.NODE_STYLE_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.REMOTE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.SETTINGS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.TEMPLATE_NODE_LIST;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.VIEW;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_SETTINGS_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BhLibs;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.CALL_STACK_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.DEBUG_WINDOW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.FOUNDATION_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.LANGUAGE_FILE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.LOG_MSG;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.NODE_SELECTION_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.NODE_SHIFTER_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.PRIVATE_TEMPLATE_BUTTON_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.TEMPLATE_NODE_LIST_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.VARIABLE_INSPECTION_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.WORKSPACE_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Ui.DEFAULT_SIM_HEIGHT_RATE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Ui.DEFAULT_SIM_WIDTH_RATE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Ui.MAX_DEFAULT_SIM_HEIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Ui.MAX_DEFAULT_SIM_WIDTH;
import static net.seapanda.bunnyhop.utility.Utility.execPath;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import java.awt.SplashScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncherImpl;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramControllerImpl;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageDispatcher;
import net.seapanda.bunnyhop.bhprogram.message.IoMessageProcessorImpl;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.bhprogram.runtime.RmiLocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.runtime.RmiRemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.common.text.TextFetcher;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.BhCompilerImpl;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.compiler.nodecollector.SourceNodeCollector;
import net.seapanda.bunnyhop.debugger.model.BhDebugger;
import net.seapanda.bunnyhop.debugger.model.DebugMessageProcessorImpl;
import net.seapanda.bunnyhop.debugger.model.breakpoint.BreakpointCache;
import net.seapanda.bunnyhop.debugger.service.EntryPointPresenter;
import net.seapanda.bunnyhop.debugger.service.ThreadContextPresenter;
import net.seapanda.bunnyhop.debugger.view.factory.DebugViewFactoryImpl;
import net.seapanda.bunnyhop.export.JsonProjectExporter;
import net.seapanda.bunnyhop.export.JsonProjectImporter;
import net.seapanda.bunnyhop.linter.model.CompileErrorChecker;
import net.seapanda.bunnyhop.linter.model.CompileErrorNodeCache;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeReplacerWithCache;
import net.seapanda.bunnyhop.node.model.event.CommonDataSupplier;
import net.seapanda.bunnyhop.node.model.event.ScriptConnectorEventInvokerImpl;
import net.seapanda.bunnyhop.node.model.event.ScriptNodeEventInvokerImpl;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactoryImpl;
import net.seapanda.bunnyhop.node.model.factory.BhNodeRepository;
import net.seapanda.bunnyhop.node.model.factory.ModelGenerator;
import net.seapanda.bunnyhop.node.model.factory.XmlBhNodeRepository;
import net.seapanda.bunnyhop.node.model.service.DerivativeCache;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactoryImpl;
import net.seapanda.bunnyhop.node.view.factory.PrivateTemplateButtonFactoryImpl;
import net.seapanda.bunnyhop.node.view.service.BhNodeViewSupervisor;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleFactory;
import net.seapanda.bunnyhop.node.view.style.JsonBhNodeViewStyleFactory;
import net.seapanda.bunnyhop.nodeselection.control.BhNodeSelectionViewProxyImpl;
import net.seapanda.bunnyhop.nodeselection.model.JsonBhNodeCategoryTree;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeCategoryBuilder;
import net.seapanda.bunnyhop.service.FileCollector;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationServiceImpl;
import net.seapanda.bunnyhop.service.message.BhMessageService;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.script.BhScriptRepositoryImpl;
import net.seapanda.bunnyhop.service.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.simulator.BhSimulator;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.ui.control.SearchBoxController;
import net.seapanda.bunnyhop.ui.model.ExclusiveSelection;
import net.seapanda.bunnyhop.ui.service.window.BhWindowManager;
import net.seapanda.bunnyhop.ui.service.window.WindowManager;
import net.seapanda.bunnyhop.utility.log.FileLogger;
import net.seapanda.bunnyhop.utility.serialization.JsonExporter;
import net.seapanda.bunnyhop.utility.serialization.JsonImporter;
import net.seapanda.bunnyhop.utility.textdb.JsonTextDatabase;
import net.seapanda.bunnyhop.workspace.control.TrashCanController;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSetController;
import net.seapanda.bunnyhop.workspace.model.CopyAndPaste;
import net.seapanda.bunnyhop.workspace.model.CutAndPaste;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.model.factory.WorkspaceFactoryImpl;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * BunnyHop アプリケーションのエントリポイントとなるクラス.
 * JavaFX アプリケーションの起動, 初期化, 終了処理を管理する.
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
    Path logDir = Paths.get(execPath, LOG, BhConstants.APP_NAME);
    var logger = new FileLogger(logDir, LOG_MSG, LOG_FILE_SIZE_LIMIT, MAX_LOG_FILE_NUM);
    try {
      importSettings();
      Path textDbFile = Paths.get(execPath, LANGUAGE, BhSettings.language, LANGUAGE_FILE);
      final var textDb = new JsonTextDatabase(textDbFile);
      TextDefs.setTextDatabase(textDb);
      TextFetcher.setTextDatabase(textDb);
      LogManager.initialize(logger);

      final Path fxmlDir = Paths.get(execPath, VIEW, FXML);
      var fxmlCollector = new FileCollector(fxmlDir, "fxml");
      final Path guiDefFile = fxmlCollector.getFilePath(FOUNDATION_FXML);
      final Path debugDefFile = fxmlCollector.getFilePath(DEBUG_WINDOW_FXML);
      final Path wsViewFile = fxmlCollector.getFilePath(WORKSPACE_FXML);
      final Path btnFile = fxmlCollector.getFilePath(PRIVATE_TEMPLATE_BUTTON_FXML);
      final Path nodeShifterViewFile = fxmlCollector.getFilePath(NODE_SHIFTER_FXML);
      final Path nodeSelectionViewFile = fxmlCollector.getFilePath(NODE_SELECTION_VIEW_FXML);
      final Path callStackViewFile = fxmlCollector.getFilePath(CALL_STACK_VIEW_FXML);
      final Path varInspectionViewFile = fxmlCollector.getFilePath(VARIABLE_INSPECTION_VIEW_FXML);
      final Path[] scriptDirs = new Path[] {
        Paths.get(execPath, BH_DEF, EVENT_HANDLERS),
        Paths.get(execPath, BH_DEF, TEMPLATE_NODE_LIST),
        Paths.get(execPath, REMOTE)};
      final Path viewStyleDir = Paths.get(execPath, VIEW, NODE_STYLE_DEF, BhSettings.language);
      final Path nodeSelectionFile =
          Paths.get(execPath, BH_DEF, TEMPLATE_NODE_LIST, TEMPLATE_NODE_LIST_JSON);
      final Path nodeDir = Paths.get(execPath, BH_DEF, NODE_DEF);
      final Path cnctrDir = Paths.get(execPath, BH_DEF, CONNECTOR_DEF);

      final var simulator = createSimulator();
      final var debugStage = new Stage();
      final var windowManager = new BhWindowManager(
          stage, debugStage, ((Lwjgl3Graphics) Gdx.app.getGraphics()).getWindow());
      setOnSimulatorCmdProcessing(simulator, windowManager);
      final SimulatorCmdProcessor simCmdProcessor = simulator.getCmdProcessor().orElseThrow(
          () -> new AppInitializationException("Simulator Command Processor not found."));

      final var wss = new WorkspaceSet();
      final var compileErrorNodeCache = new CompileErrorNodeCache(wss);
      final var executableNodeCollector =
          new SourceNodeCollector(wss, compileErrorNodeCache, msgService);
      final var compileErrChecker = new CompileErrorChecker(wss);
      final var undoRedoAgent = new UndoRedoAgent(wss);
      final var derivativeCache = new DerivativeCache();
      final var nodeViewSuperVisor = new BhNodeViewSupervisor(wss);
      final var mediator =
          new TransactionNotificationServiceImpl(derivativeCache, compileErrChecker, undoRedoAgent);
      final var wssCtrl = new WorkspaceSetController(wss, mediator);
      final var nodeSelViewProxy =
          new BhNodeSelectionViewProxyImpl(nodeSelectionViewFile, wssCtrl::addNodeSelectionView);
      final var scriptRepository = new BhScriptRepositoryImpl(scriptDirs);
      final var viewStyleFactory = new JsonBhNodeViewStyleFactory(viewStyleDir);
      final var buttonFactory = new PrivateTemplateButtonFactoryImpl(
          btnFile, viewStyleFactory, mediator, nodeSelViewProxy);
      final var nodeViewFactory = new BhNodeViewFactoryImpl(viewStyleFactory, buttonFactory);
      final var nodeRepository = new XmlBhNodeRepository(scriptRepository);
      final var trashCanCtrl = new TrashCanController();
      final var nodeFactory = new BhNodeFactoryImpl(
          nodeRepository,
          nodeViewFactory,
          mediator,
          trashCanCtrl,
          wss,
          nodeSelViewProxy,
          nodeViewSuperVisor);
      final var commonDataSupplier =
          new CommonDataSupplier(scriptRepository, nodeFactory, textDb);
      final var modelGenerator = new ModelGenerator(
          nodeFactory,
          new DerivativeReplacerWithCache(derivativeCache),
          new ScriptNodeEventInvokerImpl(
              scriptRepository, commonDataSupplier, nodeFactory, textDb),
          new ScriptConnectorEventInvokerImpl(scriptRepository, commonDataSupplier));
      nodeRepository.collect(nodeDir, cnctrDir, modelGenerator, textDb);
      final var categoryTree =
          new JsonBhNodeCategoryTree(nodeSelectionFile, nodeFactory, textDb);
      final var nodeCategoryBuilder = new BhNodeCategoryBuilder(categoryTree.getRoot());
      final var wsFactory = new WorkspaceFactoryImpl(
          wsViewFile,
          nodeShifterViewFile,
          mediator,
          nodeSelViewProxy,
          msgService,
          nodeViewSuperVisor);
      final var localCompiler = genCompiler(true);
      final var remoteCompiler = genCompiler(false);

      final var localRuntimeCtrl = new RmiLocalBhRuntimeController(simCmdProcessor, msgService);
      final var localBhProgramCtrl =
          new LocalBhProgramLauncherImpl(localCompiler, localRuntimeCtrl, msgService);
      final var remoteRuntimeCtrl =
          new RmiRemoteBhRuntimeController(msgService, scriptRepository);
      final var remoteBhProgramCtrl =
          new RemoteBhProgramControllerImpl(remoteCompiler, remoteRuntimeCtrl, msgService);
      final var searchBoxCtrl = new SearchBoxController();
      final var breakpointCache = new BreakpointCache(wss);
      final var debugger = new BhDebugger(localRuntimeCtrl, remoteRuntimeCtrl, breakpointCache);
      final var debugViewFactory = new DebugViewFactoryImpl(
          callStackViewFile,
          varInspectionViewFile,
          searchBoxCtrl,
          debugger,
          wss,
          nodeViewSuperVisor);
      new ThreadContextPresenter(debugger, msgService, nodeViewSuperVisor);
      final var entryPointPresenter = new EntryPointPresenter(
          wss, localRuntimeCtrl, remoteRuntimeCtrl, nodeViewSuperVisor);
      final var mainRoutineIds =
          List.of(localCompiler.mainRoutineId(), remoteCompiler.mainRoutineId());
      final var debugMsgProcessor =
          new DebugMessageProcessorImpl(wss, debugger, entryPointPresenter, mainRoutineIds);
      final var msgProcessor = new IoMessageProcessorImpl(msgService);
      new BhProgramMessageDispatcher(
          msgProcessor, debugMsgProcessor, simCmdProcessor, localRuntimeCtrl, mediator);
      new BhProgramMessageDispatcher(
          msgProcessor, debugMsgProcessor, simCmdProcessor, remoteRuntimeCtrl, mediator);
      final var pastePosOffsetCount = new MutableInt(-2);
      final var copyAndPaste = new CopyAndPaste(nodeFactory, pastePosOffsetCount);
      final var cutAndPaste = new CutAndPaste(pastePosOffsetCount);
      final var projImporter = new JsonProjectImporter(nodeFactory, wsFactory, msgService);
      final var projExporter = new JsonProjectExporter(msgService);
      if (!validateNodeViewStyles(nodeRepository, viewStyleFactory)) {
        return;
      }
      final var sceneBuilder = new SceneBuilder(
          guiDefFile,
          debugDefFile,
          wss,
          nodeCategoryBuilder.getCategoryRoot(),
          mediator,
          nodeFactory,
          wsFactory,
          debugViewFactory,
          undoRedoAgent,
          nodeSelViewProxy,
          localBhProgramCtrl,
          remoteBhProgramCtrl,
          projImporter,
          projExporter,
          copyAndPaste,
          cutAndPaste,
          msgService,
          breakpointCache,
          compileErrorNodeCache,
          debugger,
          executableNodeCollector,
          wssCtrl,
          searchBoxCtrl,
          trashCanCtrl,
          windowManager,
          nodeViewSuperVisor);

      setOnCloseHandler(
          stage,
          localRuntimeCtrl,
          remoteRuntimeCtrl,
          msgService,
          windowManager,
          remoteRuntimeCtrl::isProgramRunning,
          wss::isDirty,
          () -> sceneBuilder.menuBarCtrl.save(wss));
      simulator.setOnKeyPressed(
          keyCode -> onKeyPressed(keyCode, localRuntimeCtrl, remoteRuntimeCtrl));
      msgService.setWindowStyle(sceneBuilder.scene.getStylesheets());
      msgService.setMainMsgArea(sceneBuilder.msgViewCtrl.getMsgArea());
      sceneBuilder.createWindows(stage, debugStage, wsFactory);
      undoRedoAgent.deleteCommands();

      if (SplashScreen.getSplashScreen() != null) {
        SplashScreen.getSplashScreen().close();
      }
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
    BhSimulator simulator = newSimulator();
    simFuture = simExecutor.submit(() -> startSimulator(simulator));
    if (!simulator.waitForInitialization(BhSettings.BhSimulator.initTimeout)
        || simulator.getCmdProcessor().isEmpty()) {
      throw new AppInitializationException("Failed to initialize the simulator.");
    }
    return simulator;
  }

  private BhSimulator newSimulator() throws AppInitializationException {
    try {
      return new BhSimulator();
    } catch (Exception e) {
      throw new AppInitializationException("Failed to instantiate a simulator object.", e);
    }
  }

  /** シミュレータコマンドを処理する前のイベントハンドラを登録する. */
  private static void setOnSimulatorCmdProcessing(
      BhSimulator simulator, BhWindowManager windowManager) {
    simulator.getCmdProcessor().get().getCallbackRegistry().getOnCmdProcessing()
        .add((SimulatorCmdProcessor.CmdProcessingEvent event) -> {
          if (BhSettings.BhSimulator.focusOnSimulatorChanged) {
            windowManager.focusSimulator();
          }
        });
  }

  /** シミュレータがフォーカスを持っているときにキーが押されたときの処理. */
  private void onKeyPressed(
      int keyCode,
      RmiLocalBhRuntimeController localCtrl,
      RmiRemoteBhRuntimeController remoteCtrl) {
    KeyCodeConverter.toBhProgramEventName(keyCode)
        .map(name -> new BhProgramEvent(name, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES))
        .ifPresent(bhEvent -> {
          if (BhSettings.BhRuntime.currentBhRuntimeType == BhRuntimeType.LOCAL) {
            localCtrl.send(bhEvent);
          } else {
            remoteCtrl.send(bhEvent);
          }
        });
  }

  /** 終了処理を登録する. */
  private void setOnCloseHandler(
      Stage stage,
      RmiLocalBhRuntimeController localCtrl,
      RmiRemoteBhRuntimeController remoteCtrl,
      BhMessageService msgService,
      WindowManager windowManager,
      Supplier<Boolean> fnIsProgramRunning,
      Supplier<Boolean> fnIsProjectDirty,
      Supplier<Boolean> fnSave) {
    MutableBoolean killRemoteProcess = new MutableBoolean(false);
    stage.setOnCloseRequest(event -> onCloseRequest(
        event,
        killRemoteProcess,
        msgService,
        windowManager,
        fnIsProgramRunning,
        fnIsProjectDirty,
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
      MutableBoolean terminate,
      MessageService msgService,
      WindowManager windowManager,
      Supplier<Boolean> fnIsProgramRunning,
      Supplier<Boolean> fnIsProjectDirty,
      Supplier<Boolean> fnSave) {
    switch (askIfSaveProject(msgService, fnIsProjectDirty)) {
      case YES -> fnSave.get();
      case CANCEL -> {
        event.consume();
        return;
      }
      default -> { /* Do nothing. */ }
    }

    switch (askIfStopProgram(msgService, fnIsProgramRunning)) {
      case YES -> terminate.setTrue();
      case CANCEL -> event.consume();
      default -> { /* Do nothing. */ }
    }

    windowManager.saveWindowStates();
    exportSettings();
  }

  /** アプリケーションの設定をファイルに保存する. */
  private static void exportSettings() {
    try {
      Path filePath = Paths.get(execPath, SETTINGS, BH_SETTINGS_JSON);
      Files.createDirectories(filePath.toAbsolutePath().getParent());
      JsonExporter.export(BhSettings.class, filePath);
    } catch (IOException e) { /* Do nothing.*/ }
  }

  /** アプリケーションの設定をファイルから読み込む. */
  private static void importSettings() {
    try {
      Path filePath = Paths.get(execPath, SETTINGS, BH_SETTINGS_JSON);
      if (!filePath.toFile().exists()) {
        return;
      }
      JsonImporter.imports(BhSettings.class, filePath);
    } catch (IOException e) { /* Do nothing.*/ }
  }

  /**
   * プロジェクトを保存するかどうかを訪ねる.
   * プロジェクトがダーティでないない場合は何も尋ねない.
   *
   * @return YES プロジェクトを保存する <br>
   *        NO プロジェクトを保存しない <br>
   *        CANCEL キャンセルを選択 <br>
   *        NONE_OF_THEM 何も尋ねなかった場合
   */
  public ExclusiveSelection askIfSaveProject(
      MessageService messageService, Supplier<Boolean> fnIsProjectDirty) {
    if (!fnIsProjectDirty.get()) {
      return ExclusiveSelection.NONE_OF_THEM;
    }
    Optional<ButtonType> selected = messageService.alert(
        Alert.AlertType.CONFIRMATION,
        TextDefs.Export.AskIfSave.title.get(),
        null,
        TextDefs.Export.AskIfSave.body.get(),
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
    
    return selected.map(btnType -> {
      if (btnType.equals(ButtonType.YES)) {
        return ExclusiveSelection.YES;
      } else if (btnType.equals(ButtonType.NO)) {
        return ExclusiveSelection.NO;
      } else {
        return ExclusiveSelection.CANCEL;
      }
    }).orElse(ExclusiveSelection.NONE_OF_THEM);
  }

  /** BunnyHop の終了処理. */
  private void terminate(
      RmiLocalBhRuntimeController localCtrl,
      RmiRemoteBhRuntimeController remoteCtrl,
      BhMessageService msgService,
      boolean killRemoteRuntime,
      Boolean oldVal,
      Boolean newVal) {
    if (oldVal == true && newVal == false) {
      localCtrl.end();
      remoteCtrl.end(killRemoteRuntime, BhConstants.BhRuntime.Timeout.REMOTE_END_ON_EXIT);
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
    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    int width =
        (int) Math.min(screenBounds.getWidth() * DEFAULT_SIM_WIDTH_RATE, MAX_DEFAULT_SIM_WIDTH);
    int height =
        (int) Math.min(screenBounds.getHeight() * DEFAULT_SIM_HEIGHT_RATE, MAX_DEFAULT_SIM_HEIGHT);
    config.setWindowedMode(width, height);
    config.setForegroundFPS(60);
    new Lwjgl3Application(simulator, config);
  }

  /** {@link BhCompiler} オブジェクトを作成する. */
  private BhCompiler genCompiler(boolean isLocal) throws IOException {
    var languageFilePath = Paths.get(
        execPath,
        BH_DEF,
        COMPILE,
        LIBS,
        LANGUAGE,
        BhSettings.language,
        BhLibs.TEXT_DB_JS);

    var commonLibPath = Paths.get(
        execPath,
        BH_DEF,
        COMPILE,
        LIBS,
        BhLibs.COMMON_JS);
  
    var fileName = isLocal
        ? BhLibs.LOCAL_COMMON_JS
        : BhLibs.REMOTE_COMMON_JS;
    var localOrRemoteLibPath = Paths.get(
        execPath,
        BH_DEF,
        COMPILE,
        LIBS,
        fileName);
    try {
      return new BhCompilerImpl(languageFilePath, commonLibPath, localOrRemoteLibPath);
    } catch (IOException e) {
      LogManager.logger().error("Failed to initialize Compiler.\n%s".formatted(e));
      throw e;
    }
  }

  private boolean validateNodeViewStyles(
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
   * @return YES プログラムを止める <br>
   *         NO プログラムを止めない <br>
   *         CANCEL キャンセルを選択 <br>
   *         NONE_OF_THEM 何も尋ねなかった場合
   */
  private ExclusiveSelection askIfStopProgram(
      MessageService msgService, Supplier<Boolean> fnIsProgramRunning) {
    if (!fnIsProgramRunning.get()) {
      return ExclusiveSelection.NONE_OF_THEM;
    }
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
    }).orElse(ExclusiveSelection.NONE_OF_THEM);
  }
}
