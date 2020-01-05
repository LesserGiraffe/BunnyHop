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
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.constant.Rem;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.ViewInitializationException;

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


	public MultiNodeShifterView() throws ViewInitializationException {

		try {
			Path filePath = FXMLCollector.INSTANCE.getFilePath(BhParams.Path.MULTI_NODE_SHIFTER_FXML);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			loader.setController(this);
			loader.setRoot(this);
			loader.load();
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(getClass().getSimpleName() + "\n" + e.toString());
			throw new ViewInitializationException("Failed to initialize  " + getClass().getSimpleName());
		}

		setVisible(false);
		createShape();
		setPickOnBounds(false);
		shifterBase.setPickOnBounds(false);
		shifterArrow.setMouseTransparent(true);
		node_link.addListener((MapChangeListener<BhNode, Line>)(change -> {
			if (change.getMap().size() >= 2) {
				setVisible(true);
				toFront();
			}
			else {
				setVisible(false);
			}
		}));
	}

	/**
	 * 新しくリンクを作ってリンクとマルチノードシフタの位置の更新を行う.
	 * @param node 新しくマニピュレータとリンクするノード. <br>
	 * 				既にリンクがあるノードは, リンクとマルチノードシフタの位置の更新だけ行う.
	 * */
	public void createLink(BhNode node) {

		if (!node_link.containsKey(node)) {
			var newLink = new Line(0.0, 0.0, 0.0, 0.0);
			newLink.getStyleClass().add(BhParams.CSS.CLASS_NODE_SHIFTER_LINK);
			newLink.setStrokeDashOffset(1.0);
			node_link.put(node, newLink);
			getChildren().add(newLink);
			shifterBase.toFront();
		}
		updateShifterAndAllLinkPositions();
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
			updateShifterAndAllLinkPositions();
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
			updateLinkPosForShifter(link);
		}
	}

	/**
	 * シフタと全リンクの位置を更新する.
	 * */
	private void updateShifterAndAllLinkPositions() {

		if (node_link.size() == 0)
			return;

		//マルチノードシフタの新しい位置を計算する
		double shifterX = 0.0;
		double shifterY = 0.0;
		for (BhNode node : node_link.keySet()) {
			Point2D linkPos = calcLinkPosForNode(node);
			shifterX += linkPos.getX();
			shifterY += linkPos.getY();
		}
		shifterX = shifterX / node_link.size() - shifterCircle.getRadius();
		shifterY = shifterY / node_link.size() - shifterCircle.getRadius();
		setPosOnWorkspace(shifterX, shifterY);

		for (BhNode node : node_link.keySet()) {
			updateLinkPos(node);
		}
	}

	/**
	 * ノード側のリンクの端点位置を計算する.
	 * @param node このノードに繋がるリンクの端点位置を計算する
	 * @return リンクの端点位置
	 * */
	private Point2D calcLinkPosForNode(BhNode node) {

		final double yOffset = 0.5 * Rem.VAL;
		Pair<Vec2D, Vec2D> bodyRange = MsgService.INSTANCE.getNodeBodyRange(node);
		double linkPosX = (bodyRange._1.x + bodyRange._2.x) / 2;
		double linkPosY = bodyRange._1.y + yOffset;
		return new Point2D(linkPosX, linkPosY);
	}


	/**
	 * シフタ側のリンクの端点を更新する.
	 * @param link 端点を更新するリンク
	 * */
	private void updateLinkPosForShifter(Line link) {

		double x = link.getEndX();
		double y = link.getEndY();
		if (0.0 <= x && x < 1.0)
			x = 1.0;
		else if (-1.0 < x && x < 0.0)
			x = -1.0;

		if (0.0 <= y && y < 1.0)
			y = 1.0;
		else if (-1.0 < y && y < 0.0)
			y = -1.0;

		double len = Util.INSTANCE.fastSqrt(x * x + y * y);
		double cosVal = x / len;
		double sinVal = y / len;
		double r = shifterCircle.getRadius();
		link.setStartX(r * cosVal);
		link.setStartY(r * sinVal);
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

		Vec2D distance = ViewHelper.INSTANCE.distance(diff, wsSize, getPosOnWorkspace());
		setPosOnWorkspace(getTranslateX() + distance.x, getTranslateY() + distance.y);

		if (moveLink) {
			for (BhNode node : node_link.keySet()) {
				updateLinkPos(node);
			}
		}

		return distance;
	}

	/**
	 * マルチノードシフタのワークスペース上での位置を取得する
	 * */
	private Vec2D getPosOnWorkspace() {
		return ViewHelper.INSTANCE.getPosOnWorkspace(this);
	}

	/**
	 * マルチノードシフタのワークスペース上での位置を設定する
	 * */
	private void setPosOnWorkspace(double x, double y) {
		setTranslateX(x);
		setTranslateY(y);
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
		shifterCircle.setCenterX(0);
		shifterCircle.setCenterY(0);

		double l = 0.3;
		double k = 0.42;
		shifterArrow.getPoints().addAll(
			      0.5 * BhParams.LnF.NODE_SHIFTER_SIZE - radius,                                           0.0 - radius,
			(1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         k * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         k * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         l * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			            BhParams.LnF.NODE_SHIFTER_SIZE - radius,       0.5 * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			      0.5 * BhParams.LnF.NODE_SHIFTER_SIZE - radius,             BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        l * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 + l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - k) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (1.0 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
													  0.0 - radius,       0.5 * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         l * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			(0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         k * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE - radius,         k * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        k * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius,
			        l * BhParams.LnF.NODE_SHIFTER_SIZE - radius, (0.5 - l) * BhParams.LnF.NODE_SHIFTER_SIZE - radius);
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
			node_link.values().forEach(link -> link.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true));
		}
		else {
			shifterBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			shifterCircle.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			shifterArrow.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			node_link.values().forEach(link -> link.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false));
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







