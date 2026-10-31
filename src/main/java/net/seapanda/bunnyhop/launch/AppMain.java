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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Compile.ConfigFile.ID_LOCAL;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Compile.ConfigFile.ID_REMOTE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Message.LOG_FILE_SIZE_LIMIT;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Message.MAX_LOG_FILE_NUM;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.BH_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.BH_RUNTIME;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.COMPILE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.CONNECTOR_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.EVENT_HANDLERS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.LANGUAGE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.LOG;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.NODE_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.NODE_STYLE_DEF;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.SETTINGS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.TEMPLATE_NODE_LIST;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.VIEW;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_SETTINGS_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_SIM_SETTINGS_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.CALL_STACK_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.COMPILE_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.DEBUG_WINDOW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.FOUNDATION_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.LANGUAGE_FILE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.LOG_MSG;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.NODE_SELECTION_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.NODE_SHIFTER_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.PRIVATE_TEMPLATE_BUTTON_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.SIM_LANGUAGE_FILE;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.TEMPLATE_NODE_LIST_JSON;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.VARIABLE_INSPECTION_VIEW_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.WORKSPACE_FXML;
import static net.seapanda.bunnyhop.utility.Utility.execPath;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.SplashScreen;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncherImpl;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramLauncherImpl;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageDispatcher;
import net.seapanda.bunnyhop.bhprogram.message.IoMessageProcessorImpl;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.bhprogram.runtime.RmiLocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.runtime.RmiRemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.configuration.BhSettings.Runtime;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.common.text.TextFetcher;
import net.seapanda.bunnyhop.compile.CompileConfig;
import net.seapanda.bunnyhop.compile.SourceNodeCollector;
import net.seapanda.bunnyhop.debugger.model.BhDebugger;
import net.seapanda.bunnyhop.debugger.model.DebugMessageProcessorImpl;
import net.seapanda.bunnyhop.debugger.model.DebugUtil;
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
import net.seapanda.bunnyhop.runtime.api.message.BhProgramEvent;
import net.seapanda.bunnyhop.service.FileCollector;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationServiceImpl;
import net.seapanda.bunnyhop.service.message.BhMessageService;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.service.script.BhScriptRepositoryImpl;
import net.seapanda.bunnyhop.service.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.simulator.api.BhSimulator;
import net.seapanda.bunnyhop.simulator.api.BhSimulatorFactory;
import net.seapanda.bunnyhop.simulator.api.BhSimulatorSettings;
import net.seapanda.bunnyhop.simulator.api.KeyboardKey;
import net.seapanda.bunnyhop.simulator.api.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.ui.control.SharedSearchBoxController;
import net.seapanda.bunnyhop.ui.model.ExclusiveSelection;
import net.seapanda.bunnyhop.ui.service.window.WindowStateManager;
import net.seapanda.bunnyhop.ui.service.window.WindowStateManagerImpl;
import net.seapanda.bunnyhop.utility.log.FileLogger;
import net.seapanda.bunnyhop.utility.textdb.JsonTextDatabase;
import net.seapanda.bunnyhop.workspace.control.TrashCanController;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSetController;
import net.seapanda.bunnyhop.workspace.model.CopyAndPaste;
import net.seapanda.bunnyhop.workspace.model.CutAndPaste;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.model.factory.WorkspaceFactoryImpl;
import net.seapanda.bunnyhop.workspace.view.WorkspaceSetViewCallBackRegistry;
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

  /** BhSimulator 終了待ち用. */
  private Future<?> simFuture;
  
  @Override
  public void start(Stage stage) throws Exception {
    var msgService = new BhMessageService();
    Path logDir = Paths.get(execPath, LOG, BhConstants.APP_NAME);
    var logger = new FileLogger(logDir, LOG_MSG, LOG_FILE_SIZE_LIMIT, MAX_LOG_FILE_NUM);
    try {
      Path settingFilePath = Paths.get(execPath, SETTINGS, BH_SETTINGS_JSON);
      BhSettings settings = importFromJson(new BhSettings(), settingFilePath);
      String language = settings.language.get();
      Path textDbFile = Paths.get(execPath, LANGUAGE, language, LANGUAGE_FILE);
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
        Paths.get(execPath, BH_RUNTIME)};
      final Path viewStyleDir = Paths.get(execPath, VIEW, NODE_STYLE_DEF, settings.language.get());
      final Path nodeSelectionFile =
          Paths.get(execPath, BH_DEF, TEMPLATE_NODE_LIST, TEMPLATE_NODE_LIST_JSON);
      final Path nodeDir = Paths.get(execPath, BH_DEF, NODE_DEF);
      final Path cnctrDir = Paths.get(execPath, BH_DEF, CONNECTOR_DEF);
      final Path compileConfigFile = Paths.get(execPath, BH_DEF, COMPILE, COMPILE_JSON);
      final Path simSettingFile = Paths.get(execPath, SETTINGS, BH_SIM_SETTINGS_JSON);
      final Path simLangeFile = Paths.get(execPath, LANGUAGE, language, SIM_LANGUAGE_FILE);

      BhSimulator simulator = createSimulator(simLangeFile, simSettingFile);
      simFuture = simulator.startAsync(settings.simulator.initTimeout.get());
      final SimulatorCmdProcessor simCmdProcessor = simulator.getCmdProcessor();

      final var debugStage = new Stage();
      final var windowStateManager = new WindowStateManagerImpl(stage, debugStage, settings);
      final var wss = new WorkspaceSet();
      final var wssViewCbRegistry = new WorkspaceSetViewCallBackRegistry(wss);
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
      final var nodeSelViewProxy = new BhNodeSelectionViewProxyImpl(
          nodeSelectionViewFile,
          wssCtrl::addNodeSelectionView,
          settings.ui.currentNodeSelectionViewZoomLevel);
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
          nodeViewSuperVisor,
          settings);
      final var commonDataSupplier = new CommonDataSupplier(scriptRepository, nodeFactory, textDb);
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
          nodeViewSuperVisor,
          settings.ui.currentWorkspaceZoomLevel);
      Map<String, CompileConfig> idToCompileConfig =
          createCompileConfigs(compileConfigFile, settings.language.get());
      final CompileConfig localCompileConfig = idToCompileConfig.get(ID_LOCAL);
      final CompileConfig remoteCompileConfig = idToCompileConfig.get(ID_REMOTE);

      final var localRuntimeCtrl =
          new RmiLocalBhRuntimeController(simCmdProcessor, msgService, scriptRepository);
      final var localBhProgramCtrl =
          new LocalBhProgramLauncherImpl(localCompileConfig, localRuntimeCtrl, msgService);
      final var remoteRuntimeCtrl =
          new RmiRemoteBhRuntimeController(msgService, scriptRepository);
      final var remoteBhProgramCtrl =
          new RemoteBhProgramLauncherImpl(remoteCompileConfig, remoteRuntimeCtrl, msgService);

      simulator.getCallbackRegistry().getOnKeyPressed().add(
          key -> onKeyPressed(key, localRuntimeCtrl, remoteRuntimeCtrl, settings.runtime));
      final var sharedSearchBoxCtrl = new SharedSearchBoxController();
      final var breakpointCache = new BreakpointCache(wss);
      final var debugger = new BhDebugger(
          localRuntimeCtrl,
          remoteRuntimeCtrl,
          breakpointCache,
          settings.runtime.currentBhRuntimeType);
      final var debugViewFactory = new DebugViewFactoryImpl(
          callStackViewFile,
          varInspectionViewFile,
          sharedSearchBoxCtrl,
          debugger,
          wss,
          nodeViewSuperVisor,
          settings);
      final var debugUtil = new DebugUtil(settings.debug.maxErrMsgChars);
      new ThreadContextPresenter(debugger, msgService, nodeViewSuperVisor, debugUtil);
      final var entryPointPresenter = new EntryPointPresenter(
          wss, localRuntimeCtrl, remoteRuntimeCtrl, nodeViewSuperVisor);
      final var mainRoutineIds = List.of(
          localCompileConfig.getMainRoutineName(), remoteCompileConfig.getMainRoutineName());
      final var debugMsgProcessor =
          new DebugMessageProcessorImpl(wss, debugger, entryPointPresenter, mainRoutineIds);
      final var msgProcessor = new IoMessageProcessorImpl(msgService);
      new BhProgramMessageDispatcher(
          msgProcessor, debugMsgProcessor, simCmdProcessor, localRuntimeCtrl, mediator);
      new BhProgramMessageDispatcher(
          msgProcessor, debugMsgProcessor, simCmdProcessor, remoteRuntimeCtrl, mediator);
      final var pastePosOffsetCount = new MutableInt(-2);
      final var copyAndPaste = new CopyAndPaste(
          nodeFactory,
          pastePosOffsetCount,
          settings.ui.trackNodeInCurrentWorkspace,
          settings.ui.trackNodeInInactiveWorkspace);
      final var cutAndPaste = new CutAndPaste(
          pastePosOffsetCount,
          settings.ui.trackNodeInCurrentWorkspace,
          settings.ui.trackNodeInInactiveWorkspace);
      final var projImporter = new JsonProjectImporter(nodeFactory, wsFactory, msgService);
      final var projExporter = new JsonProjectExporter(msgService);
      if (!validateNodeViewStyles(nodeRepository, viewStyleFactory)) {
        return;
      }
      final var sceneBuilder = new SceneBuilder(
          guiDefFile,
          debugDefFile,
          stage,
          wss,
          nodeCategoryBuilder.getCategoryRoot(),
          mediator,
          nodeFactory,
          wsFactory,
          debugViewFactory,
          debugUtil,
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
          sharedSearchBoxCtrl,
          trashCanCtrl,
          windowStateManager,
          simulator,
          nodeViewSuperVisor,
          wssViewCbRegistry,
          settings);

      setOnCloseHandler(
          stage,
          localRuntimeCtrl,
          remoteRuntimeCtrl,
          msgService,
          windowStateManager,
          simulator,
          () -> exportSettings(settingFilePath, settings, simSettingFile, simulator),
          remoteRuntimeCtrl::isProgramRunning,
          wss::isDirty,
          () -> sceneBuilder.menuBarCtrl.save(wss));
      msgService.setWindowStyle(sceneBuilder.scene.getStylesheets());
      msgService.setMainMsgArea(sceneBuilder.msgViewCtrl.getMsgArea());
      sceneBuilder.createWindows(stage, debugStage, wsFactory);
      undoRedoAgent.deleteCommands();

      if (SplashScreen.getSplashScreen() != null) {
        SplashScreen.getSplashScreen().close();
      }
    } catch (Throwable e) {
      String cause = "\n  " + (e.getCause() == null ? "" : e.getCause());
      String msg = "Application Start Error.\n  %s%s".formatted(e, cause);
      logger.error(msg);
      msgService.alert(AlertType.ERROR, msg, null, e.toString(), ButtonType.OK);
      System.exit(-1);
    }
  }

  /** シミュレータを作成する. */
  private static BhSimulator createSimulator(Path simLangeFile, Path simSettingFile)
      throws Exception {
    BhSimulatorFactory.initializeTextDb(simLangeFile.toAbsolutePath().toString());
    BhSimulatorSettings settings = importFromJson(new BhSimulatorSettings(), simSettingFile);
    return BhSimulatorFactory.create(settings);
  }

  /**
   * {@code configFilePath} で指定された設定ファイルからコンパイルに関する設定一覧を取得する.
   *
   * @param configFilePath コンパイルに関する設定が書かれたファイルのパス
   * @param language 言語設定
   * @return ID と {@link CompileConfig} の対応関係を格納したマップ
   * @throws AppInitializationException 設定ファイルの読み込みに失敗した場合, もしくは,
   *     ID が local と remote の項目が設定ファイルに存在しない場合
   */
  private static Map<String, CompileConfig> createCompileConfigs(
      Path configFilePath, String language)
      throws AppInitializationException {
    Map<String, CompileConfig> idToCompileConfig;
    try {
      idToCompileConfig = CompileConfig.readConfig(configFilePath, language);
    } catch (Exception e) {
      throw new AppInitializationException("Failed to read a compile configuration file.", e);
    }
    if (idToCompileConfig.containsKey(ID_LOCAL) && idToCompileConfig.containsKey(ID_REMOTE)) {
      return idToCompileConfig;
    }
    throw new AppInitializationException(
        "The compile configuration must contain entries whose IDs are 'local' and 'remote'.\n  (%s)"
            .formatted(configFilePath.toAbsolutePath()));
  }

  /** シミュレータがフォーカスを持っているときにキーが押されたときの処理. */
  private void onKeyPressed(
      KeyboardKey key,
      RmiLocalBhRuntimeController localCtrl,
      RmiRemoteBhRuntimeController remoteCtrl,
      Runtime runtimeSettings) {
    KeyCodeConverter.toBhProgramEventName(key)
        .map(BhProgramEvent::new)
        .ifPresent(bhEvent -> {
          if (runtimeSettings.currentBhRuntimeType.get() == BhRuntimeType.LOCAL) {
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
      WindowStateManager windowStateManager,
      BhSimulator simulator,
      Runnable fnExportSettings,
      Supplier<Boolean> fnIsProgramRunning,
      Supplier<Boolean> fnIsProjectDirty,
      Supplier<Boolean> fnSave) {
    MutableBoolean killRemoteProcess = new MutableBoolean(false);
    stage.setOnCloseRequest(event -> onCloseRequest(
        event,
        killRemoteProcess,
        msgService,
        windowStateManager,
        fnExportSettings,
        fnIsProgramRunning,
        fnIsProjectDirty,
        fnSave));
    stage.showingProperty().addListener((observable, oldValue, newValue) -> terminate(
        localCtrl,
        remoteCtrl,
        simulator,
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
      WindowStateManager windowManager,
      Runnable fnExportSettings,
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
    fnExportSettings.run();
  }

  /** オブジェクトをファイルに保存する. */
  private static void exportToJson(Object src, Path path) {
    Path filePath = Paths.get(execPath, SETTINGS, BH_SETTINGS_JSON);
    var gson = new GsonBuilder().setPrettyPrinting().create();
    try (var jw = gson.newJsonWriter(new FileWriter(path.toAbsolutePath().toString()))) {
      Files.createDirectories(filePath.toAbsolutePath().getParent());
      gson.toJson(src, src.getClass(), jw);
    } catch (IOException e) { /* Do nothing.*/ }
  }

  /** オブジェクトの値をファイルから読み込む. */
  private static <T> T importFromJson(T defaultVal, Path path) {
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(path.toAbsolutePath().toString()))) {
      return gson.fromJson(jr, defaultVal.getClass());
    } catch (IOException e) { /* Do nothing.*/ }
    return defaultVal;
  }

  /** アプリケーションの設定をファイルに保存する. */
  private static void exportSettings(
      Path bhSettingFilePath,
      BhSettings settings,
      Path bhSimSettingFilePath,
      BhSimulator simulator) {
    exportToJson(settings, bhSettingFilePath);
    exportToJson(simulator.getCurrentSettings(), bhSimSettingFilePath);
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
      BhSimulator simulator,
      BhMessageService msgService,
      boolean killRemoteRuntime,
      Boolean oldVal,
      Boolean newVal) {
    if (oldVal == true && newVal == false) {
      localCtrl.end();
      remoteCtrl.end(killRemoteRuntime, BhConstants.BhRuntime.Timeout.REMOTE_END_ON_EXIT);
      msgService.close();
      simulator.halt();
      try {
        simFuture.get();
      } catch (InterruptedException | ExecutionException e) { /* do nothing. */ }
      System.exit(0);
    }
  }

  private boolean validateNodeViewStyles(
      BhNodeRepository repository, BhNodeViewStyleFactory factory) {
    boolean allExist = true;
    for (BhNode node : repository.getAll()) {
      if (!factory.canCreateStyleOf(node.getStyleId())) {
        allExist = false;
        LogManager.logger().error(
            "A node style (%s) is not found among *.json files.", node.getStyleId());
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
