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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javafx.scene.Group;
import net.seapanda.bunnyhop.common.Showable;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.connective.Connector;

/**
 * BhNodeView の集合を持つクラス
 * @author K.Koike
 * */
public class BhNodeViewGroup extends Group implements Showable {

	private final List<BhNodeViewGroup> subGroupList = new ArrayList<>();	//!< group が子に持つ BhNodeView のリスト
	private ConnectiveNodeView parentView;	//!<このグループを持つConnectiveNode
	private BhNodeViewGroup parentGroup;	//!< このグループを持つBhNodeViewGroup
	public final boolean inner;	//!< このグループが内部描画ノードを持つグループの場合true
	private BhNodeViewStyle.Arrangement arrangeParams; //!< ノード配置パラメータ
	private final Map<String, BhNodeView> cnctrName_NodeView = new HashMap<>();	//!< コネクタ名とそのコネクタにつながるBhNodeView
	private final Vec2D size = new Vec2D(0.0, 0.0);

	/**
	 * コンストラクタ
	 * @param parentView このグループを持つConnectiveNode
	 * @param inner このグループが外部描画ノードを持つグループの場合true
	 * */
	public BhNodeViewGroup(ConnectiveNodeView parentView, boolean inner) {
		this.parentView = parentView;
		this.inner = inner;
	}

	/**
	 * コンストラクタ
	 * @param parentGroup このグループを持つBhNodeViewGroup
	 * @param inner このグループが内部描画ノードを持つグループの場合true
	 */
	public BhNodeViewGroup(BhNodeViewGroup parentGroup, boolean inner) {
		this.parentGroup = parentGroup;
		this.inner = inner;
	}

	/**
	 * このノード以下のサブグループを作成する
	 * @param arrangeParams ノード配置パラメータ
	 */
	public void buildSubGroup(BhNodeViewStyle.Arrangement arrangeParams) {

		this.arrangeParams = arrangeParams;
		arrangeParams.cnctrNameList.forEach(cnctrName -> cnctrName_NodeView.put(cnctrName, null));
		for (String cnctrName : arrangeParams.cnctrNameList) {
			cnctrName_NodeView.put(cnctrName, null);
			if (!inner)	//外部ノードをつなぐコネクタは1つだけとする
				return;
		}

		arrangeParams.subGroup.forEach(subGroupParams -> {
			BhNodeViewGroup subGroup = new BhNodeViewGroup(this, inner);
			subGroup.buildSubGroup(subGroupParams);
			subGroupList.add(subGroup);
			getChildren().add(subGroup);
		});
	}

	/**
	 * BhNodeView を追加する
	 * @param view 追加するノードビュー
	 * @return
	 * */
	public boolean addNodeView(BhNodeView view) {

		Connector cnctr = view.getModel().getParentConnector();
		if (cnctr != null) {
			String cnctrName = cnctr.getSymbolName();
			if (cnctrName_NodeView.containsKey(cnctrName)) {	// このグループ内に追加すべき場所が見つかった
				cnctrName_NodeView.put(cnctrName, view);
				getChildren().add(view);
				view.getTreeManager().setParentGroup(this);
				cnctr.setOuterFlag(!inner);
				return true;
			}
			else {	//サブグループに追加する
				for (BhNodeViewGroup subGroup : subGroupList) {
					if (subGroup.addNodeView(view))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * このグループが持つoldNodeView をnewNodeViewと入れ替える. <br>
	 * ただし, 古いノードのGUIツリーからの削除は行わない
	 * @param oldNodeView 入れ替えられる古いノード
	 * @param newNodeView 入れ替える新しいノード
	 */
	public void replace(BhNodeView oldNodeView, BhNodeView newNodeView) {

		for (Entry<String, BhNodeView> entrySet : cnctrName_NodeView.entrySet()) {
			if (entrySet.getValue().equals(oldNodeView)) {
				entrySet.setValue(newNodeView);
				getChildren().remove(newNodeView);
				getChildren().add(0, newNodeView);
				newNodeView.getTreeManager().setParentGroup(this);	//親をセット
				oldNodeView.getTreeManager().setParentGroup(null);	//親を削除
				return;
			}
		}
	}

	/**
	 * このグループの親ノードを返す
	 * @return このグループの親ノード
	 * */
	public ConnectiveNodeView getParentView() {

		if (parentView != null)
			return parentView;

		return parentGroup.getParentView();
	}

	/**
	 * 親ノードまたはグループからの相対位置を取得する
	 * @return 親ノードまたは親グループからの相対位置
	 */
	public Vec2D getRelativePosFromParent() {
		return new Vec2D(getTranslateX(), getTranslateY());
	}

	/**
	 * 親ノードまたはグループからの相対位置をセットする
	 * @param posX 親ノードまたは親グループからのX相対位置
	 * @param posY 親ノードまたは親グループからのY相対位置
	 */
	public void setRelativePosFromParent(double posX, double posY) {
		setTranslateX(posX);
		setTranslateY(posY);
	}

	public Vec2D getSize() {
		return size;
	}

	/**
	 * このグループの絶対位置を更新する関数
	 * @param posX グループ左上のX絶対位置
	 * @param posY グループ左上のY絶対位置
	 * */
	public void updateAbsPos(double posX, double posY) {

		arrangeParams.cnctrNameList.forEach(cnctrName -> {
			BhNodeView childNodeView = cnctrName_NodeView.get(cnctrName);
			if (childNodeView != null) {
				Vec2D relativePos = childNodeView.getPositionManager().getRelativePosFromParent();
				childNodeView.getPositionManager().updateAbsPos(posX + relativePos.x, posY + relativePos.y);
			}
		});

		subGroupList.forEach(subGroup -> {
			Vec2D subGroupRelPos = subGroup.getRelativePosFromParent();
			subGroup.updateAbsPos(posX + subGroupRelPos.x, posY + subGroupRelPos.y);});
	}

	/**
	 * 子要素を並べ直す. <br>
	 * 子ノードの形状が変わったことを, 親ConnectiveNodeViewもしくは, 親BhNodeViewGroupに伝える.
	 * */
	public void rearrangeChild() {

		Vec2D offset = calcOffset();
		Vec2D relPos = new Vec2D(offset.x, offset.y);
		Vec2D sizeBefor = new Vec2D(size.x, size.y);
		Vec2D childMaxLen = new Vec2D(0.0, 0.0);
		Vec2D childSumLen = new Vec2D(0.0, 0.0);
		size.x = 0.0;
		size.y = 0.0;

		arrangeParams.cnctrNameList.forEach(cnctrName -> {
			BhNodeView childNodeView = cnctrName_NodeView.get(cnctrName);
			if (childNodeView != null) {
				//outer はコネクタの大きさを考慮しない
				Vec2D cnctrSize = inner ? childNodeView.getConnectorManager().getConnectorSize() : new Vec2D(0.0,  0.0);
				Vec2D childNodeSize = childNodeView.getRegionManager().getNodeSizeIncludingOuter(false);
				//コネクタが上に付く
				if (childNodeView.getConnectorManager().getConnectorPos() == BhNodeViewStyle.CNCTR_POS.TOP) {
					childSumLen.add(childNodeSize.x, childNodeSize.y + cnctrSize.y);
					childMaxLen.updateIfGreter(childNodeSize.x, childNodeSize.y + cnctrSize.y);

					//グループの中が縦並び
					if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.COLUMN)
						relPos.add(0, cnctrSize.y);
				}
				//コネクタが左に付く
				else if (childNodeView.getConnectorManager().getConnectorPos() == BhNodeViewStyle.CNCTR_POS.LEFT) {
					childSumLen.add(childNodeSize.x + cnctrSize.x, childNodeSize.y);
					childMaxLen.updateIfGreter(childNodeSize.x + cnctrSize.x, childNodeSize.y);

					//グループの中が横並び
					if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.ROW)
						relPos.add(cnctrSize.x, 0);
				}
				childNodeView.getPositionManager().setRelativePosFromParent(relPos.x, relPos.y);
				updateChildRelativePos(relPos, childNodeSize);
			}
		});

		subGroupList.forEach(subGroup -> {
			subGroup.setRelativePosFromParent(relPos.x, relPos.y);
			Vec2D subGroupSize = subGroup.getSize();
			updateChildRelativePos(relPos, subGroupSize);
			childMaxLen.updateIfGreter(subGroupSize);
			childSumLen.add(subGroupSize);
		});

		//サイズ更新
		updateGroupSize(childMaxLen, childSumLen);

		if (sizeBefor.equals(size)) {
			Vec2D posOnWS = BhNodeView.getRelativePos(null, this);
			updateAbsPos(posOnWS.x, posOnWS.y);
			return;
		}

		if (parentView != null)
			parentView.getAppearanceManager().updateAppearance(this);
		else
			parentGroup.rearrangeChild();
	}

	/**
	 * グループのサイズを更新する
	 * @param childMaxSize 子ノードの中の最大の長さ
	 * @param childSumSize 全子ノードの合計の長さ
	 * */
	private void updateGroupSize(Vec2D childMaxLen, Vec2D childSumLen) {

		int numSpace = arrangeParams.cnctrNameList.size() + subGroupList.size() - 1;
		//グループの中が縦並び
		if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.COLUMN) {
			size.x = arrangeParams.paddingLeft + childMaxLen.x + arrangeParams.paddingRight;
			size.y = arrangeParams.paddingTop + childSumLen.y + numSpace * arrangeParams.space + arrangeParams.paddingBottom;
		}
		//グループの中が横並び
		else {
			size.x = arrangeParams.paddingLeft + childSumLen.x + numSpace * arrangeParams.space + arrangeParams.paddingRight;
			size.y = arrangeParams.paddingTop + childMaxLen.y + arrangeParams.paddingBottom;
		}
	}

	/**
	 * 子ノードとサブグループのこのノードからの相対位置を更新する
	 * @param relPos 更新する相対位置
	 * @param childSize 子ノードまたはサブグループの大きさ
	 */
	private void updateChildRelativePos(Vec2D relPos, Vec2D childSize) {

		//グループの中が縦並び
		if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.COLUMN) {
			relPos.y += childSize.y + arrangeParams.space;
		}
		//グループの中が横並び
		else {
			relPos.x += childSize.x + arrangeParams.space;
		}
	}

	/**
	 * 子ノードとサブグループを並べる際のオフセットを計算する
	 */
	private Vec2D calcOffset() {

		double offsetX = 0.0;
		double offsetY = 0.0;

		// 外部ノードはコネクタの大きさを考慮しない
		if (!inner)
			return new Vec2D(0.0, 0.0);

		for (String cnctrName : arrangeParams.cnctrNameList) {

			BhNodeView childNodeView = cnctrName_NodeView.get(cnctrName);
			if (childNodeView == null)
				continue;

			Vec2D cnctrSize = childNodeView.getConnectorManager().getConnectorSize();
			if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.COLUMN &&
				childNodeView.getConnectorManager().getConnectorPos() == BhNodeViewStyle.CNCTR_POS.LEFT) {	//グループの中が縦並びでかつコネクタが左に付く
				offsetX = Math.max(offsetX, cnctrSize.x);
			}
			else if (arrangeParams.arrangement == BhNodeViewStyle.CHILD_ARRANGEMENT.ROW &&
				 childNodeView.getConnectorManager().getConnectorPos() == BhNodeViewStyle.CNCTR_POS.TOP){	//グループの中が横並びでかつコネクタが上に付く
				offsetY = Math.max(offsetY, cnctrSize.y);
			}
		}
		return new Vec2D(offsetX + arrangeParams.paddingLeft, offsetY + arrangeParams.paddingTop);
	}

	/**
	 * BhNodeView を引数にとる関数オブジェクトを子ノードに渡す<br>
	 * @param visitorFunc BhNodeView を引数にとり処理するオブジェクト
	 * */
	public void accept(Consumer<BhNodeView> visitorFunc) {

		arrangeParams.cnctrNameList.forEach(cnctrName -> {
			BhNodeView childNodeView =  cnctrName_NodeView.get(cnctrName);
			if (childNodeView != null)
				childNodeView.accept(visitorFunc);
		});
		subGroupList.forEach(subGroup -> subGroup.accept(visitorFunc));
	}

	public void toForeGround() {

		toFront();
		if (parentGroup != null)
			parentGroup.toForeGround();

		if (parentView != null)
			parentView.getAppearanceManager().toForeGround();
	}

	@Override
	public void show(int depth) {

		try {
			MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<BhNodeViewGroup>  "  + this.hashCode());
			MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + (inner ? "<inner>" : "<outer>"));
			arrangeParams.cnctrNameList.forEach(cnctrName -> {
				BhNodeView childNodeView =  cnctrName_NodeView.get(cnctrName);
				if (childNodeView != null)
					childNodeView.show(depth + 1);
			});
			subGroupList.forEach(subGroup -> subGroup.show(depth + 1));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.msgForDebug("connectiveNodeView show exception " + e);
		}
	}
}






