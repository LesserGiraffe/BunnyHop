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
package net.seapanda.bunnyhop.view;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.transform.Scale;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OVERLAP_OPTION;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * ワークスペースを表すビュー (タブの中の描画物に対応)
 * @author K.Koike
 * */
public class WorkspaceView extends Tab {

	private @FXML ScrollPane wsScrollPane;	//!< 操作対象のビュー
	private @FXML Pane wsPane;	//!< 操作対象のビュー
	private @FXML Pane wsWrapper;	//!< wsPane の親ペイン
	private @FXML Polyline rectSelTool;	//!< 矩形選択用ビュー
	private final Workspace workspace;
	private final Vec2D minPaneSize = new Vec2D(0.0, 0.0);
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
			Path filePath = FXMLCollector.INSTANCE.getFilePath(BhParams.Path.WORKSPACE_FXML);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			loader.setController(this);
			loader.setRoot(this);
			loader.load();
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug("failed to initizlize " + WorkspaceView.class.getSimpleName() + "\n" + e.toString());
			return false;
		}

		minPaneSize.x = width;
		minPaneSize.y = height;
		wsPane.setMinSize(minPaneSize.x, minPaneSize.y);	//タブの中の部分の最小サイズを決める
		wsPane.getTransforms().add(new Scale());
		quadTreeMngForBody = new QuadTreeManager(BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
		quadTreeMngForConnector = new QuadTreeManager(BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
		rectSelTool.getPoints().addAll(Stream.generate(() -> 0.0).limit(10).toArray(Double[]::new));
		drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());


		//拡大縮小処理
		wsScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
			if (event.isControlDown()) {
				event.consume();
				boolean zoomIn = event.getDeltaY() >= 0;
				zoom(zoomIn);
			}
		});

		setOnClosed(event -> {
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			BunnyHop.INSTANCE.deleteWorkspace(workspace, userOpeCmd);
			BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
		});

		setOnCloseRequest(event -> {

			if (workspace.getRootNodeList().isEmpty())	//空のワークスペース削除時は警告なし
				return;

			Optional<ButtonType> buttonType = MsgPrinter.INSTANCE.alert(
				Alert.AlertType.CONFIRMATION,
				"ワークスペースの削除",
				null,
				"ワークスペースを削除します.");

			buttonType.ifPresent(btnType -> {
				if (!btnType.equals(ButtonType.OK))
					event.consume();
			});
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
		wsPane.getChildren().add(nodeView);
		rootNodeViewList.add(nodeView);
	}

	/**
	 * ノードビューを削除する
	 * @param nodeView 削除するビュー (nullダメ)
	 * @param saveGuiTreeRels GUIツリー上の親子関係を保持する場合true
	 * */
	public void removeNodeView(BhNodeView nodeView, boolean saveGuiTreeRels) {

		assert(nodeView != null);
		if (!saveGuiTreeRels)
			wsPane.getChildren().remove(nodeView);
		rootNodeViewList.remove(nodeView);
	}

	/**
	 * 4分木空間に矩形を登録する
	 * @param nodeView 登録する矩形を持つBhNodeViewオブジェクト
	 * */
	public void addRectangleToQTSpace(BhNodeView nodeView) {

		nodeView.accept(view -> {
			Pair<QuadTreeRectangle, QuadTreeRectangle> body_cnctr = view.getRegionManager().getRegions();
			quadTreeMngForBody.addQuadTreeObj(body_cnctr._1);
			quadTreeMngForConnector.addQuadTreeObj(body_cnctr._2);
		});
	}

	/**
	 * 引数で指定した矩形と重なるこのワークスペース上にあるノードを探す.
	 * @param rect この矩形と重なるノードを探す.
	 * @param overlapWithBodyPart ノードのボディ部分と重なるノードを探す場合 true. <br>
	 * 							   ノードのコネクタ部分と重なるノードを探す場合 false.
	 * @param option 検索オプション
	 * @return 引数の矩形と重なるノードのビュー
	 * */
	public List<BhNodeView> searchForOverlappedNodeViews(
		QuadTreeRectangle rect,
		boolean overlapWithBodyPart,
		OVERLAP_OPTION option) {

		if (rect == null)
			return new ArrayList<BhNodeView>();

		if (overlapWithBodyPart)
			quadTreeMngForBody.addQuadTreeObj(rect);
		else
			quadTreeMngForConnector.addQuadTreeObj(rect);

		rect.updatePos();
		List<QuadTreeRectangle> overlappedRectList = rect.searchOverlappedRects(option);
		QuadTreeManager.removeQuadTreeObj(rect);

		return overlappedRectList.stream()
				.map(rectangle -> rectangle.<BhNodeView>getRelatedObj())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * WS内でマウスが押された時の処理を登録する
	 * @param handler WS内でマウスが押されたときの処理
	 * */
	public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
		wsPane.setOnMousePressed(handler);
	}

	/**
	 * WS内でマウスがドラッグされた時の処理を登録する
	 * @param handler WS内でマウスがドラッグされたときの処理
	 * */
	public void setOnMouseDraggedHandler(EventHandler<? super MouseEvent> handler) {
		wsPane.setOnMouseDragged(handler);
	}

	/**
	 * WS内でマウスが離された時の処理を登録する
	 * @param handler WS内でマウスが離されたときの処理
	 * */
	public void setOnMouseReleasedHandler(EventHandler<? super MouseEvent> handler) {
		wsPane.setOnMouseReleased(handler);
	}

	/**
	 * ワークスペースの大きさを返す
	 * @return ワークスペースの大きさ
	 */
	public Vec2D getWorkspaceSize() {
		return new Vec2D(wsPane.getWidth(), wsPane.getHeight());
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

		if ((workspaceSizeLevel == BhParams.LnF.MIN_WORKSPACE_SIZE_LEVEL) && !widen)
			return;
		if ((workspaceSizeLevel == BhParams.LnF.MAX_WORKSPACE_SIZE_LEVEL) && widen)
			return;

		workspaceSizeLevel = widen ? workspaceSizeLevel + 1 : workspaceSizeLevel - 1;
		Vec2D currentSize = quadTreeMngForBody.getQTSpaceSize();
		double newWsWidth = widen ? currentSize.x * 2.0 : currentSize.x / 2.0;
		double newWsHeight = widen ? currentSize.y * 2.0 : currentSize.y / 2.0;

		wsPane.setMinSize(newWsWidth, newWsHeight);
		quadTreeMngForBody = new QuadTreeManager(quadTreeMngForBody, BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);
		quadTreeMngForConnector = new QuadTreeManager(quadTreeMngForConnector, BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);

		//全ノードの位置更新
		for (BhNodeView rootView : rootNodeViewList) {
			Vec2D pos = rootView.getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
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
	 * Scene上の座標をWorkspace上の位置に変換して返す
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

		if ((BhParams.LnF.MIN_ZOOM_LEVEL == zoomLevel) && !zoomIn)
			return;

		if ((BhParams.LnF.MAX_ZOOM_LEVEL == zoomLevel) && zoomIn)
			return;

		Scale scale = new Scale();
		if (zoomIn)
			++zoomLevel;
		else
			--zoomLevel;
		double mag = Math.pow(BhParams.LnF.ZOOM_MAGNIFICATION, zoomLevel);
		scale.setX(mag);
		scale.setY(mag);
		wsPane.getTransforms().clear();
		wsPane.getTransforms().add(scale);
		wsWrapper.setPrefSize(
			wsPane.getMinWidth() * wsPane.getTransforms().get(0).getMxx(),
			wsPane.getMinHeight() * wsPane.getTransforms().get(0).getMyy());	//スクロール時にスクロールバーの可動域が変わるようにする
	}

	/**
	 * 複数ノードを同時に移動させるマルチノードシフタのビューをワークスペースに追加する
	 * */
	public void addtMultiNodeShifterView(MultiNodeShifterView multiNodeShifter) {
		wsPane.getChildren().add(multiNodeShifter);
	}

	/**
	 * 矩形選択ツールを表示する
	 * @param upperLeft 表示する矩形のワークスペース上の左上の座標
	 * @param upperLeft 表示する矩形のワークスペース上の右下の座標
	 * */
	public void showSelectionRectangle(Vec2D upperLeft, Vec2D lowerRight) {
		rectSelTool.setVisible(true);
		rectSelTool.getPoints().set(0, upperLeft.x);
		rectSelTool.getPoints().set(1, upperLeft.y);
		rectSelTool.getPoints().set(2, lowerRight.x);
		rectSelTool.getPoints().set(3, upperLeft.y);
		rectSelTool.getPoints().set(4, lowerRight.x);
		rectSelTool.getPoints().set(5, lowerRight.y);
		rectSelTool.getPoints().set(6, upperLeft.x);
		rectSelTool.getPoints().set(7, lowerRight.y);
		rectSelTool.getPoints().set(8, upperLeft.x);
		rectSelTool.getPoints().set(9, upperLeft.y);
		rectSelTool.toFront();
	}

	/**
	 * 矩形選択ツールを非表示にする.
	 * */
	public void hideSelectionRectangle() {
		rectSelTool.setVisible(false);
	}
}






