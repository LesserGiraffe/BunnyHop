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

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.FxmlCollector;
import net.seapanda.bunnyhop.control.FoundationController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceController;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.TrashboxService;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/** アプリケーションの初期化およびワークスペースへのアクセスを行うクラス. */
public class BunnyHop {

  private final WorkspaceSet workspaceSet = new WorkspaceSet();
  /** BhNode 選択用画面のモデル. */
  private BhNodeCategoryList nodeCategoryList;
  private FoundationController foundationController;
  public static final BunnyHop INSTANCE  = new BunnyHop();
  /** 終了時の保存が必要かどうかのフラグ. */
  private boolean shoudlSave = false;
  private Scene scene;

  private BunnyHop() {}

  /**
   * メインウィンドウを作成する.
   *
   * @param stage JavaFx startメソッドのstage
   */
  public boolean createWindow(Stage stage) {
    TrashboxService.INSTANCE.init(workspaceSet);
    nodeCategoryList = genNodeCategoryList().orElse(null);
    if (nodeCategoryList == null) {
      return false;
    }
    VBox root;
    try {
      Path filePath = FxmlCollector.INSTANCE.getFilePath(BhConstants.Path.FOUNDATION_FXML);
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      root = loader.load();
      foundationController = loader.getController();
      boolean success = foundationController.init(workspaceSet, nodeCategoryList);
      if (!success) {
        return false;
      }
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "failed to load fxml " + BhConstants.Path.FOUNDATION_FXML + "\n"
          + e + "\n");
      return false;
    }

    addNewWorkSpace(
        BhConstants.LnF.INITIAL_WORKSPACE_NAME,
        BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT,
        BhConstants.LnF.DEFAULT_WORKSPACE_HEIGHT,
        new UserOperation());
    Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
    double width = primaryScreenBounds.getWidth() * BhConstants.LnF.DEFAULT_APP_WIDTH_RATE;
    double height = primaryScreenBounds.getHeight() * BhConstants.LnF.DEFAULT_APP_HEIGHT_RATE;
    scene = new Scene(root, width, height);
    setCss(scene);
    initStage(stage, scene);
    //ScenicView.show(scene);
    return true;
  }

  /**
   * 引数で指定したステージを初期化する.
   *
   * @param stage 初期化するステージ
   * @param scene ステージに登録するシーン
   */
  private void initStage(Stage stage, Scene scene) {
    String iconPath = Paths.get(
        Util.INSTANCE.execPath,
        BhConstants.Path.VIEW_DIR,
        BhConstants.Path.IMAGES_DIR,
        BhConstants.Path.BUNNY_HOP_ICON).toUri().toString();
    stage.getIcons().add(new Image(iconPath));
    stage.setScene(scene);
    stage.setTitle(BhConstants.APPLICATION_NAME);
    stage.show();
  }

  private Optional<BhNodeCategoryList> genNodeCategoryList() {
    Path filePath = Paths.get(
        Util.INSTANCE.execPath,
        BhConstants.Path.BH_DEF_DIR,
        BhConstants.Path.TEMPLATE_LIST_DIR,
        BhConstants.Path.NODE_TEMPLATE_LIST_JSON);
    return BhNodeCategoryList.create(filePath);
  }

  /**
   * ワークスペースを新しく作成し追加する.
   *
   * @param workspaceName ワークスペース名
   * @param width ワークスペース幅
   * @param height ワークスペース高さ
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNewWorkSpace(
      String workspaceName,
      double width,
      double height,
      UserOperation userOpe) {
    Workspace ws = new Workspace(workspaceName);
    WorkspaceView wsView = new WorkspaceView(ws);
    wsView.init(width, height);
    WorkspaceController wsController;
    try {
      wsController = new WorkspaceController(ws, wsView, new MultiNodeShifterView());
    } catch (ViewInitializationException e) {
      MsgPrinter.INSTANCE.errMsgForDebug(Util.INSTANCE.getCurrentMethodName() + "\n" + e);
      return;
    }
    ws.setMsgProcessor(wsController);
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.ADD_WORKSPACE, new MsgData(ws, wsView, userOpe), workspaceSet);
    for (int i = 0; i < Math.abs(BhConstants.LnF.INITIAL_ZOOM_LEVEL); ++i) {
      boolean zoomIn = BhConstants.LnF.INITIAL_ZOOM_LEVEL > 0;
      MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(zoomIn), ws);
    }
  }

  /**
   * 引数で指定したワークスペースを追加する.
   *
   * @param ws 追加するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addWorkspace(Workspace ws, UserOperation userOpe) {
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.ADD_WORKSPACE, new MsgData(userOpe), ws, workspaceSet);
    for (int i = 0; i < Math.abs(BhConstants.LnF.INITIAL_ZOOM_LEVEL); ++i) {
      boolean zoomIn = BhConstants.LnF.INITIAL_ZOOM_LEVEL > 0;
      MsgTransporter.INSTANCE.sendMessage(BhMsg.ZOOM, new MsgData(zoomIn), ws);
    }
  }

  /**
   * 現在操作対象のワークスペースを取得する.
   *
   * @return 現在操作対象のワークスペース. 存在しない場合は null.
   */
  public Workspace getCurrentWorkspace() {
    return workspaceSet.getCurrentWorkspace();
  }

  /**
   * アプリケーションのワークスペースセットを返す.
   *
   * @return アプリケーションのワークスペースセット
   */
  public WorkspaceSet getWorkspaceSet() {
    return workspaceSet;
  }

  /**
   * 引数で指定したワークスペースを削除する.
   *
   * @param ws 消したいワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void deleteWorkspace(Workspace ws, UserOperation userOpe) {
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.DELETE_WORKSPACE, new MsgData(userOpe), ws, workspaceSet);
  }

  /**
   * 全てのワークスペースを削除する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void deleteAllWorkspace(UserOperation userOpe) {
    Workspace[] wsList = workspaceSet.getWorkspaceList().toArray(
        new Workspace[workspaceSet.getWorkspaceList().size()]);
    for (Workspace ws : wsList) {
      deleteWorkspace(ws, userOpe);
    }
  }

  /**
   * CSS ファイルを読み込む.
   *
   * @param scene css の適用先シーングラフ
   */
  private void setCss(Scene scene) {
    Path dirPath =
        Paths.get(Util.INSTANCE.execPath, BhConstants.Path.VIEW_DIR, BhConstants.Path.CSS_DIR);
    List<Path> files = null;  //読み込むファイルパスリスト
    try {
      files = Files.walk(dirPath, FOLLOW_LINKS).filter(
          filePath -> filePath.toString().toLowerCase().endsWith(".css")).toList();
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("css directory not found " + dirPath);
      return;
    }
    files.forEach(path -> {
      try {
        scene.getStylesheets().add(path.toUri().toString());
      } catch (Exception e) {
        MsgPrinter.INSTANCE.errMsgForDebug(Util.INSTANCE.getCurrentMethodName() + "\n" + e);
      }
    });
  }

  /**
   * undo 用コマンドオブジェクトをundoスタックに積む.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void pushUserOperation(UserOperation userOpe) {
    MsgTransporter.INSTANCE.sendMessage(
        BhMsg.PUSH_USER_OPE_CMD, new MsgData(userOpe), workspaceSet);
  }

  /**
   * アプリ終了時の処理を行う.
   *
   * @return アプリの終了を許可する場合trueを返す.
   */
  public boolean processCloseRequest() {
    if (!shoudlSave) {
      return true;
    }
    Optional<ButtonType> buttonType = MsgPrinter.INSTANCE.alert(
        Alert.AlertType.CONFIRMATION,
        BhConstants.APPLICATION_NAME,
        null,
        "保存しますか",
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

    return buttonType
      .map(btnType -> {
        if (btnType.equals(ButtonType.YES)) {
          return foundationController.getMenuBarController().save(workspaceSet);
        }
        return btnType.equals(ButtonType.NO);
      })
      .orElse(false);
  }

  /**
   * 終了時に保存が必要かどうかのフラグをセットする.
   *
   * @param save trueの場合終了時に保存が必要となる
   */
  public void shouldSave(boolean save) {
    shoudlSave = save;
  }

  /**
   * アプリに設定された全スタイルを返す.
   *
   * @return アプリに設定された全スタイル
   */
  public List<String> getAllStyles() {
    return new ArrayList<>(scene.getStylesheets());
  }

  /** 現在選択されている BhProgram の実行環境がローカルかリモートか調べる. */
  public boolean isBhRuntimeLocal() {
    return foundationController.isBhRuntimeLocal();
  }
}
