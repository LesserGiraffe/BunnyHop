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

import net.seapanda.bunnyhop.common.Vec2D;

/**
 * 特定の領域内での移動先を計算するクラス
 * @author K.Koike
 * */
public class FieldPosCalculator {

	/**
	 * 移動後のフィールド上の位置を算出する
	 * @param diff 移動量
	 * @param fieldSize フィールドサイズ
	 * @param curePos 現在のフィールド上の位置
	 * @return 移動後の新しい位置
	 */
	public static Vec2D newPosition(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {

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
	public static Vec2D distance(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {

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
	private static double calcNewDiff(double targetRange, double curPos, double diff) {

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
}
