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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * 複数ノードを同時に移動させるマルチノードシフタのビュー
 * @author K.Koike
 * */
public class MultiNodeShifterView extends Pane {

	/** マルチノードシフタが操作するノードとリンクのマップ  */
	private ObservableMap<BhNode, Line> node_link = FXCollections.observableMap(new HashMap<BhNode, Line>());
	@FXML private Pane shifterBase;
	@FXML private Circle shifterCircle;
	@FXML private Polygon shifterArrow;


	public MultiNodeShifterView() {}

	public boolean init() {

		try {
			Path filePath = FXMLCollector.INSTANCE.getFilePath(BhParams.Path.MULTI_NODE_SHIFTER_FXML);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			loader.setController(this);
			loader.setRoot(this);
			loader.load();
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"failed to initizlize " + MultiNodeShifterView.class.getSimpleName() + "\n" + e.toString());
			return false;
		}

		setVisible(false);
		createShape();
		shifterBase.setPickOnBounds(false);
		shifterArrow.setMouseTransparent(true);
		node_link.addListener((MapChangeListener<BhNode, Line>)(change -> {
			if (change.getMap().size() >= 2)
				setVisible(true);
			else
				setVisible(false);
		}));
		return true;
	}

	/**
	 * 新しくリンクを作ってリンクとマルチノードシフタの位置の更新を行う.
	 * @param node 新しくマニピュレータとリンクするノード. <br>
	 * 				既にリンクがあるノードは, リンクとマルチノードシフタの位置の更新だけ行う.
	 * */
	public void createLink(BhNode node) {

		if (!node_link.containsKey(node)) {
			var newLink = new Line(shifterCircle.getRadius(), shifterCircle.getRadius(), 0.0, 0.0);
			newLink.getStyleClass().add(BhParams.CSS.CLASS_NODE_SHIFTER_LINK);
			newLink.setStrokeDashOffset(1.0);
			node_link.put(node, newLink);
			getChildren().add(newLink);
			shifterBase.toFront();
		}
		updateAllLinkPositions();
	}

	/**
	 * リンクを削除する
	 * @param node マルチノードシフタとのリンクを消すノード.<br>
	 * 				マニピュレータとリンクしていないノードを指定した場合, 何もしない.
	 * */
	public void deleteLink(BhNode node) {

		Line link = node_link.remove(node);
		if (link != null) {
			getChildren().remove(link);
			updateAllLinkPositions();
		}
	}

	/**
	 * リンクの位置を更新する
	 * @param node このノードと繋がるリンクの位置を更新する. <br>
	 * 				マニピュレータとリンクしていないノードを指定した場合, 何もしない.
	 * */
	public void updateLinkPos(BhNode node) {

		Line link = node_link.get(node);
		if (link != null) {
			Point2D linkPos = calcLinkPosForNode(node);
			Point2D newPos = parentToLocal(linkPos);
			link.setEndX(newPos.getX());
			link.setEndY(newPos.getY());
		}
	}

	private void updateAllLinkPositions() {

		if (node_link.size() == 0)
			return;

		//マルチノードシフタの新しい位置を計算する
		Map<BhNode, Point2D> node_nodeLinkPos = new HashMap<>();
		double shifterX = 0.0;
		double shifterY = 0.0;
		for (BhNode node : node_link.keySet()) {
			Point2D linkPos = calcLinkPosForNode(node);
			shifterX += linkPos.getX();
			shifterY += linkPos.getY();
			node_nodeLinkPos.put(node, linkPos);
		}
		shifterX = shifterX / node_link.size() - shifterCircle.getRadius();
		shifterY = shifterY / node_link.size() - shifterCircle.getRadius();
		setTranslateX(shifterX);
		setTranslateY(shifterY);

		//リンクのノード側の点の位置を更新する
		for (BhNode node : node_link.keySet()) {
			Point2D linkPos = node_nodeLinkPos.get(node);
			linkPos = parentToLocal(linkPos);
			Line link = node_link.get(node);
			link.setEndX(linkPos.getX());
			link.setEndY(linkPos.getY());
		}
	}

	/**
	 * ノード側のリンクの端点位置を計算する.
	 * @param node このノードに繋がるリンクの端点位置を計算する
	 * @return リンクの端点位置
	 * */
	private Point2D calcLinkPosForNode(BhNode node) {

		final double yOffset = 10.0;
		Pair<Vec2D, Vec2D> bodyRange = MsgService.INSTANCE.getNodeBodyRange(node);
		double linkPosX = (bodyRange._1.x + bodyRange._2.x) / 2;
		double linkPosY = bodyRange._1.y + yOffset;
		return new Point2D(linkPosX, linkPosY);
	}

	/**
	 * 引数のノードがマルチノードシフタとリンクしているノードがどうか調べる
	 * @param node マルチノードシフタとリンクしているか調べるノード
	 * @return 引数のノードがマルチノードシフタとリンクしているならtrue.
	 * */
	public boolean isLinked(BhNode node) {
		return node_link.containsKey(node);
	}

	/**
	 * マルチノードシフタを移動する
	 * @param diff 移動量
	 * @param wsSize マルチノードシフタがあるワークスペースのサイズ
	 * @param moveLink リンクも動かす場合 true
	 * @return 実際に移動した量
	 * */
	public Vec2D move(Vec2D diff, Vec2D wsSize, boolean moveLink) {

		Vec2D distance = FieldPosCalculator.distance(diff, wsSize, getPosOnWorkspace());
		setTranslateX(getTranslateX() + distance.x);
		setTranslateY(getTranslateY() + distance.y);

		if (moveLink) {
			//リンクのノード側の点の位置を更新する
			for (BhNode node : node_link.keySet()) {
				Point2D linkPos = calcLinkPosForNode(node);
				linkPos = parentToLocal(linkPos);
				Line link = node_link.get(node);
				link.setEndX(linkPos.getX());
				link.setEndY(linkPos.getY());
			}
		}

		return distance;
	}

	/**
	 * マルチノードシフタのワークスペース上での位置を取得する
	 * */
	private Vec2D getPosOnWorkspace() {
		return BhNodeView.getRelativePos(null, this);
	}

	/**
	 * 現在リンクしているノードのリストを取得する
	 * @return 現在リンクしているノードのリスト
	 * */
	public List<BhNode> getLinkedNodeList() {
		return new ArrayList<>(node_link.keySet());
	}

	/**
	 * マルチノードシフタの形を作る
	 * */
	private void createShape() {

		var radius = BhParams.LnF.NODE_SHIFTER_SIZE / 2.0;
		shifterCircle.setRadius(radius);
		shifterCircle.setCenterX(radius);
		shifterCircle.setCenterY(radius);

		double l = 0.3;
		double k = 0.42;
		shifterArrow.getPoints().addAll(
			      0.5 * BhParams.LnF.NODE_SHIFTER_SIZE, 0.0,
			(1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE,         k * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,         k * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,         l * BhParams.LnF.NODE_SHIFTER_SIZE,
			            BhParams.LnF.NODE_SHIFTER_SIZE,       0.5 * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			      0.5 * BhParams.LnF.NODE_SHIFTER_SIZE,             BhParams.LnF.NODE_SHIFTER_SIZE,
			        l * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE, (1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			0.0,                                                 0.5 * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,         l * BhParams.LnF.NODE_SHIFTER_SIZE,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,         k * BhParams.LnF.NODE_SHIFTER_SIZE,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE,         k * BhParams.LnF.NODE_SHIFTER_SIZE,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE,
			        l * BhParams.LnF.NODE_SHIFTER_SIZE, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE);
	}

	/**
	 * CSSの擬似クラスの有効無効を切り替える
	 * @param activate 擬似クラスを有効にする場合true
	 * @param pseudoClassName 有効/無効を切り替える擬似クラス名
	 * */
	public void switchPseudoClassActivation(boolean activate, String pseudoClassName) {

		if (activate) {
			shifterBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
			shifterCircle.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
			shifterArrow.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
		}
		else {
			shifterBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			shifterCircle.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			shifterArrow.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
		}
	}

	/**
	 * マウス押下時のイベントハンドラを登録する
	 * @param handler 登録するイベントハンドラ
	 * */
	public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
		shifterCircle.setOnMousePressed(handler);
	}

	/**
	 * マウスドラッグ中のイベントハンドラを登録する
	 * @param handler 登録するイベントハンドラ
	 * */
	public void setOnMouseDraggedHandler(EventHandler<? super MouseEvent> handler) {
		shifterCircle.setOnMouseDragged(handler);
	}

	/**
	 * マウスボタンを離した時のイベントハンドラを登録する
	 * @param handler 登録するイベントハンドラ
	 * */
	public void setOnMouseReleasedHandler(EventHandler<? super MouseEvent> handler) {
		shifterCircle.setOnMouseReleased(handler);
	}
}







