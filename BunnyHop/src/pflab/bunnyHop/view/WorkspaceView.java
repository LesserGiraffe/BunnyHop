package pflab.bunnyHop.view;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import pflab.bunnyHop.quadTree.QuadTreeManager;
import pflab.bunnyHop.quadTree.QuadTreeRectangle;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Pair;
import pflab.bunnyHop.model.*;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.root.BunnyHop;
import pflab.bunnyHop.configFileReader.FXMLCollector;

/**
 * ワークスペースを表すビュー (タブの中のものに対応)
 * @author K.Koike
 * */
public class WorkspaceView extends Tab {

	private @FXML ScrollPane scrollPane;	//!< 操作対象のビュー
	private @FXML Pane wsPane;	//!< 操作対象のビュー
	private @FXML Pane wsWrapper;	//!< wsPane の親ペイン
	private final Workspace workspace;
	private final Point2D minPaneSize = new Point2D(0.0, 0.0);
	private final ArrayList<BhNodeView> rootNodeViewList = new ArrayList<>();	//このワークスペースにあるルートBhNodeViewのリスト
	private QuadTreeManager quadTreeMngForBody;			//!< ノードの本体部分の重なり判定に使う4分木管理クラス
	private QuadTreeManager quadTreeMngForConnector;	//!< ノードのコネクタ部分の重なり判定に使う4分木管理クラス
	int zoomLevel = 0;	//!< ワークスペースの拡大/縮小の段階
	int workspaceSizeLevel = 0;	//!< ワークスペースの大きさの段階
	
	public WorkspaceView(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * 初期化処理を行う
	 * @param width ワークスペースの初期幅
	 * @param height ワークスペースの初期高さ
	 * @return 初期化に成功した場合 true
	 */
	public boolean init(double width, double height) {

		try {
			Path filePath = FXMLCollector.instance.getFilePath(BhParams.Path.workspaceFxml);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			loader.setController(this);
			loader.setRoot(this);
			loader.load();
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to initizlize " + WorkspaceView.class.getSimpleName() + "\n" + e.toString());
			return false;
		}

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		minPaneSize.x = width;
		minPaneSize.y = height;
		wsPane.setMinSize(minPaneSize.x, minPaneSize.y);	//タブの中の部分の最小サイズを決める		
		wsPane.getTransforms().add(new Scale());
		quadTreeMngForBody = new QuadTreeManager(BhParams.numDivOfQTreeSpace, minPaneSize.x, minPaneSize.y);
		quadTreeMngForConnector = new QuadTreeManager(BhParams.numDivOfQTreeSpace, minPaneSize.x, minPaneSize.y);
		drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());		
		
		//拡大縮小処理
		scrollPane.addEventFilter(ScrollEvent.ANY, event -> {
			if (event.isControlDown()) {
				event.consume();
				boolean zoomIn = event.getDeltaY() >= 0;
				zoom(zoomIn);
			}
		});

		setOnClosed(event -> {
			BunnyHop.instance().deleteWorkspace(workspace);
		});
		
		setOnCloseRequest(event -> {
			
			if (workspace.getRootNodeList().isEmpty())	//ワークスペース削除時は警告なし
				return;
			
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("ワークスペースの削除");
			alert.setHeaderText(null);
			alert.setContentText("ワークスペースを削除します.\n削除すると元に戻せません.");
			Optional<ButtonType> buttonType = alert.showAndWait();			
			if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK))
				return;
			event.consume();
		});
		
		setText(workspace.getWorkspaceName());		
		return true;
	}

	/**
	 * ノードビューを追加する
	 * @param nodeView 追加するノードビュー (nullダメ)
	 * */
	public void addNodeView(BhNodeView nodeView) {
		assert(nodeView != null);
		assert(rootNodeViewList.indexOf(nodeView) == -1);	//すでに登録済みでないかチェック
		wsPane.getChildren().add(nodeView);
		rootNodeViewList.add(nodeView);
	}

	/**
	 * ノードビューを削除する
	 * @param nodeView 削除するビュー (nullダメ)
	 * */
	public void removeNodeView(BhNodeView nodeView) {
		assert(nodeView != null);
		wsPane.getChildren().remove(nodeView);
		rootNodeViewList.remove(nodeView);
	}

	/**
	 * 4分木空間に矩形を登録する
	 * @param nodeView 登録する矩形を持つBhNodeViewオブジェクト
	 * */
	public void addRectangleToQTSpace(BhNodeView nodeView) {

		nodeView.accept(view -> {
			Pair<QuadTreeRectangle, QuadTreeRectangle> body_cnctr = view.getRegionManager().getRegion();
			//quadTreeMngForBody.addQuadTreeObj(body_cnctr._1); // 現状ボディ部分の重なり判定は不要
			quadTreeMngForConnector.addQuadTreeObj(body_cnctr._2);
		});
	}

	/**
	 * WS内でマウスが押された時の処理を登録する
	 * @param handler WS内でマウスが押されたときの処理
	 * */
	public void setOnMousePressedEvent(EventHandler<? super MouseEvent> handler) {
		scrollPane.setOnMousePressed(handler);
	}

	/**
	 * ワークスペースの大きさを返す
	 * @return ワークスペースの大きさ
	 */
	public Point2D getWorkspaceSize() {
		return new Point2D(wsPane.getWidth(), wsPane.getHeight());
	}
	
	/**
	 * このViewに対応しているWorkspace を返す
	 * @return このViewに対応しているWorkspace
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
	
	/**
	 * ワークスペースの大きさを変える
	 * @param widen ワークスペースの大きさを大きくする場合true
	 */
	public void changeWorkspaceViewSize(boolean widen) {
		
		if ((workspaceSizeLevel == BhParams.minWorkspaceSizeLevel) && !widen)
			return;
		if ((workspaceSizeLevel == BhParams.maxWorkspaceSizeLevel) && widen)
			return;
		
		workspaceSizeLevel = widen ? workspaceSizeLevel + 1 : workspaceSizeLevel - 1;		
		Point2D currentSize = quadTreeMngForBody.getQTSpaceSize();
		double newWsWidth = widen ? currentSize.x * 2.0 : currentSize.x / 2.0;
		double newWsHeight = widen ? currentSize.y * 2.0 : currentSize.y / 2.0;
		
		wsPane.setMinSize(newWsWidth, newWsHeight);
		quadTreeMngForBody = new QuadTreeManager(quadTreeMngForBody, BhParams.numDivOfQTreeSpace, newWsWidth, newWsHeight);
		quadTreeMngForConnector = new QuadTreeManager(quadTreeMngForConnector, BhParams.numDivOfQTreeSpace, newWsWidth, newWsHeight);
		
		//全ノードの位置更新
		for (BhNodeView rootView : rootNodeViewList) {
			Point2D pos = rootView.getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			rootView.getPositionManager().updateAbsPos(pos.x, pos.y);
		}
		drawGridLines(newWsWidth, newWsHeight, quadTreeMngForBody.getNumPartitions());
		wsWrapper.setPrefSize(
			wsPane.getMinWidth() * wsPane.getTransforms().get(0).getMxx(),
			wsPane.getMinHeight() * wsPane.getTransforms().get(0).getMyy());	//スクロールバーの可動域が変わるようにする
	}
	
	//デバッグ用
	private void drawGridLines(double width, double height, int numDiv) {
		
		ArrayList<Line> removedList = new ArrayList<>();
		wsPane.getChildren().forEach((content) -> {
			if (content instanceof Line)
				removedList.add((Line)content);
		});
		removedList.forEach((line) -> {
			wsPane.getChildren().remove(line);
		});

		for(int i = 0; i < numDiv; ++i) {
			int x = (int)((width / numDiv) * i);
			wsPane.getChildren().add(0, new Line(x, 0, x, height));
		}
		for(int i = 0; i < numDiv; ++i) {
			int y = (int)((height / numDiv) * i);
			wsPane.getChildren().add(0, new Line(0, y, width, y));
		}
	}
	
	/**
	 * 引数のローカル座標をWorkspace上での位置に変換して返す
	 * @param x Scene座標の変換したいX位置
	 * @param y Scene座標の変換したいY位置
	 * @return 引数の座標のWorkspace上の位置
	 * */
	public javafx.geometry.Point2D sceneToWorkspace(double x, double y) {
		return wsPane.sceneToLocal(x, y);
	}
	
	/**
	 * ワークスペースのズーム処理を行う
	 * @param zoomIn 拡大処理を行う場合true
	 */
	public void zoom(boolean zoomIn) {
		
		if ((BhParams.minZoomLevel == zoomLevel) && !zoomIn)
			return;
		
		if ((BhParams.maxZoomLevel == zoomLevel) && zoomIn)
			return;
		
		Scale scale = new Scale();
		if (zoomIn) {
			scale.setX(wsPane.getTransforms().get(0).getMxx() * BhParams.wsMagnification);
			scale.setY(wsPane.getTransforms().get(0).getMyy() * BhParams.wsMagnification);
			++zoomLevel;
		}
		else {
			scale.setX(wsPane.getTransforms().get(0).getMxx() / BhParams.wsMagnification);
			scale.setY(wsPane.getTransforms().get(0).getMyy() / BhParams.wsMagnification);
			--zoomLevel;
		}
		wsPane.getTransforms().clear();
		wsPane.getTransforms().add(scale);
		wsWrapper.setPrefSize(
			wsPane.getWidth() * wsPane.getTransforms().get(0).getMxx(),
			wsPane.getHeight() * wsPane.getTransforms().get(0).getMyy());	//スクロール時にスクロールバーの可動域が変わるようにする
	}
}






