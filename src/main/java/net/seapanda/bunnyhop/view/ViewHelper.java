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

import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Vec2D;

/**
 * Viewの処理を補助する機能を定義したクラス
 * @author K.Koike
 * */
public class ViewHelper {

	public static final ViewHelper INSTANCE = new ViewHelper();		//!< シングルトンインスタンス

	/**
	 * 移動後のフィールド上の位置を算出する
	 * @param diff 移動量
	 * @param fieldSize フィールドサイズ
	 * @param curePos 現在のフィールド上の位置
	 * @return 移動後の新しい位置
	 */
	public Vec2D newPosition(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {

		double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
		double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
		return new Vec2D(curPos.x + newDiffX, curPos.y + newDiffY);
	}

	/**
	 * 移動時のフィールド上の移動距離を算出する
	 * @param diff 移動量
	 * @param fieldSize フィールドサイズ
	 * @param curePos 現在のフィールド上の位置
	 * @return 移動距離
	 */
	public Vec2D distance(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {

		double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
		double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
		return new Vec2D(newDiffX, newDiffY);
	}

	/**
	 * ワークスペースの範囲を元に新しい移動量を算出する
	 * @param targetRange 新しい位置を計算する際に使う範囲
	 * @param curPos 現在のWS上での位置
	 * @param diff 移動量
	 * @return 新しい移動量
	 */
	private double calcNewDiff(double targetRange, double curPos, double diff) {

		boolean curPosIsInTargetRange = (0 < curPos) && (curPos < targetRange);
		if (curPosIsInTargetRange) {
			double newPos = curPos + diff;
			boolean newPosIsInTargetRange = (0 < newPos) && (newPos < targetRange);
			if (!newPosIsInTargetRange) {	//現在範囲内に居て移動後に範囲外に居る場合, 移動させない
				if (newPos < 0)
					return -curPos + 1.0;
				else
					return targetRange - curPos - 1.0;
			}
		}
		return diff;
	}

	/**
	 * 親子関係にある2つのノードの相対距離を測る
	 * @param base 基点となるNodeオブジェクト<br> null を入れると target が居るワークスペースからの相対距離が得られる
	 * @param target 基点からの距離を測るオブジェクト
	 * @return target - base で算出される距離
	 * */
	public Vec2D getRelativePos(Node base, Node target) {

		Vec2D relativePos = new Vec2D(0.0, 0.0);
		Node parent = target;
		while (parent != base && !BhParams.Fxml.ID_WS_PANE.equals(parent.getId())) {
			relativePos.x += parent.getTranslateX();
			relativePos.y += parent.getTranslateY();
			parent = parent.getParent();

			if (parent == null)
				break;
		}
		return relativePos;
	}

	/**
	 * node のワークスペース上での位置を取得する
	 * @param node ワークペース上での位置を計算するノード
	 * @return node のワークスペース上での位置.
	 * */
	public Vec2D getPosOnWorkspace(Node node) {
		return getRelativePos(null, node);
	}

	/**
	 * 文字列を表示したときのサイズを計算する
	 * @param str サイズを計算する文字列
	 * @param font フォント
	 * @param boundType 境界算出方法
	 * @param lineSpacing 行間
	 * */
	public Vec2D calcStrBounds(
		String str,
		Font font,
		TextBoundsType boundType,
		double lineSpacing) {

		Text text = new Text(str);
		text.setFont(font);
		text.setBoundsType(boundType);
		text.setLineSpacing(lineSpacing);
		return new Vec2D(text.getBoundsInLocal().getWidth(), text.getBoundsInLocal().getHeight());
	}

	/**
	 * 引数で指定した文字列の表示幅を計算する
	 * @param str 表示幅を計算する文字列
	 * @param font 表示時のフォント
	 * @return 文字列を表示したときの幅
	 */
	public double calcStrWidth(String str, Font font) {
        Text text = new Text(str);
        text.setFont(font);
		return text.getBoundsInLocal().getWidth();
	}
}
















