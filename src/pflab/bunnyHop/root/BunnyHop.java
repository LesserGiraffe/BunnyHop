package pflab.bunnyHop.root;

import pflab.bunnyHop.configFileReader.FXMLCollector;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.control.FoundationController;
import pflab.bunnyHop.control.WorkspaceController;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNodeCategoryList;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.model.WorkspaceSet;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.WorkspaceView;

/**
 * アプリケーションの初期化およびワークスペースへのアクセスを行うクラス
 * */
public class BunnyHop {

	private static BunnyHop bh = new BunnyHop(); //!< シングルトンインスタンス
	private final WorkspaceSet workspaceSet = new WorkspaceSet();	//!<  ワークスペースの集合
	private final BhNodeCategoryList nodeCategoryList = new BhNodeCategoryList();	//!< BhNode 選択用画面のモデル
	private FoundationController foundationController;
	
	/**
	 * シングルトンアクセスメソッド
	 * */
	public static BunnyHop instance() {
		return bh;
	}

	/**
	 * メインウィンドウを作成する
	 * @param stage JavaFx startメソッドのstage
	 * */
	public void createWindow(Stage stage) {
		
		VBox root;
		try {
			Path filePath = FXMLCollector.instance.getFilePath(BhParams.Path.foundationFxml);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			root = loader.load();
			foundationController = loader.getController();
			foundationController.init(workspaceSet, nodeCategoryList);
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to load fxml " + BhParams.Path.foundationFxml + "\n" + e.getMessage() + "\n");
			return;
		}	
		
		addNewWorkSpace(BhParams.mainWorkspaceName, BhParams.defaultWorkspaceHeight, BhParams.defaultWorkspaceHeight);

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		double width = primaryScreenBounds.getWidth() * BhParams.defaultAppWidthRate;
		double height = primaryScreenBounds.getHeight() * BhParams.defaultAppHeightRate;
		Scene scene = new Scene(root, width, height);
		setCSS(scene);
		stage.setScene(scene);
		stage.setTitle(BhParams.applicationName);
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

		MsgTransporter.instance().sendMessage(BhMsg.BUILD_NODE_CATEGORY_LIST_VIEW, nodeCategoryList);
		MsgTransporter.instance().sendMessage(BhMsg.ADD_NODE_SELECTION_PANELS, nodeCategoryList, workspaceSet);		
		return true;
	}

	/**
	 * 現在表示中のパネルを隠す
	 * */
	public void hideTemplatePanel() {
		MsgTransporter.instance().sendMessage(BhMsg.HIDE_NODE_SELECTION_PANEL, nodeCategoryList);
	}

	/**
	 * ワークスペースを新しく作成し追加する
	 * @param workspaceName ワークスペース名
	 * @param width ワークスペース幅
	 * @param height ワークスペース高さ
	 * */
	public void addNewWorkSpace(String workspaceName, double width, double height) {

		Workspace ws = new Workspace(workspaceName);
		WorkspaceView wsView = new WorkspaceView(ws);
		wsView.init(width, height);
		WorkspaceController wsController = new WorkspaceController(ws, wsView);
		MsgTransporter.instance().setSenderAndReceiver(ws, wsController, new UserOperationCommand());
		MsgTransporter.instance().sendMessage(BhMsg.ADD_WORKSPACE, new MsgData(ws, wsView), workspaceSet);	
	}
	
	/**
	 * 引数で指定したワークスペースを追加する
	 * @param ws 追加するワークスペース
	 */
	public void addWorkSpace(Workspace ws) {
		MsgTransporter.instance().sendMessage(BhMsg.ADD_WORKSPACE, ws, workspaceSet);
	}
	
	/**
	 * 引数で指定したワークスペースを削除する
	 * @param workspace 消したいワークスペース
	 */
	public void deleteWorkspace(Workspace workspace) {
		MsgTransporter.instance().sendMessage(BhMsg.DELETE_WORKSPACE, workspace, workspaceSet);
		MsgTransporter.instance().deleteSenderAndReceiver(workspace, new UserOperationCommand());
	}

	/**
	 * CSS ファイルを読み込む
	 * @param scene cssの適用先シーングラフ
	 * */
	private void setCSS(Scene scene) {

		Path dirPath = Paths.get(Util.execPath, BhParams.Path.viewDir, BhParams.Path.cssDir);
		Stream<Path> files = null;	//読み込むファイルパスリスト
		try {
			files = Files.walk(dirPath).filter(filePath -> filePath.toString().toLowerCase().endsWith(".css"));
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("css directory not found " + dirPath);
			return;
		}

		files.forEach(path -> {
			URL url = null;
			try {
				scene.getStylesheets().add(path.toUri().toString());
			} catch (Exception e) {
				MsgPrinter.instance.ErrMsgForDebug(e.getMessage());
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
}








