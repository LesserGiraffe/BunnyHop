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
package net.seapanda.bunnyhop.view.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Showable;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OVERLAP_OPTION;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape.BODY_SHAPE;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CNCTR_SHAPE;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle.CNCTR_POS;

/**
 * ノードのビュークラス <br>
 * 大きさや色などの変更を行うインタフェースを提供 <br>
 * 位置変更のインタフェースを提供
 * View ノード同士の親子関係を処理するインタフェースを提供 <br>
 * イベントハンドラ登録用インタフェースを提供 <br>
 * ノードのシェイプを持つペインを継承している
 * @author K.Koike
 * */
public abstract class BhNodeView extends Pane implements Showable {

	final protected Polygon nodeShape = new Polygon();	//!< 描画されるポリゴン
	final protected Line syntaxErrorMark = new Line(0.0, 0.0, 0.0, 0.0);	//!< 構文エラーノードであることを示す印
	final protected BhNodeViewStyle viewStyle;	//!< ノードの見た目のパラメータオブジェクト
	final private BhNode model;
	final protected SimpleObjectProperty<BhNodeViewGroup> parent = new SimpleObjectProperty<>(null);	//!<このノードが子ノードとなっているConnectiveView のグループ

	final protected BhNodeViewConnector connectorPart;	//!< コネクタ部分
	final private ViewRegionManager viewRegionManager = this.new ViewRegionManager();
	final private ViewTreeManager viewTreeManager = this.new ViewTreeManager();
	final private PositionManager positionManager = this.new PositionManager();
	final private EventManager eventHandlerManager = this.new EventManager();
	final private AppearanceManager appearanceManager;

	/**
	 * 初期化する
	 */
	protected void initialize() {
		getTreeManager().addChild(nodeShape);
		getTreeManager().addChild(syntaxErrorMark);
		syntaxErrorMark.setVisible(false);
		syntaxErrorMark.setMouseTransparent(true);
		appearanceManager.addCssClass(viewStyle.cssClass);
		appearanceManager.addCssClass(BhParams.CSS.CLASS_BHNODE);
	}

	/**
	 * このビューのモデルであるBhNodeを取得する
	 * @return このビューのモデルであるBhNode
	 */
	abstract public BhNode getModel();

	/**
	 * コンストラクタ
	 * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
	 * @param model ビューが表すモデル
	 * */
	protected BhNodeView(BhNodeViewStyle viewStyle, BhNode model) {

		this.setPickOnBounds(false);	//nodeShape 部分だけがMouseEvent を拾うように
		this.viewStyle = viewStyle;
		this.model = model;
		connectorPart = this.new BhNodeViewConnector(viewStyle.connectorShape);
		appearanceManager = this.new AppearanceManager(viewStyle.bodyShape, viewStyle.notchShape);
	}

	/**
	 * BhNodeView を引数にとる関数オブジェクトを実行する<br>
	 * @param visitorFunc BhNodeView を引数にとり処理するオブジェクト
	 * */
	public void accept(Consumer<BhNodeView> visitorFunc) {
		visitorFunc.accept(this);
	}

	/**
	 * ノードの領域関連の処理用インタフェースを返す
	 * @return ノードの領域関連の処理用インタフェース
	 * */
	public ViewRegionManager getRegionManager() {
		return viewRegionManager;
	}

	/**
	 * View の親子関係の処理用インタフェースを返す
	 * @return View の親子関係の処理用インタフェース
	 * */
	public ViewTreeManager getTreeManager() {
		return viewTreeManager;
	}

	/**
	 * 位置変更/取得用インタフェースを返す
	 * @return 位置変更/取得用インタフェース
	 * */
	public PositionManager getPositionManager() {
		return positionManager;
	}

	/**
	 * イベントハンドラ登録用インタフェースを返す
	 * @return イベントハンドラ登録用インタフェース
	 * */
	public  EventManager getEventManager() {
		return eventHandlerManager;
	}

	/**
	 * 見た目変更用インタフェースを返す
	 * @return 見た目変更用インタフェース
	 * */
	public AppearanceManager getAppearanceManager() {
		return appearanceManager;
	}

	/**
	 * コネクタ情報へのアクセス用インタフェースを返す
	 * @return コネクタ情報へのアクセス用インタフェース
	 */
	public BhNodeViewConnector getConnectorManager() {
		return connectorPart;
	}

	/**
	 * サブクラスから処理を指定する必要のある関数のオブジェクトをセットする
	 * @param rearrangeNodesFunc 大きさ変更時に呼ばれる関数. (必ず指定すること)
	 * @param updateAbsPosFunc 絶対位置更新時の関数
	 * */
	final protected void setFuncs(
		Consumer<BhNodeViewGroup> rearrangeNodesFunc,
		BiConsumer<Double, Double> updateAbsPosFunc) {

		if (rearrangeNodesFunc != null)
			appearanceManager.setUpdateAppearanceFunc(rearrangeNodesFunc);

		if (updateAbsPosFunc != null)
			positionManager.setUpdateAbsPosFunc(updateAbsPosFunc);
	}

	/**
	 * BhNodeのコネクタに関する処理をするクラス
	 */
	public class BhNodeViewConnector {

		private ConnectorShape connector;	//!< コネクタ部分の形を表すオブジェクト

		/**
		 * コンストラクタ
		 * @param cnctrShape コネクタの形
		 * */
		BhNodeViewConnector(CNCTR_SHAPE cnctrShape) {
			connector = cnctrShape.SHAPE;
		}

		/**
		 * コネクタの大きさを返す
		 * @return コネクタの大きさ
		 * */
		public Vec2D getConnectorSize() {
			return viewStyle.getConnectorSize();
		}

		/**
		 * コネクタの位置を返す
		 * @return コネクタの位置
		 */
		public CNCTR_POS getConnectorPos() {
			return viewStyle.connectorPos;
		}
	}

	/**
	 * 見た目を変更する処理を行うクラス
	 * */
	public class AppearanceManager {

		private Consumer<BhNodeViewGroup> updateAppearanceFunc;	//!< ノードの形状を更新する関数
		private final ConnectorShape notch;	//!< 切り欠き部分の形を表すオブジェクト
		private BodyShape body;

		/**
		 * コンストラクタ
		 * @param bodyShape 本体の形
		 * @param notchShape 切り欠きの形
		 * */
		public AppearanceManager(BODY_SHAPE bodyShape, CNCTR_SHAPE notchShape) {
			notch = notchShape.SHAPE;
			body = bodyShape.SHAPE;
		}

		/**
		 * CSSの擬似クラスの有効無効を切り替える
		 * @param activate 擬似クラスを有効にする場合true
		 * @param pseudoClassName 有効/無効を切り替える擬似クラス名
		 * */
		public void switchPseudoClassActivation(boolean activate, String pseudoClassName) {

			if (activate) {
				nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
				BhNodeView.this.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
			}
			else {
				nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
				BhNodeView.this.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
			}
		}

		/**
		 * 最前面に移動する
		 * */
		public void toForeGround() {
			BhNodeView.this.toFront();
			if (parent.get() != null)
				parent.get().toForeGround();
		}

		/**
		 * cssクラス名を追加する
		 * @param cssClassName cssクラス名
		 * */
		public void addCssClass(String cssClassName) {
			nodeShape.getStyleClass().add(cssClassName);
			BhNodeView.this.getStyleClass().add(cssClassName + BhParams.CSS.CLASS_SUFFIX_PANE);
			BhNodeView.this.syntaxErrorMark.getStyleClass().add(cssClassName + BhParams.CSS.CLASS_SUFFIX_SYNTAX_ERROR);
		}

		/**
		 * ノードを形作るポリゴンを更新する
		 * @param drawBody ボディを描画する場合 true
		 * */
		protected void updatePolygonShape() {

			nodeShape.getPoints().clear();
			Vec2D bodySize = getRegionManager().getBodySize(false);
			nodeShape.getPoints().addAll(
				body.createVertices(
					bodySize.x,
					bodySize.y,
					connectorPart.connector,
					viewStyle.connectorPos,
					viewStyle.connectorWidth,
					viewStyle.connectorHeight,
					viewStyle.connectorShift,
					notch,
					viewStyle.notchPos,
					viewStyle.notchWidth,
					viewStyle.notchHeight));
			syntaxErrorMark.setEndX(bodySize.x);
			syntaxErrorMark.setEndY(bodySize.y);
		}

		/**
		 * ノードの大きさと子ノードの配置を更新する関数をセットする
		 * */
		public void setUpdateAppearanceFunc(Consumer<BhNodeViewGroup> updateAppearanceFunc) {
			this.updateAppearanceFunc = updateAppearanceFunc;
		}

		/**
		 * ノードの大きさと子ノードの配置を更新する.
		 * 4分木空間上の位置も更新する.
		 * @param child 形状が変わった子ノード
		 * */
		public void updateAppearance(BhNodeViewGroup child) {

			updateAppearanceFunc.accept(child);
			getTreeManager().updateEvenFlg();

			//BhNoteSelectionView のBhNode配置に必要
			Vec2D wholeBodySize = getRegionManager().getNodeSizeIncludingOuter(true);
			BhNodeView.this.setMaxSize(0.0, 0.0);
			if (BhNodeView.this.heightProperty().get() != wholeBodySize.y)
				BhNodeView.this.setHeight(wholeBodySize.y);
		}

		/**
		 * このノード以下の可視性を変更する.
		 * */
		public void setVisible(boolean visible) {

			// ダングリング状態のノードはGUIツリー上では繋がっている.
			// ダングリング状態のノードの可視性を変更しないために, 継承している Pane ではなく nodeShape の可視性を変更する
			accept(view -> view.nodeShape.setVisible(visible));
		}

		/**
		 * ボディの形をセットする. (再描画は行わない)
		 * */
		public void setBodyShape(BODY_SHAPE bodyShape) {
			body = bodyShape.SHAPE;
		}

		/**
		 * 構文エラー表示の有効/無効を切り替える
		 * @param hasError 構文エラー表示を有効にする場合 true. 無効にする場合 false.
		 * */
		public void setSytaxError(boolean hasError) {
			BhNodeView.this.syntaxErrorMark.setVisible(hasError);
		}

		/**
		 * 構文エラー表示の状態を返す
		 * @return 構文エラー表示されている場合 true.
		 * */
		public boolean getSyntaxError() {
			return BhNodeView.this.syntaxErrorMark.isVisible();
		}
	}

	/**
	 * このノードの画面上での領域に関する処理を行うクラス
	 * */
	public class ViewRegionManager {

		private final QuadTreeRectangle wholeBodyRange = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);		//!< ノード全体の範囲
		private final QuadTreeRectangle connectorPartRange = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);	//!< コネクタ部分の範囲

		/**
		 * コネクタ部分同士がこのビューに重なっているビューに対応するモデルを探す
		 * @return コネクタ部分同士がこのビューに重なっているビューに対応するモデルのリスト
		 * */
		public List<BhNode> searchForOverlappedModels() {

			List<QuadTreeRectangle> overlappedRectList = connectorPartRange.searchOverlappedRects(OVERLAP_OPTION.INTERSECT);
			return overlappedRectList.stream()
					.map(rectangle -> rectangle.<BhNodeView>getRelatedObj().model)
					.collect(Collectors.toCollection(ArrayList::new));
		}

		/**
		 * ボディとコネクタ部分の領域を保持するQuadTreeRectangleを返す
		 * @return ボディとコネクタ部分の領域を保持するQuadTreeRectangleオブジェクトのペア
		 * */
		public Pair<QuadTreeRectangle, QuadTreeRectangle> getRegions() {
			return new Pair<>(wholeBodyRange, connectorPartRange);
		}

		public void updateBodyPos(double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
			wholeBodyRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
		}

		public void updateConnectorPos(double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
			connectorPartRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
		}

		/**
		 * 4分木空間からこのView以下の領域判定オブジェクトを消す
		 */
		public void removeQtRectable() {
			accept(view -> {
				QuadTreeManager.removeQuadTreeObj(view.getRegionManager().connectorPartRange);
				QuadTreeManager.removeQuadTreeObj(view.getRegionManager().wholeBodyRange);
			});
		}

		/**
		 * ボディ部分に外部ノードを加えた大きさを返す
		 * @param includeCnctr コネクタ部分を含む大きさを返す場合true
		 * @return 描画ノードの大きさ
		 * */
		public Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
			return viewStyle.getBodyAndOuterSize(includeCnctr);
		}

		/**
		 * 外部ノードを覗くボディ部分の大きさを返す
		 * @param includeCnctr コネクタ部分を含む大きさを返す場合true
		 * @return 描画ノードの大きさ
		 * */
		public Vec2D getBodySize(boolean includeCnctr) {
			return viewStyle.getBodySize(includeCnctr);
		}

		/**
		 * このノードのボディの領域が引数のノードのボディ領域と重なっているかどうか調べる. <br>
		 * @param view このノードとのボディ部分の重なりを調べるノード
		 * @param option 重なり具合を判定するオプション
		 * @return このノードのボディの領域が引数のノードのボディと重なっている場合 true.
		 * */
		public boolean overlapsWith(BhNodeView view, OVERLAP_OPTION option) {
			return wholeBodyRange.overlapsWith(view.getRegionManager().wholeBodyRange, option);
		}
	}

	/**
	 * View の木構造を操作するクラス
	 * */
	public class ViewTreeManager {

		private boolean isEven = true;	//!< ルートを0として、階層が偶数であった場合true.
											//!< ただし, outerノードは親と同階層とする
		/**
		 * NodeView の親をセットする
		 * @param parentGroup このBhNodeViewを保持するBhNodeViewGroup
		 * */
		public void setParentGroup(BhNodeViewGroup parentGroup) {
			parent.setValue(parentGroup);
		}

		/**
		 * NodeView の親を取得する
		 * @return このビューの親となるビュー.  <br>
		 * このビューがルートノードの場合は null を返す
		 * */
		public BhNodeView getParentView() {

			if (parent.get() == null)
				return null;

			return parent.get().getParentView();
		}

		/**
		 * BhNodeViewの木構造上で, このノードを引数のノードと入れ替える. <br>
		 * GUIツリーからは取り除かない.
		 * @param newNode
		 */
		public void replace(BhNodeView newNode) {
			parent.get().replace(BhNodeView.this, newNode);
		}

		/**
		 * このBhNodeView をGUIツリーから取り除く
		 *  BhNodeView の木構造からは取り除かない
		 */
		public void removeFromGUITree() {

			Parent parent = BhNodeView.this.getParent();
			if (parent instanceof Group)
				((Group)parent).getChildren().remove(BhNodeView.this);
			else if (parent instanceof Pane)
				((Pane)parent).getChildren().remove(BhNodeView.this);
		}

		/**
		 * このノード以下の奇偶フラグを更新する
		 * */
		public void updateEvenFlg() {
			accept(view -> {
				BhNodeView parentView = view.getTreeManager().getParentView();

				if (parentView != null) {
					if (view.parent.get().inner && !parentView.viewStyle.bodyShape.equals(BODY_SHAPE.BODY_SHAPE_NONE))
						view.getTreeManager().isEven = !parentView.getTreeManager().isEven;
					else
						view.getTreeManager().isEven = parentView.getTreeManager().isEven;
				}
				else {
					view.getTreeManager().isEven = true;	//ルートはeven
				}
				view.getAppearanceManager().switchPseudoClassActivation(view.getTreeManager().isEven, BhParams.CSS.PSEUDO_IS_EVEN);
			});
		}

		/**
		 * BhNodeView のペインに子要素を追加する
		 * @param child 追加する要素
		 * */
		public void addChild(Node child) {
			BhNodeView.this.getChildren().add(child);
			BhNodeView.this.syntaxErrorMark.toFront();
		}
	}

	/**
	 * 位置変更, 取得操作を行うクラス
	 * */
	public class PositionManager {

		private BiConsumer<Double, Double> updateAbsPosFunc = this::defaultUpdateAbsPos;

		/**
		 * ノードの親からの相対位置を指定する
		 * @param posX 親ノードからの相対位置 X
		 * @param posY 親ノードからの相対位置 Y
		 * */
		public final void setRelativePosFromParent(double posX, double posY) {
			BhNodeView.this.setTranslateX(posX);
			BhNodeView.this.setTranslateY(posY);
		}

		/**
		 * ノードの親からの相対位置を取得する
		 * @return ノードの親からの相対位置
		 * */
		public final Vec2D getRelativePosFromParent() {
			return new Vec2D(BhNodeView.this.getTranslateX(), BhNodeView.this.getTranslateY());
		}

		/**
		 * ワークスペース上での位置を返す
		 * @return ワークスペース上での位置
		 * */
		public Vec2D getPosOnWorkspace() {
			return ViewHelper.INSTANCE.getPosOnWorkspace(BhNodeView.this);
		}

		/**
		 * ノードの絶対値値を更新する (=4分木空間上での位置を更新する)
		 * @param posX 本体部分左上のX位置
		 * @param posY 本体部分左上のY位置
		 * */
		public void updateAbsPos(double posX, double posY) {
			updateAbsPosFunc.accept(posX, posY);
		}

		/**
		 * ノードをGUI上で動かす. WS上の絶対位置(=4分木空間上の位置)は更新されない.
		 * @param diffX X方向移動量
		 * @param diffY Y方向移動量
		 * @return 移動後の新しい位置
		 */
		public Vec2D move(double diffX, double diffY) {

			Vec2D posOnWS = getPosOnWorkspace();
			Vec2D wsSize = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_WORKSPACE_SIZE, model.getWorkspace()).vec2d;
			Vec2D movDistance = ViewHelper.INSTANCE.distance(new Vec2D(diffX, diffY), wsSize, posOnWS);
			Vec2D curRelPos = getRelativePosFromParent();
			double newPosX = curRelPos.x + movDistance.x;
			double newPosY = curRelPos.y + movDistance.y;
			setRelativePosFromParent(newPosX, newPosY);	//GUI上での移動
			return getPosOnWorkspace();
		}

		/**
		 * このノードの絶対位置を更新する
		 * @param posX 本体部分左上のX位置
		 * @param posY 本体部分左上のY位置
		 * */
		public void defaultUpdateAbsPos(double posX, double posY) {

			Vec2D bodySize = getRegionManager().getBodySize(false);
			double bodyUpperLeftX = posX;
			double bodyUpperLeftY = posY;
			double bodyLowerRightX = posX + bodySize.x;
			double bodyLowerRightY = posY + bodySize.y;
			double cnctrUpperLeftX = 0.0;
			double cnctrUpperLeftY = 0.0;
			double cnctrLowerRightX = 0.0;
			double cnctrLowerRightY = 0.0;

			double boundsWidth = viewStyle.connectorWidth * viewStyle.connectorBoundsRate;
			double boundsHeight = viewStyle.connectorHeight * viewStyle.connectorBoundsRate;

			if (viewStyle.connectorPos == CNCTR_POS.LEFT) {
				cnctrUpperLeftX = posX - (boundsWidth + viewStyle.connectorWidth) / 2.0;
				cnctrUpperLeftY = posY - (boundsHeight - viewStyle.connectorHeight) / 2.0 + viewStyle.connectorShift;
				cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
				cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
			}
			else if (viewStyle.connectorPos == CNCTR_POS.TOP) {
				cnctrUpperLeftX = posX - (boundsWidth - viewStyle.connectorWidth) / 2.0 + viewStyle.connectorShift;
				cnctrUpperLeftY = posY - (boundsHeight + viewStyle.connectorHeight) / 2.0;
				cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
				cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
			}
			viewRegionManager.updateBodyPos(bodyUpperLeftX, bodyUpperLeftY, bodyLowerRightX, bodyLowerRightY);
			viewRegionManager.updateConnectorPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
		}

		/**
		 * 絶対位置更新用関数をセットする
		 * */
		void setUpdateAbsPosFunc(BiConsumer<Double, Double> updateAbsPosFunc) {
			this.updateAbsPosFunc = updateAbsPosFunc;
		}
	}

	public class EventManager {

		/**
		 * マウス押下時のイベントハンドラを登録する
		 * @param handler 登録するイベントハンドラ
		 * */
		public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
			nodeShape.setOnMousePressed(handler);
		}

		/**
		 * マウスドラッグ中のイベントハンドラを登録する
		 * @param handler 登録するイベントハンドラ
		 * */
		public void setOnMouseDraggedHandler(EventHandler<? super MouseEvent> handler) {
			nodeShape.setOnMouseDragged(handler);
		}

		/**
		 * マウスドラッグを検出したときのイベントハンドラを登録する
		 * @param handler 登録するイベントハンドラ
		 * */
		public void setOnDragDetectedHandler(EventHandler<? super MouseEvent> handler) {
			nodeShape.setOnDragDetected(handler);
		}

		/**
		 * マウスボタンを離した時のイベントハンドラを登録する
		 * @param handler 登録するイベントハンドラ
		 * */
		public void setOnMouseReleasedHandler(EventHandler<? super MouseEvent> handler) {
			nodeShape.setOnMouseReleased(handler);
		}

		/**
		 * この view のマウスイベント受け取り対象にイベントを伝える
		 * @param event イベント受信対象に伝えるイベント
		 * */
		public void propagateEvent(Event event) {
			nodeShape.fireEvent(event);
		}
	}
}













