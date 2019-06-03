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
package net.seapanda.bunnyhop.view.connectorshape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;

/**
 * コネクタ描画クラスの基底クラス
 * @author K.Koike
 */
public abstract class ConnectorShape {

	/** コネクタの頂点を算出する
	 * @param offsetX 頂点に加算するオフセットX
	 * @param offsetY 頂点に加算するオフセットY
	 * @param width   コネクタの幅
	 * @param height  コネクタの高さ
	 * @param pos     コネクタの位置 (Top or Left)
	 * @return コネクタを形成する点群
	 * */
	public abstract List<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos);

	/**
	 * コネクタの形の識別子を定義した列挙型
	 * */
	public enum CNCTR_SHAPE {

		ARROW ("ARROW", new ConnectorArrow()),
		CHAR_T ("CHAR_T", new ConnectorCharT()),
		CHAR_U ("CHAR_U", new ConnectorCharU()),
		CHAR_V ("CHAR_V", new ConnectorCharV()),
		CROSS ("CROSS", new ConnectorCross()),
		DIAMOND ("DIAMOND", new ConnectorDiamond()),
		HEXAGON ("HEXAGON", new ConnectorHexagon()),
		INV_PENTAGON ("INV_PENTAGON", new ConnectorInvPentagon()),
		INV_TRAPEZOID ("INV_TRAPEZOID", new ConnectorInvTrapezoid()),
		INV_TRIANGLE ("INV_TRIANGLE", new ConnectorInvTriangle()),
		NONE ("NONE", new ConnectorNone()),
		OCTAGON ("OCTAGON", new ConnectorOctagon()),
		PENTAGON ("PENTAGON", new ConnectorPentagon()),
		SQUARE ("SQUARE", new ConnectorSquare()),
		STAR ("STAR", new ConnectorStar()),
		TRAPEZOID ("TRAPEZOID", new ConnectorTrapezoid()),
		TRIANGLE ("TRIANGLE", new ConnectorTriangle()),
		LIGHTNING ("LIGHTNING", new ConnectorLightning());

		public final String NAME;
		public final ConnectorShape SHAPE;
		private static final Map<String, CNCTR_SHAPE> shapeNameToConnectorShape =
			new HashMap<>() {{
				put(CNCTR_SHAPE.ARROW.NAME, CNCTR_SHAPE.ARROW);
				put(CNCTR_SHAPE.CHAR_T.NAME, CNCTR_SHAPE.CHAR_T);
				put(CNCTR_SHAPE.CHAR_U.NAME, CNCTR_SHAPE.CHAR_U);
				put(CNCTR_SHAPE.CHAR_V.NAME, CNCTR_SHAPE.CHAR_V);
				put(CNCTR_SHAPE.CROSS.NAME, CNCTR_SHAPE.CROSS);
				put(CNCTR_SHAPE.DIAMOND.NAME, CNCTR_SHAPE.DIAMOND);
				put(CNCTR_SHAPE.HEXAGON.NAME, CNCTR_SHAPE.HEXAGON);
				put(CNCTR_SHAPE.INV_PENTAGON.NAME, CNCTR_SHAPE.INV_PENTAGON);
				put(CNCTR_SHAPE.INV_TRAPEZOID.NAME, CNCTR_SHAPE.INV_TRAPEZOID);
				put(CNCTR_SHAPE.INV_TRIANGLE.NAME, CNCTR_SHAPE.INV_TRIANGLE);
				put(CNCTR_SHAPE.NONE.NAME, CNCTR_SHAPE.NONE);
				put(CNCTR_SHAPE.OCTAGON.NAME, CNCTR_SHAPE.OCTAGON);
				put(CNCTR_SHAPE.PENTAGON.NAME, CNCTR_SHAPE.PENTAGON);
				put(CNCTR_SHAPE.SQUARE.NAME, CNCTR_SHAPE.SQUARE);
				put(CNCTR_SHAPE.STAR.NAME, CNCTR_SHAPE.STAR);
				put(CNCTR_SHAPE.TRAPEZOID.NAME, CNCTR_SHAPE.TRAPEZOID);
				put(CNCTR_SHAPE.TRIANGLE.NAME,  CNCTR_SHAPE.TRIANGLE);
				put(CNCTR_SHAPE.LIGHTNING.NAME,  CNCTR_SHAPE.LIGHTNING);
			}};

		private CNCTR_SHAPE(String shapeName, ConnectorShape shape) {
			NAME = shapeName;
			SHAPE = shape;
		}

		/**
		 * コネクタ名からコネクタの識別子を取得する
		 * */
		public static CNCTR_SHAPE getByName(String name) {
			return CNCTR_SHAPE.shapeNameToConnectorShape.get(name);
		}
	}

	/**
	 * コネクタ名から対応する CNCTR_SHAPE を返す
	 * @param cnctrShapeName コネクタの形を表す文字列
	 * @param fileName コネクタの形が記述してあるjsonファイルの名前 (null可)
	 * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
	 * */
	public static CNCTR_SHAPE getConnectorTypeFromName(String cnctrShapeName, String fileName) {

		CNCTR_SHAPE type = CNCTR_SHAPE.getByName(cnctrShapeName);
		if (type == null)
			throw new AssertionError(ConnectorShape.class.getSimpleName()
					+ "  invalid connector shape name " + cnctrShapeName + " (" + fileName + ")");

		return type;
	}
}










