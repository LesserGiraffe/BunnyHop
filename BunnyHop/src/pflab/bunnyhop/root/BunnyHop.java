/**
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
package pflab.bunnyhop.root;

import pflab.bunnyhop.configfilereader.FXMLCollector;
import java.io.IOException;
import java.net.URL;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.control.FoundationController;
import pflab.bunnyhop.control.WorkspaceController;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNodeCategoryList;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.model.WorkspaceSet;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.view.WorkspaceView;

/**
 * アプリケーションの初期化およびワークスペースへのアクセスを行うクラス
 * */
public class BunnyHop {
	
	private final WorkspaceSet workspaceSet = new WorkspaceSet();	//!<  ワークスペースの集合
	private final BhNodeCategoryList nodeCategoryList = new BhNodeCategoryList();	//!< BhNode 選択用画面のモデル
	private FoundationController foundationController;
	public static final BunnyHop instance  = new BunnyHop();
	private boolean shoudlSave = false;

	/**
	 * メインウィンドウを作成する
	 * @param stage JavaFx startメソッドのstage
	 * */
	public void createWindow(Stage stage) {
		
		VBox root;
		try {
			Path filePath = FXMLCollector.instance.getFilePath(BhParams.Path.FOUNDATION_FXML);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			root = loader.load();
			foundationController = loader.getController();
			foundationController.init(workspaceSet, nodeCategoryList);
		}
		catch (IOException e) {
			MsgPrinter.instance.errMsgForDebug("failed to load fxml " + BhParams.Path.FOUNDATION_FXML + "\n" + e.toString() + "\n");
			return;
		}
		
		addNewWorkSpace(BhParams.INITIAL_WORKSPACE_NAME, BhParams.DEFAULT_WORKSPACE_HEIGHT, BhParams.DEFAULT_WORKSPACE_HEIGHT, new UserOperationCommand());
		
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		double width = primaryScreenBounds.getWidth() * BhParams.DEFAULT_APP_WIDTH_RATE;
		double height = primaryScreenBounds.getHeight() * BhParams.DEFAULT_APP_HEIGHT_RATE;
		Scene scene = new Scene(root, width, height);
		setCSS(scene);
		stage.setScene(scene);
		stage.setTitle(BhParams.APPLICATION_NAME);
		stage.show();

		//org.scenicview.ScenicView.show(scene);
	}

	/**
	 * BhNode カテゴリリストと, ノード選択パネルを作成する
	 * @return カテゴリリストと, ノード選択パネルの作成に成功した場合 true
	 */
	public boolean genNodeCategoryList() {

		boolean success = nodeCategoryList.genNodeCategoryList();
		if (!success)
			return false;

		MsgTransporter.instance.sendMessage(BhMsg.BUILD_NODE_CATEGORY_LIST_VIEW, nodeCategoryList);
		MsgTransporter.instance.sendMessage(BhMsg.ADD_NODE_SELECTION_PANELS, nodeCategoryList, workspaceSet);
		for (int i = 0; i != BhParams.INITIAL_ZOOM_LEVEL; i += Math.abs(BhParams.INITIAL_ZOOM_LEVEL) / BhParams.INITIAL_ZOOM_LEVEL) {
			boolean zoomIn = BhParams.INITIAL_ZOOM_LEVEL > 0;
			MsgTransporter.instance.sendMessage(BhMsg.ZOOM, new MsgData(zoomIn), nodeCategoryList);
		}
		return true;
	}

	/**
	 * 現在表示中のパネルを隠す
	 * */
	public void hideTemplatePanel() {
		MsgTransporter.instance.sendMessage(BhMsg.HIDE_NODE_SELECTION_PANEL, nodeCategoryList);
	}

	/**
	 * ワークスペースを新しく作成し追加する
	 * @param workspaceName ワークスペース名
	 * @param width ワークスペース幅
	 * @param height ワークスペース高さ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void addNewWorkSpace(String workspaceName, double width, double height, UserOperationCommand userOpeCmd) {

		Workspace ws = new Workspace(workspaceName);
		WorkspaceView wsView = new WorkspaceView(ws);
		wsView.init(width, height);
		WorkspaceController wsController = new WorkspaceController(ws, wsView);
		ws.setMsgProcessor(wsController);
		MsgTransporter.instance.sendMessage(BhMsg.ADD_WORKSPACE, new MsgData(ws, wsView, userOpeCmd), workspaceSet);	
		for (int i = 0; i != BhParams.INITIAL_ZOOM_LEVEL; i += Math.abs(BhParams.INITIAL_ZOOM_LEVEL) / BhParams.INITIAL_ZOOM_LEVEL) {
			boolean zoomIn = BhParams.INITIAL_ZOOM_LEVEL > 0;
			MsgTransporter.instance.sendMessage(BhMsg.ZOOM, new MsgData(zoomIn), ws);
		}
	}
	
	/**
	 * 引数で指定したワークスペースを追加する
	 * @param ws 追加するワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void addWorkspace(Workspace ws, UserOperationCommand userOpeCmd) {
		MsgTransporter.instance.sendMessage(BhMsg.ADD_WORKSPACE, new MsgData(userOpeCmd), ws, workspaceSet);
		for (int i = 0; i != BhParams.INITIAL_ZOOM_LEVEL; i += Math.abs(BhParams.INITIAL_ZOOM_LEVEL) / BhParams.INITIAL_ZOOM_LEVEL) {
			boolean zoomIn = BhParams.INITIAL_ZOOM_LEVEL > 0;
			MsgTransporter.instance.sendMessage(BhMsg.ZOOM, new MsgData(zoomIn), ws);
		}
	}
	
	/**
	 * 引数で指定したワークスペースを削除する
	 * @param ws 消したいワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteWorkspace(Workspace ws, UserOperationCommand userOpeCmd) {
		MsgTransporter.instance.sendMessage(BhMsg.DELETE_WORKSPACE, new MsgData(userOpeCmd), ws, workspaceSet);
	}
	
	/**
	 * 全てのワークスペースを削除する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteAllWorkspace(UserOperationCommand userOpeCmd) {
		
		Workspace[] wsList = workspaceSet.getWorkspaceList().toArray(new Workspace[workspaceSet.getWorkspaceList().size()]);
		for (Workspace ws : wsList) {
			deleteWorkspace(ws, userOpeCmd);
		}
	}

	/**
	 * CSS ファイルを読み込む
	 * @param scene cssの適用先シーングラフ
	 * */
	private void setCSS(Scene scene) {

		Path dirPath = Paths.get(Util.EXEC_PATH, BhParams.Path.VIEW_DIR, BhParams.Path.CSS_DIR);
		Stream<Path> files = null;	//読み込むファイルパスリスト
		try {
			files = Files.walk(dirPath, FOLLOW_LINKS).filter(filePath -> filePath.toString().toLowerCase().endsWith(".css"));
		}
		catch (IOException e) {
			MsgPrinter.instance.errMsgForDebug("css directory not found " + dirPath);
			return;
		}

		files.forEach(path -> {
			URL url = null;
			try {
				scene.getStylesheets().add(path.toUri().toString());
			} catch (Exception e) {
				MsgPrinter.instance.errMsgForDebug(BunnyHop.class.getSimpleName() + ".setCSS\n" + e.toString());
			}
		});
	}

	/**
	 * 現在操作対象になっているワークスペースを返す
	 * @return 現在操作対象になっているワークスペース
	 * */
	public Workspace getCurrentWorkspace() {
		return workspaceSet.getCurrentWorkspace();
	}
	
	/**
	 * undo 用コマンドオブジェクトをundoスタックに積む
	 * @param userOpeCmd undo用コマンドオブジェクト 
	 */
	public void pushUserOpeCmd(UserOperationCommand userOpeCmd) {
		MsgTransporter.instance.sendMessage(BhMsg.PUSH_USER_OPE_CMD, new MsgData(userOpeCmd), workspaceSet);
	}
	
	/**
	 * アプリ終了時の処理を行う
	 * @return アプリの終了を許可する場合trueを返す.
	 */
	public boolean processCloseRequest() {
		
		if (!shoudlSave)
			return true;
		
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("BunnyHop");
		alert.setHeaderText(null);
		alert.setContentText("保存しますか?");
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		Optional<ButtonType> result = alert.showAndWait();
		
		if (result.get().equals(ButtonType.YES))
			return foundationController.getMenuBarController().save(workspaceSet);
		
		return result.get().equals(ButtonType.NO);
	}
	
	/**
	 * 終了時に保存が必要かどうかのフラグをセットする
	 * @param save trueの場合終了時に保存が必要となる
	 */
	public void shouldSave(boolean save) {
		shoudlSave = save;
	}
}








