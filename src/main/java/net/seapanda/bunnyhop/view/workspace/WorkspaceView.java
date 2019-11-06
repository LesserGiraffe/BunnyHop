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
package net.seapanda.bunnyhop.view.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OVERLAP_OPTION;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.viewprocessor.CallbackInvoker;

/**
 * ワークスペースを表すビュー (タブの中の描画物に対応)
 * @author K.Koike
 * */
public class WorkspaceView extends Tab {

	private @FXML ScrollPane wsScrollPane;	//!< 操作対象のビュー
	private @FXML WorkspaceViewPane wsPane;	//!< BhNodeを保持するペイン
	private @FXML WorkspaceViewPane errInfoPane;	//!< エラー情報表示用
	private @FXML Pane wsWrapper;	//!< wsPane の親ペイン
	private @FXML Polygon rectSelTool;	//!< 矩形選択用ビュー
	private final Workspace workspace;
	private final Vec2D minPaneSize = new Vec2D(0.0, 0.0);
	/** このワークスペースにあるルートノードビューとルートノード以下のノードを持つGorupオブジェクトのマップ */
	private final Map<BhNodeView, Group> rootNodeToGroup = new HashMap<>();
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

		setErrInfoPaneListener();
		minPaneSize.x = width;
		minPaneSize.y = height;
		//タブの中の部分のサイズを決める. スクロールバーが表示されなくなるので setPrefSize() は使わない.
		errInfoPane.setHolder(this);
		wsPane.setHolder(this);
		wsPane.setMinSize(minPaneSize.x, minPaneSize.y);
		wsPane.setMaxSize(minPaneSize.x, minPaneSize.y);
		wsPane.getTransforms().add(new Scale());
		quadTreeMngForBody = new QuadTreeManager(BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
		quadTreeMngForConnector = new QuadTreeManager(BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
		rectSelTool.getPoints().addAll(Stream.generate(() -> 0.0).limit(8).toArray(Double[]::new));
		drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());
		setEventHandlers();
		setText(workspace.getName());
		return true;
	}

	private void setEventHandlers() {

		wsScrollPane.addEventFilter(ScrollEvent.ANY, this::onScroll);
		setOnCloseRequest(this::onCloseRequest);
		setOnClosed(this::onClosed);
	}

	/**
	 * スクロール時の処理
	 */
	private void onScroll(ScrollEvent event) {

		if (event.isControlDown()) {
			event.consume();
			boolean zoomIn = event.getDeltaY() >= 0;
			zoom(zoomIn);
		}
	}

	/**
	 * ワークスペース削除命令を受けた時の処理
	 */
	private void onCloseRequest(Event event) {

		//空のワークスペース削除時は警告なし
		if (workspace.getRootNodeList().isEmpty())
			return;

		Optional<ButtonType> buttonType = MsgPrinter.INSTANCE.alert(
			Alert.AlertType.CONFIRMATION,
			"ワークスペースの削除",
			null,
			"「" + this.getText() + "」 を削除します.");

		// このイベントハンドラを抜けるとき, TabDragPolicy.FIXED にしないと
		// タブ消しをキャンセルした後, そのタブが使えなくなる.
		getTabPane().setTabDragPolicy(TabDragPolicy.FIXED);
		buttonType.ifPresent(btnType -> {
			if (!btnType.equals(ButtonType.OK))
				event.consume();
		});
	}

	/**
	 * ワークスペース削除時の処理
	 */
	private void onClosed(Event event) {

		UserOperationCommand userOpeCmd = new UserOperationCommand();
		BunnyHop.INSTANCE.deleteWorkspace(workspace, userOpeCmd);
		BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
	}

	private void setErrInfoPaneListener() {

		wsPane.prefWidthProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setPrefWidth(newVal.doubleValue()));
		wsPane.prefHeightProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setPrefHeight(newVal.doubleValue()));
		wsPane.maxWidthProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setMaxWidth(newVal.doubleValue()));
		wsPane.maxHeightProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setMaxHeight(newVal.doubleValue()));
		wsPane.minWidthProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setMinWidth(newVal.doubleValue()));
		wsPane.minHeightProperty().addListener(
			(observable, oldVal, newVal) -> errInfoPane.setMinHeight(newVal.doubleValue()));
		wsPane.getTransforms().addListener((ListChangeListener.Change<? extends Transform> change) -> {
			errInfoPane.getTransforms().clear();
			change.getList().forEach(errInfoPane.getTransforms()::add);
		});
	}

	/**
	 * ノードビューを追加する
	 * @param nodeView 追加するノードビュー
	 */
	public void addNodeView(BhNodeView nodeView) {

		if (rootNodeToGroup.containsKey(nodeView))
			return;

		var group = new Group();
		var shadowGroup = new Group();
		shadowGroup.setId(BhParams.Fxml.ID_NODE_VIEW_SHADOW_PANE);
		group.getChildren().add(shadowGroup);
		nodeView.getTreeManager().addToGUITree(group);
		wsPane.getChildren().add(group);
		rootNodeToGroup.put(nodeView, group);
	}

	/**
	 * ノードビューを削除する
	 * @param nodeView 削除するビュー
	 */
	public void removeNodeView(BhNodeView nodeView) {

		if (!rootNodeToGroup.containsKey(nodeView))
			return;

		Group group = rootNodeToGroup.get(nodeView);
		nodeView.getTreeManager().removeFromGUITree();
		wsPane.getChildren().remove(group);
		rootNodeToGroup.remove(nodeView);
	}

	/**
	 * 4分木空間に矩形を登録する
	 * @param nodeView 登録する矩形を持つBhNodeViewオブジェクト
	 */
	public void addRectangleToQTSpace(BhNodeView nodeView) {

		CallbackInvoker.invoke(
			view -> {
				Pair<QuadTreeRectangle, QuadTreeRectangle> body_cnctr = view.getRegionManager().getRegions();
				quadTreeMngForBody.addQuadTreeObj(body_cnctr._1);
				quadTreeMngForConnector.addQuadTreeObj(body_cnctr._2);
			},
			nodeView);
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
		QuadTreeRectangle rect, boolean overlapWithBodyPart, OVERLAP_OPTION option) {

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
	public void setOnMousePressed(EventHandler<? super MouseEvent> handler) {
		wsPane.setOnMousePressed(handler);
	}

	/**
	 * WS内でマウスがドラッグされた時の処理を登録する
	 * @param handler WS内でマウスがドラッグされたときの処理
	 * */
	public void setOnMouseDragged(EventHandler<? super MouseEvent> handler) {
		wsPane.setOnMouseDragged(handler);
	}

	/**
	 * WS内でマウスが離された時の処理を登録する
	 * @param handler WS内でマウスが離されたときの処理
	 * */
	public void setOnMouseReleased(EventHandler<? super MouseEvent> handler) {
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
		wsPane.setMaxSize(newWsWidth, newWsHeight);
		quadTreeMngForBody = new QuadTreeManager(quadTreeMngForBody, BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);
		quadTreeMngForConnector = new QuadTreeManager(quadTreeMngForConnector, BhParams.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);

		//全ノードの位置更新
		for (BhNodeView rootView : rootNodeToGroup.keySet()) {
			Vec2D pos = rootView.getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			rootView.getPositionManager().setPosOnWorkspace(pos.x, pos.y);
		}
		drawGridLines(newWsWidth, newWsHeight, quadTreeMngForBody.getNumPartitions());
		recalculateScrollableRange();
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
		recalculateScrollableRange();
	}

	/**
	 * スクロール可能な範囲を再計算する
	 */
	private void recalculateScrollableRange() {

		double magX = wsPane.getTransforms().get(0).getMxx();
		double magY = wsPane.getTransforms().get(0).getMyy();

		// 全ノードの内の右端の最大の位置と下端の最大の位置
		Vec2D maxRightAndBottomPosOfNodes = rootNodeToGroup.keySet().stream()
			.map(nodeView -> {
				Vec2D nodeSize = nodeView.getRegionManager().getNodeSizeIncludingOuter(false);
				Vec2D nodePos = nodeView.getPositionManager().getPosOnWorkspace();
				return new Vec2D(
					magX * (nodePos.x + nodeSize.x) + BhParams.LnF.NODE_SCALE * 20,
					magY * (nodePos.y + nodeSize.y) + BhParams.LnF.NODE_SCALE * 20);
			})
			.reduce(
				new Vec2D(0.0, 0.0),
				(accum, pos) -> new Vec2D(Math.max(accum.x, pos.x), Math.max(accum.y, pos.y)));

		Vec2D zoomedWsPaneSize = new Vec2D(wsPane.getMinWidth() * magX, wsPane.getMinHeight() * magY);
		double scrollableHorizontalRange = Math.max(maxRightAndBottomPosOfNodes.x, zoomedWsPaneSize.x);
		double scrollableVerticalRange = Math.max(maxRightAndBottomPosOfNodes.y, zoomedWsPaneSize.y);
		wsWrapper.setPrefSize(scrollableHorizontalRange, scrollableVerticalRange);

	}

	/**
	 * ワークスペース上での node の右下の位置を取得する
	 * @param node 右下の位置を取得するノードビュー
	 * @param includeOuter 外部ノード
	 */
	public Vec2D getLowerRightPosOnWorkspace(BhNodeView node) {

		Vec2D nodeSize = node.getRegionManager().getNodeSizeIncludingOuter(false);
		Vec2D nodePos = node.getPositionManager().getPosOnWorkspace();
		return new Vec2D(nodePos.x + nodeSize.x, nodePos.y + nodeSize.y);
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
	 * @param lowerRight 表示する矩形のワークスペース上の右下の座標
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
		rectSelTool.toFront();
	}

	/**
	 * 矩形選択ツールを非表示にする.
	 * */
	public void hideSelectionRectangle() {
		rectSelTool.setVisible(false);
	}

	/**
	 * 引数で指定したノードビューが中央に表示されるようにスクロールする.
	 * このワークスペースビューに存在しないノードビューを指定した場合は, なにもしない.
	 * @param nodeView 中央に表示するノードビュー
	 */
	public void lookAt(BhNodeView nodeView) {

		WorkspaceView wsView = ViewHelper.INSTANCE.getWorkspaceView(nodeView);
		if (!this.equals(wsView))
			return;

		getTabPane().getSelectionModel().select(this);

		var zoomedWsSize = new Vec2D(wsWrapper.getPrefWidth(), wsWrapper.getPrefHeight());
		var scrollPaneUpperLeftCenterPos = new Vec2D(wsScrollPane.getWidth() * 0.5, wsScrollPane.getHeight() * 0.5);
		var scrollPaneLowerRightCenterPos = new Vec2D(
			zoomedWsSize.x - scrollPaneUpperLeftCenterPos.x,
			zoomedWsSize.y - scrollPaneUpperLeftCenterPos.y);
		var scrollableRange = new Vec2D(
			Math.max(1.0, scrollPaneLowerRightCenterPos.x - scrollPaneUpperLeftCenterPos.x),
			Math.max(1.0, scrollPaneLowerRightCenterPos.y - scrollPaneUpperLeftCenterPos.y));
		var nodeViewCenterPos = getCenterPosOnZoomedWorkspace(nodeView);
		var scrollBarPos = new Vec2D(
			(nodeViewCenterPos.x - scrollPaneUpperLeftCenterPos.x) / scrollableRange.x,
			(nodeViewCenterPos.y - scrollPaneUpperLeftCenterPos.y) / scrollableRange.y);

		scrollBarPos.x = scrollBarPos.x * (wsScrollPane.getHmax() - wsScrollPane.getHmin()) + wsScrollPane.getHmin();
		scrollBarPos.y = scrollBarPos.y * (wsScrollPane.getVmax() - wsScrollPane.getVmin()) + wsScrollPane.getVmin();
		wsScrollPane.setHvalue(Util.INSTANCE.clamp(scrollBarPos.x, wsScrollPane.getHmin(), wsScrollPane.getHmax()));
		wsScrollPane.setVvalue(Util.INSTANCE.clamp(scrollBarPos.y, wsScrollPane.getVmin(), wsScrollPane.getVmax()));
	}

	/**
	 * ズームしたワークスペース上でのノードビューの中心位置を返す.
	 */
	private Vec2D getCenterPosOnZoomedWorkspace(BhNodeView nodeView) {

		double magX = wsPane.getTransforms().get(0).getMxx();
		double magY = wsPane.getTransforms().get(0).getMyy();
		Vec2D nodeSize = nodeView.getRegionManager().getBodySize(false);
		Vec2D nodePos = nodeView.getPositionManager().getPosOnWorkspace();
		return new Vec2D(magX * (nodePos.x + nodeSize.x * 0.5), magY * (nodePos.y + nodeSize.y * 0.5));
	}
}


















