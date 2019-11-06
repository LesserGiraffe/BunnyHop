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

import java.util.Optional;

import javafx.scene.control.Button;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.view.node.part.ImitationCreator;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * ConnectiveNode に対応するビュークラス
 * @author K.Koike
 * */
public class ConnectiveNodeView extends BhNodeView implements ImitationCreator{

	private final BhNodeViewGroup innerGroup = new BhNodeViewGroup(this, true); //!< ノード内部に描画されるノードのGroup
	private final BhNodeViewGroup outerGroup = new BhNodeViewGroup(this, false); //!< ノード外部に描画されるノードのGroup
	private Button imitCreateBtn;	//!< イミテーション作成ボタン
	private ConnectiveNode model;

	/**
	 * コンストラクタ
	 * @param model ビューが表すモデル
	 * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
	 * */
	public ConnectiveNodeView(ConnectiveNode model, BhNodeViewStyle viewStyle) {
		super(viewStyle, model);
		this.model = model;
	}

	/**
	 * 初期化する
	 */
	public boolean init() {

		initialize();
		innerGroup.buildSubGroup(viewStyle.connective.inner);
		outerGroup.buildSubGroup(viewStyle.connective.outer);
		setFuncs(this::rearrangeChildNodes, this::updateAbsPos);
		getAppearanceManager().addCssClass(BhParams.CSS.CLASS_CONNECTIVE_NODE);

		boolean success = true;
		if (model.canCreateImitManually) {
			Optional<Button> btnOpt = loadButton(BhParams.Path.IMIT_BUTTON_FXML, viewStyle.imitation);
			success &= btnOpt.isPresent();
			imitCreateBtn = btnOpt.orElse(new Button());
			getTreeManager().addChild(imitCreateBtn);
		}
		return success;
	}

	/**
	 * このビューのモデルであるBhNodeを取得する
	 * @return このビューのモデルであるBhNode
	 */
	@Override
	public ConnectiveNode getModel() {
		return model;
	}

	/**
	 * ノード内部に描画されるノードをリストの最後に追加する
	 * @param view ノード内部に描画されるノード
	 * */
	public void addToGroup(BhNodeView view) {

		// innerGroup に追加できなかったらouterGroupに入れる
		if (!innerGroup.addNodeView(view))
			outerGroup.addNodeView(view);
	}

	/**
	 * このノードの絶対位置を更新する関数
	 * @param posX 本体部分左上のX位置
	 * @param posY 本体部分左上のY位置
	 * */
	private void updateAbsPos(double posX, double posY) {

		//内部ノード絶対位置更新
		Vec2D relativePos = innerGroup.getRelativePosFromParent();
		innerGroup.updateAbsPos(posX + relativePos.x, posY + relativePos.y);

		//外部ノード絶対位置更新
		Vec2D bodySize = getRegionManager().getBodySize(false);
		if (viewStyle.connectorPos == CNCTR_POS.LEFT)	//外部ノードが右に繋がる
			outerGroup.updateAbsPos(posX + bodySize.x, posY);
		else										   //外部ノードが下に繋がる
			outerGroup.updateAbsPos(posX, posY + bodySize.y);
	}

	/**
	 * 子ノードの配置を更新する
	 * @param child 大きさや位置が変わった子ノードを含むグループ
	 * */
	private void rearrangeChildNodes(BhNodeViewGroup child) {

		Vec2D sizeBefore = getRegionManager().getNodeSizeIncludingOuter(true);
		if (child == null) {
			Vec2D outerSize = outerGroup.getSize();
			viewStyle.connective.outerWidth = outerSize.x;
			viewStyle.connective.outerHeight = outerSize.y;
			Vec2D innerSize = innerGroup.getSize();
			viewStyle.width = innerSize.x;
			viewStyle.height = innerSize.y;
			getAppearanceManager().updatePolygonShape();
		}
		else {
			Vec2D childSize = child.getSize();
			if (child == outerGroup) {
				viewStyle.connective.outerWidth = childSize.x;
				viewStyle.connective.outerHeight = childSize.y;
			}
			else if (child == innerGroup){
				viewStyle.width = childSize.x;
				viewStyle.height = childSize.y;
				getAppearanceManager().updatePolygonShape();
			}
		}

		updateChildRelativePos();

		Vec2D sizeAfter = getRegionManager().getNodeSizeIncludingOuter(true);
		if (parent.get() == null ||
			(sizeBefore.equals(sizeAfter) && child != null)) {
			Vec2D pos = getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			getPositionManager().setPosOnWorkspace(pos.x, pos.y);
			return;
		}

		parent.get().rearrangeChild();
	}

	/**
	 * ノードの親からの相対位置を指定する
	 * @param innerSizeList 内部描画ノードの大きさが格納された配列
	 * */
	private void updateChildRelativePos() {

		innerGroup.setRelativePosFromParent(viewStyle.paddingLeft, viewStyle.paddingTop);
		Vec2D bodySize = getRegionManager().getBodySize(false);
		if (viewStyle.connectorPos == CNCTR_POS.LEFT)	//外部ノードが右に繋がる
			outerGroup.setRelativePosFromParent(bodySize.x, 0.0);
		else										   //外部ノードが下に繋がる
			outerGroup.setRelativePosFromParent(0.0, bodySize.y);
	}

	/**
	 * visitor を内部ノードを管理するグループに渡す
	 * @param visitor 内部ノードを管理するグループに渡す visitor
	 * */
	public void sendToInnerGroup(NodeViewProcessor visitor) {
		innerGroup.accept(visitor);
	}

	/**
	 * visitor を外部ノードを管理するグループに渡す
	 * @param visitor 外部ノードを管理するグループに渡す visitor
	 * */
	public void sendToOuterGroup(NodeViewProcessor visitor) {
		outerGroup.accept(visitor);
	}

	@Override
	public void accept(NodeViewProcessor visitor) {
		visitor.visit(this);
	}

	@Override
	public void show(int depth) {

		try {
			MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<ConnectiveNodeView" + ">   " + this.hashCode());
			innerGroup.show(depth + 1);
			outerGroup.show(depth + 1);
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.msgForDebug("connectiveNodeView show exception " + e);
		}
	}

	@Override
	public Button imitCreateButton() {
		return imitCreateBtn;
	}
}











