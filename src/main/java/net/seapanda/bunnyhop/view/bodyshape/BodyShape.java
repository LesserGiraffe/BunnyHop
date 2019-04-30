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
package net.seapanda.bunnyhop.view.bodyshape;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle.NOTCH_POS;

public abstract class BodyShape {

	/**
	 * ボディの形の識別子を定義した列挙型
	 * */
	public enum BODY_SHAPE {

		BODY_SHAPE_ROUND_RECT ("ROUND_RECT", new BodyRoundRect()),
		BODY_SHAPE_NONE ("NONE", new BodyNone());

		public final String NAME;
		public final BodyShape SHAPE;
		private static final Map<String, BODY_SHAPE> shapeNameToBodyShape =
			new HashMap<>() {{
				put(BODY_SHAPE.BODY_SHAPE_ROUND_RECT.NAME, BODY_SHAPE.BODY_SHAPE_ROUND_RECT);
				put(BODY_SHAPE.BODY_SHAPE_NONE.NAME, BODY_SHAPE.BODY_SHAPE_NONE);
			}};

		private BODY_SHAPE(String shapeName, BodyShape shape) {
			NAME = shapeName;
			SHAPE = shape;
		}

		/**
		 * ボディ名からボディの識別子を取得する
		 * */
		public static BODY_SHAPE getByName(String name) {
			return BODY_SHAPE.shapeNameToBodyShape.get(name);
		}
	}

	/**
	 * ノードを形作る頂点を生成する
	 * @param bodyWidth ボディの幅
	 * @param bodyHeight ボディの高さ
	 * @param connector 描画するコネクタ
	 * @param cnctrPos コネクタの位置 (Left, Top)
	 * @param cnctrWidth コネクタの幅
	 * @param cnctrHeight コネクタの高さ
	 * @param cnctrShift ノードの左上からのコネクタの位置
	 * @param notch 描画する切り欠き
	 * @param notchPos 切り欠きの位置 (Right, Bottom)
	 * @param notchWidth 切り欠きの幅
	 * @param notchHeigth 切り欠きの高さ
	 * @return ノードを形成する頂点のリスト
	 * */
	abstract public Collection<Double> createVertices(
		double bodyWidth,
		double bodyHeight,
		ConnectorShape connector,
		BhNodeViewStyle.CNCTR_POS cnctrPos,
		double cnctrWidth,
		double cnctrHeight,
		double cnctrShift,
		ConnectorShape notch,
		BhNodeViewStyle.NOTCH_POS notchPos,
		double notchWidth,
		double notchHeight);

	/**
	 * コネクタ部分の頂点を作成する
	 * @param connector 描画するコネクタオブジェクト
	 * @param cnctrPos コネクタの位置 (Top, Left)
	 * @param cnctrWidth コネクタの幅
	 * @param cnctrHeight コネクタの高さ
	 * @param cnctrShift ノードの左上からのコネクタの位置
	 * @return コネクタ部分の頂点リスト
	 * */
	protected List<Double> createConnectorVertices(
		ConnectorShape connector,
		BhNodeViewStyle.CNCTR_POS cnctrPos,
		double cnctrWidth,
		double cnctrHeight,
		double cnctrShift) {

		double offsetX = 0.0;	// 頂点に加算するオフセットX
		double offsetY = 0.0;	// 頂点に加算するオフセットY

		if (cnctrPos == CNCTR_POS.LEFT) {
			offsetX = -cnctrWidth;
			offsetY = cnctrShift;
		}
		else if (cnctrPos == CNCTR_POS.TOP) {
			offsetX = cnctrShift;
			offsetY = -cnctrHeight;
		}
		return connector.createVertices(offsetX, offsetY, cnctrWidth, cnctrHeight, cnctrPos);
	}

	/**
	 * 切り欠き部分の頂点を作成する
	 * @param notch 描画する切り欠きオブジェクト
	 * @param cnctrPos コネクタの位置 (Top, Left)
	 * @param cnctrWidth コネクタの幅
	 * @param cnctrHeight コネクタの高さ
	 * @param cnctrShift ノードの左上からのコネクタの位置
	 * @param コネクタ部分の頂点リスト
	 * */
	protected List<Double> createNotchVertices(
			ConnectorShape notch,
			BhNodeViewStyle.NOTCH_POS notchPos,
			double notchWidth,
			double notchHeight,
			double bodyWidth,
			double bodyHeight) {

		double offsetX = 0.0;
		double offsetY = 0.0;
		BhNodeViewStyle.CNCTR_POS cnctrPos = CNCTR_POS.LEFT;

		if (notchPos == NOTCH_POS.RIGHT) {
			offsetX = bodyWidth - notchWidth;
			offsetY = (bodyHeight - notchHeight) * 0.5;
			cnctrPos = CNCTR_POS.LEFT;
		}
		else if (notchPos == NOTCH_POS.BOTTOM) {
			offsetX = (bodyWidth - notchWidth) * 0.5;
			offsetY = bodyHeight - notchHeight;
			cnctrPos = CNCTR_POS.TOP;
		}

		List<Double> vertices = notch.createVertices(offsetX, offsetY, notchWidth, notchHeight, cnctrPos);
		Collections.reverse(vertices);
		for (int i = 0; i < vertices.size(); i += 2) {
			double tmp = vertices.get(i);
			vertices.set(i, vertices.get(i + 1));
			vertices.set(i + 1, tmp);
		}
		return vertices;
	}

	/**
	 * ボディ名から対応する BODY_SHAPE を返す
	 * @param bodyShapeName ボディの形を表す文字列
	 * @param fileName ボディの形が記述してあるjsonファイルの名前 (null可)
	 * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
	 * */
	public static BODY_SHAPE getBodyTypeFromName(String bodyShapeName, String fileName) {

		BODY_SHAPE type = BODY_SHAPE.getByName(bodyShapeName);
		if (type == null)
			throw new AssertionError(ConnectorShape.class.getSimpleName()
					+ "  invalid body shape name " + bodyShapeName + " (" + fileName + ")");

		return type;
	}
}











