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

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncher;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramController;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.FoundationController;
import net.seapanda.bunnyhop.control.MenuBarController;
import net.seapanda.bunnyhop.control.MenuViewController;
import net.seapanda.bunnyhop.control.MessageViewController;
import net.seapanda.bunnyhop.control.SearchBoxController;
import net.seapanda.bunnyhop.control.debugger.BreakpointListController;
import net.seapanda.bunnyhop.control.debugger.DebugViewController;
import net.seapanda.bunnyhop.control.debugger.DebugWindowController;
import net.seapanda.bunnyhop.control.debugger.StepExecutionViewController;
import net.seapanda.bunnyhop.control.debugger.ThreadSelectorController;
import net.seapanda.bunnyhop.control.debugger.ThreadStateViewController;
import net.seapanda.bunnyhop.control.debugger.WorkspaceSelectorController;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeCategoryListController;
import net.seapanda.bunnyhop.control.workspace.TrashCanController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactory;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategory;
import net.seapanda.bunnyhop.model.workspace.CopyAndPaste;
import net.seapanda.bunnyhop.model.workspace.CutAndPaste;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.factory.DebugViewFactory;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

/**
 * GUI 画面のロードと初期化を行う.
 *
 * @author K.Koike
 */
public class SceneBuilder {

  public final Scene scene;
  private final Scene debugScene;
  private final WorkspaceSetController wssCtrl;
  private final DebugWindowController debugWindowCtrl;

  private final WorkspaceSet wss;
  private final TreeItem<BhNodeCategory> nodeCategoryRoot;
  private final ModelAccessNotificationService notifService;
  private final BhNodeFactory nodeFactory;
  private final WorkspaceFactory wsFactory;
  private final DebugViewFactory debugViewFactory;
  private final UndoRedoAgent undoRedoAgent;
  private final BhNodeSelectionViewProxy nodeSelProxy;
  private final LocalBhProgramLauncher localCtrl;
  private final RemoteBhProgramController remoteCtrl;
  private final CopyAndPaste copyAndPaste;
  private final CutAndPaste cutAndPaste;
  private final MessageService msgService;
  private final Debugger debugger;
  private final SearchBoxController searchBoxCtrl;
  private final TrashCanController trashCanCtrl;
  public final MenuBarController menuBarCtrl;
  public final MessageViewController msgViewCtrl;

  /**
   * コンストラクタ.
   *
   * @param mainWindowFxml アプリケーションの GUI のルート要素が定義された FXML のパス.
   * @param debugWindowFxml デバッグウィンドウのルート要素が定義された FXML のパス.
   */
  public SceneBuilder(
      Path mainWindowFxml,
      Path debugWindowFxml,
      WorkspaceSet wss,
      TreeItem<BhNodeCategory> nodeCategoryRoot,
      ModelAccessNotificationService notifService,
      BhNodeFactory nodeFactory,
      WorkspaceFactory wsFactory,
      DebugViewFactory debugViewFactory,
      UndoRedoAgent undoRedoAgent,
      BhNodeSelectionViewProxy nodeSelProxy,
      LocalBhProgramLauncher localCtrl,
      RemoteBhProgramController remoteCtrl,
      ProjectImporter importer,
      ProjectExporter exporter,
      CopyAndPaste copyAndPaste,
      CutAndPaste cutAndPaste,
      MessageService msgService,
      Debugger debugger,
      WorkspaceSetController wssCtrl,
      SearchBoxController searchBoxCtrl,
      TrashCanController trashCanCtrl)
      throws AppInitializationException {
    this.wss = wss;
    this.nodeCategoryRoot = nodeCategoryRoot;
    this.notifService = notifService;
    this.nodeFactory = nodeFactory;
    this.wsFactory = wsFactory;
    this.debugViewFactory = debugViewFactory;
    this.undoRedoAgent = undoRedoAgent;
    this.nodeSelProxy = nodeSelProxy;
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
    this.copyAndPaste = copyAndPaste;
    this.cutAndPaste = cutAndPaste;
    this.msgService = msgService;
    this.debugger = debugger;
    this.searchBoxCtrl = searchBoxCtrl;
    this.trashCanCtrl = trashCanCtrl;
    this.wssCtrl = wssCtrl;
    this.debugWindowCtrl = new DebugWindowController(debugger);
    this.menuBarCtrl = new MenuBarController(
        wss, notifService, undoRedoAgent, importer, exporter, msgService);
    this.msgViewCtrl = new MessageViewController();

    VBox root;
    try {
      FXMLLoader loader = new FXMLLoader(mainWindowFxml.toUri().toURL());
      loader.setControllerFactory(this::createController);
      root = loader.load();
      scene = genMainScene(root);
    } catch (Exception e) {
      throw new AppInitializationException("Failed to load %s\n%s".formatted(mainWindowFxml, e));
    }
    try {
      FXMLLoader loader = new FXMLLoader(debugWindowFxml.toUri().toURL());
      loader.setControllerFactory(this::createController);
      root = loader.load();
      debugScene = new Scene(root);
      debugScene.getStylesheets().addAll(scene.getStylesheets());
    } catch (Exception e) {
      throw new AppInitializationException("Failed to load %s\n%s".formatted(debugWindowFxml, e));
    }
  }

  /** 各種ビューのコントローラを作成する. */
  private Object createController(Class<?> type) {
    if (type == ThreadSelectorController.class) {
      return new ThreadSelectorController(debugger);
    }
    if (type == ThreadStateViewController.class) {
      return new ThreadStateViewController(debugger);
    }
    if (type == StepExecutionViewController.class) {
      return new StepExecutionViewController(debugger);
    }
    if (type == DebugWindowController.class) {
      return debugWindowCtrl;
    }
    if (type == WorkspaceSelectorController.class) {
      return new WorkspaceSelectorController(wss);
    }
    if (type == BreakpointListController.class) {
      return new BreakpointListController(
          wss, notifService, searchBoxCtrl, debugger.getBreakpointRegistry());
    }
    if (type == DebugViewController.class) {
      return new DebugViewController(debugger, debugViewFactory);
    }
    if (type == BhNodeCategoryListController.class) {
      return new BhNodeCategoryListController(nodeCategoryRoot, nodeSelProxy, nodeFactory);
    }
    if (type == SearchBoxController.class) {
      return searchBoxCtrl;
    }
    if (type == TrashCanController.class) {
      return trashCanCtrl;
    }
    if (type == MessageViewController.class) {
      return msgViewCtrl;
    }
    if (type == MenuBarController.class) {
      return menuBarCtrl;
    }
    if (type == MenuViewController.class) {
      return new MenuViewController(
          wssCtrl,
          notifService,
          wsFactory,
          undoRedoAgent,
          nodeSelProxy,
          localCtrl,
          remoteCtrl,
          copyAndPaste,
          cutAndPaste,
          msgService,
          debugWindowCtrl);
    }
    if (type == WorkspaceSetController.class) {
      return wssCtrl;
    }
    if (type == FoundationController.class) {
      return new FoundationController(localCtrl, remoteCtrl);
    }
    return null;
  }

  /** メインウィンドウを作成する. */
  public void createWindow(Stage stage, WorkspaceFactory wsFactory)
      throws ViewConstructionException {
    String iconPath = Paths.get(
        Utility.execPath,
        BhConstants.Path.Dir.VIEW,
        BhConstants.Path.Dir.IMAGES,
        BhConstants.Path.File.BUNNY_HOP_ICON).toUri().toString();
    stage.getIcons().add(new Image(iconPath));
    stage.setScene(scene);
    stage.setTitle(BhConstants.APP_NAME);
    var debugStage = new Stage();
    debugStage.setScene(debugScene);
    debugStage.initOwner(stage);
    debugStage.initStyle(StageStyle.UTILITY);
    debugStage.setOnCloseRequest(WindowEvent::consume);
    createInitialWorkspace(wsFactory);
    stage.show();
    debugStage.show();
    debugWindowCtrl.setVisibility(false);
  }

  /** 初期ワークスペースを作成する. */
  private void createInitialWorkspace(WorkspaceFactory wsFactory) throws ViewConstructionException {
    var wsName = TextDefs.Workspace.initialWsName.get();
    Workspace ws = wsFactory.create(wsName);
    Vec2D wsSize = new Vec2D(
        BhConstants.LnF.DEFAULT_WORKSPACE_WIDTH, BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT);
    wsFactory.setMvc(ws, wsSize);
    wssCtrl.getWorkspaceSet().addWorkspace(ws, new UserOperation());
  }

  private Scene genMainScene(VBox root) {
    Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
    double width = primaryScreenBounds.getWidth() * BhConstants.LnF.DEFAULT_APP_WIDTH_RATE;
    double height = primaryScreenBounds.getHeight() * BhConstants.LnF.DEFAULT_APP_HEIGHT_RATE;
    var scene =  new Scene(root, width, height);
    scene.getStylesheets().addAll(collectCssPaths());
    return scene;
  }

  /** css ファイルのパスをリストにして返す. */
  private Collection<String> collectCssPaths() {
    Path dirPath = Paths.get(Utility.execPath, BhConstants.Path.Dir.VIEW, BhConstants.Path.Dir.CSS);
    try (Stream<Path> paths = Files.walk(dirPath, FOLLOW_LINKS)) {
      return paths.filter(filePath -> filePath.toString()
          .toLowerCase()
          .endsWith(".css"))
          .map(file -> file.toUri().toString())
          .toList();
    } catch (IOException e) {
      LogManager.logger().error("Directory not found.  (%s)".formatted(dirPath));
      return new ArrayList<>();
    }
  }
}
