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
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.FoundationController;
import net.seapanda.bunnyhop.control.MenuBarController;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeSelectionViewProxyImpl;
import net.seapanda.bunnyhop.control.workspace.TrashboxController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.model.AppRoot;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/** GUI 画面のロードと初期化を行う. */
public class SceneBuilder {

  /** BhNode 選択用画面のモデル. */
  public final BhNodeCategoryList nodeCategoryList;
  public final FoundationController foundationCtrl;
  public final MenuBarController menuBarCtrl;
  public final WorkspaceSetController wssCtrl;
  public final TrashboxController trashboxCtrl;
  public final Scene scene;
  public final WorkspaceSet wss;
  public final AppRoot appRoot;

  /** コンストラクタ. */
  public SceneBuilder(WorkspaceSet wss) throws AppInitializationException {
    this.wss = wss;
    nodeCategoryList = genNodeCategoryList().orElse(null);
    if (nodeCategoryList == null) {
      throw new AppInitializationException("Failed to create a node category list.");
    }
    VBox root;
    try {
      Path filePath = BhService.fxmlCollector().getFilePath(BhConstants.Path.FOUNDATION_FXML);
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      root = loader.load();
      foundationCtrl = loader.getController();
      wssCtrl = foundationCtrl.getWorkspaceSetController();
      menuBarCtrl = foundationCtrl.getMenuBarController();
      trashboxCtrl = wssCtrl.getTrashboxController();
      appRoot = new AppRoot(
        wss, nodeCategoryList, new BhNodeSelectionViewProxyImpl(wssCtrl::addNodeSelectionView));
      wss.setAppRoot(appRoot);
      nodeCategoryList.setAppRoot(appRoot);
      if (!foundationCtrl.initialize(wss, nodeCategoryList)) {
        throw new AppInitializationException("Failed to initialize a FoundationController.");
      }
      scene = genScene(root);
    } catch (IOException e) {
      throw new AppInitializationException(
          "Failed to load %s\n%s".formatted(BhConstants.Path.FOUNDATION_FXML, e));
    }
  }

  /** メインウィンドウを作成する. */
  public void createWindow(Stage stage) {
    String iconPath = Paths.get(
        Utility.execPath,
        BhConstants.Path.VIEW_DIR,
        BhConstants.Path.IMAGES_DIR,
        BhConstants.Path.BUNNY_HOP_ICON).toUri().toString();
    stage.getIcons().add(new Image(iconPath));
    stage.setScene(scene);
    stage.setTitle(BhConstants.APPLICATION_NAME);
    var wsName = TextDefs.Workspace.initialWsName.get();
    createWorkspace(wsName).ifPresent(ws -> wss.addWorkspace(ws, new UserOperation()));
    stage.show();
  }

  private Optional<BhNodeCategoryList> genNodeCategoryList() {
    Path filePath = Paths.get(
        Utility.execPath,
        BhConstants.Path.BH_DEF_DIR,
        BhConstants.Path.TEMPLATE_LIST_DIR,
        BhConstants.Path.NODE_TEMPLATE_LIST_JSON);
    return BhNodeCategoryList.create(filePath);
  }

  private Scene genScene(VBox root) {
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
        Paths.get(Utility.execPath, BhConstants.Path.VIEW_DIR, BhConstants.Path.CSS_DIR);
    List<Path> files = null;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(
          filePath -> filePath.toString().toLowerCase().endsWith(".css")).toList();
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Directory not found.  (%s)".formatted(dirPath));
      return new ArrayList<>();
    }
    return files.stream().map(file -> file.toUri().toString()).toList();
  }

  /** {@link Workspace} とその MVC 構造を作成する. */
  private Optional<Workspace> createWorkspace(String name) {
    Workspace ws = new Workspace(name);
    WorkspaceView wsView;
    WorkspaceController wsController;
    try {
      wsView = new WorkspaceView(
          ws, BhConstants.LnF.DEFAULT_WORKSPACE_WIDTH, BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT);
      wsController = new WorkspaceController(ws, wsView, new MultiNodeShifterView());
    } catch (ViewInitializationException e) {
      BhService.msgPrinter().errForDebug(e.toString());
      return Optional.empty();
    }
    return Optional.of(ws);
  }
}
