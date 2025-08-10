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
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramController;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramController;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.FoundationController;
import net.seapanda.bunnyhop.control.MenuBarController;
import net.seapanda.bunnyhop.control.NotificationViewController;
import net.seapanda.bunnyhop.control.SearchBoxController;
import net.seapanda.bunnyhop.control.debugger.DebugWindowController;
import net.seapanda.bunnyhop.control.workspace.TrashboxController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactory;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryTree;
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
import net.seapanda.bunnyhop.view.nodeselection.BhNodeShowcaseBuilder;

/**
 * GUI 画面のロードと初期化を行う.
 *
 * @author K.Koike
 */
public class SceneBuilder {

  public final FoundationController foundationCtrl;
  public final MenuBarController menuBarCtrl;
  public final WorkspaceSetController wssCtrl;
  public final NotificationViewController notifViewCtrl;
  public final TrashboxController trashboxCtrl;
  public final SearchBoxController searchBoxCtrl;
  public final DebugWindowController debugWindowCtrl;
  public final Scene scene;
  private final Scene debugScene;

  /**
   * コンストラクタ.
   *
   * @param mainWindowFxml アプリケーションの GUI のルート要素が定義された FXML のパス.
   * @param debugWindowFxml デバッグウィンドウのルート要素が定義された FXML のパス.
   */
  public SceneBuilder(
      Path mainWindowFxml, Path debugWindowFxml) throws AppInitializationException {
    VBox root;
    try {
      FXMLLoader loader = new FXMLLoader(mainWindowFxml.toUri().toURL());
      root = loader.load();
      foundationCtrl = loader.getController();
      wssCtrl = foundationCtrl.getWorkspaceSetController();
      notifViewCtrl = foundationCtrl.getNotificationViewController();
      menuBarCtrl = foundationCtrl.getMenuBarController();
      trashboxCtrl = wssCtrl.getTrashboxController();
      searchBoxCtrl = notifViewCtrl.getSearchBoxController();
      scene = genMainScene(root);
    } catch (IOException e) {
      throw new AppInitializationException("Failed to load %s\n%s".formatted(mainWindowFxml, e));
    }
    try {
      FXMLLoader loader = new FXMLLoader(debugWindowFxml.toUri().toURL());
      root = loader.load();
      debugWindowCtrl = loader.getController();
      debugScene = new Scene(root);
      debugScene.getStylesheets().addAll(scene.getStylesheets());
    } catch (IOException e) {
      throw new AppInitializationException("Failed to load %s\n%s".formatted(debugWindowFxml, e));
    }
  }

  /** GUI を構築するオブジェクトを初期化する. */
  public void initialze(
      WorkspaceSet wss,
      BhNodeCategoryTree nodeCategoryList,
      BhNodeShowcaseBuilder builder,
      ModelAccessNotificationService service,
      WorkspaceFactory wsFactory,
      DebugViewFactory debugViewFactory,
      UndoRedoAgent undoRedoAgent,
      BhNodeSelectionViewProxy proxy,
      LocalBhProgramController localCtrl,
      RemoteBhProgramController remoteCtrl,
      ProjectImporter importer,
      ProjectExporter exporter,
      CopyAndPaste copyAndPaste,
      CutAndPaste cutAndPaste,
      MessageService msgService,
      Debugger debugger) throws AppInitializationException {
    if (!foundationCtrl.initialize(
        wss,
        nodeCategoryList,
         builder,
         service,
         wsFactory,
         debugViewFactory,
         undoRedoAgent,
         proxy,
         localCtrl,
         remoteCtrl,
         importer,
         exporter,
         copyAndPaste,
         cutAndPaste,
         msgService,
         debugger,
         debugWindowCtrl)) {
      throw new AppInitializationException("Failed to initialize a FoundationController.");
    }
    debugWindowCtrl.initialize(debugger);
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
    Path dirPath =
        Paths.get(Utility.execPath, BhConstants.Path.Dir.VIEW, BhConstants.Path.Dir.CSS);
    List<Path> files = null;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(
          filePath -> filePath.toString().toLowerCase().endsWith(".css")).toList();
    } catch (IOException e) {
      LogManager.logger().error("Directory not found.  (%s)".formatted(dirPath));
      return new ArrayList<>();
    }
    return files.stream().map(file -> file.toUri().toString()).toList();
  }
}
