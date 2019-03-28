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

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle.CNCTR_POS;

/**
 * コネクタ描画クラスの基底クラス
 * @author K.Koike
 */
public abstract class ConnectorShape {

	private static final Map<String, CNCTR_SHAPE> shapeNameToConnectorShape =
		new HashMap<String, CNCTR_SHAPE>() {{
			put(BhParams.NodeStyleDef.VAL_ARROW, CNCTR_SHAPE.CNCTR_SHAPE_ARROW);
			put(BhParams.NodeStyleDef.VAL_CHAR_T, CNCTR_SHAPE.CNCTR_SHAPE_CHAR_T);
			put(BhParams.NodeStyleDef.VAL_CHAR_U, CNCTR_SHAPE.CNCTR_SHAPE_CHAR_U);
			put(BhParams.NodeStyleDef.VAL_CHAR_V, CNCTR_SHAPE.CNCTR_SHAPE_CHAR_V);
			put(BhParams.NodeStyleDef.VAL_CROSS, CNCTR_SHAPE.CNCTR_SHAPE_CROSS);
			put(BhParams.NodeStyleDef.VAL_DIAMOND, CNCTR_SHAPE.CNCTR_SHAPE_DIAMOND);
			put(BhParams.NodeStyleDef.VAL_HEXAGON, CNCTR_SHAPE.CNCTR_SHAPE_HEXAGON);
			put(BhParams.NodeStyleDef.VAL_INV_PENTAGON, CNCTR_SHAPE.CNCTR_SHAPE_INV_PENTAGON);
			put(BhParams.NodeStyleDef.VAL_INV_TRAPEZOID, CNCTR_SHAPE.CNCTR_SHAPE_INV_TRAPEZOID);
			put(BhParams.NodeStyleDef.VAL_INV_TRIANGLE, CNCTR_SHAPE.CNCTR_SHAPE_INV_TRIANGLE);
			put(BhParams.NodeStyleDef.VAL_NONE, CNCTR_SHAPE.CNCTR_SHAPE_NONE);
			put(BhParams.NodeStyleDef.VAL_OCTAGON, CNCTR_SHAPE.CNCTR_SHAPE_OCTAGON);
			put(BhParams.NodeStyleDef.VAL_PENTAGON, CNCTR_SHAPE.CNCTR_SHAPE_PENTAGON);
			put(BhParams.NodeStyleDef.VAL_SQARE, CNCTR_SHAPE.CNCTR_SHAPE_SQUARE);
			put(BhParams.NodeStyleDef.VAL_TRAPEZOID, CNCTR_SHAPE.CNCTR_SHAPE_TRAPEZOID);
			put(BhParams.NodeStyleDef.VAL_TRIANGLE, CNCTR_SHAPE.CNCTR_SHAPE_TRIANGLE);
			put(BhParams.NodeStyleDef.VAL_STAR, CNCTR_SHAPE.CNCTR_SHAPE_STAR);
		}};

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
	 * type に対応する ConnecrtorShape を取得する
	 * @param type コネクタの形を表す列挙子
	 * @return ConnecrtorShape オブジェクト
	 * */
	public static ConnectorShape genConnector(CNCTR_SHAPE type){

		switch (type) {
			case CNCTR_SHAPE_ARROW : return new ConnectorArrow();
			case CNCTR_SHAPE_CHAR_T : return new ConnectorCharT();
			case CNCTR_SHAPE_CHAR_U : return new ConnectorCharU();
			case CNCTR_SHAPE_CHAR_V : return new ConnectorCharV();
			case CNCTR_SHAPE_CROSS : return new ConnectorCross();
			case CNCTR_SHAPE_DIAMOND : return new ConnectorDiamond();
			case CNCTR_SHAPE_HEXAGON : return new ConnectorHexagon();
			case CNCTR_SHAPE_INV_PENTAGON : return new ConnectorInvPentagon();
			case CNCTR_SHAPE_INV_TRAPEZOID : return new ConnectorInvTrapezoid();
			case CNCTR_SHAPE_INV_TRIANGLE : return new ConnectorInvTriangle();
			case CNCTR_SHAPE_NONE : return new ConnectorNone();
			case CNCTR_SHAPE_OCTAGON : return new ConnectorOctagon();
			case CNCTR_SHAPE_PENTAGON : return new ConnectorPentagon();
			case CNCTR_SHAPE_SQUARE : return new ConnectorSquare();
			case CNCTR_SHAPE_STAR : return new ConnectorStar();
			case CNCTR_SHAPE_TRAPEZOID : return new ConnectorTrapezoid();
			case CNCTR_SHAPE_TRIANGLE : return new ConnectorTriangle();
			default : throw new AssertionError(ConnectorShape.class.getSimpleName() + "  invalid connector shape " + type.toString());
		}
	}

	public enum CNCTR_SHAPE {
		CNCTR_SHAPE_ARROW,
		CNCTR_SHAPE_CHAR_T,
		CNCTR_SHAPE_CHAR_U,
		CNCTR_SHAPE_CHAR_V,
		CNCTR_SHAPE_CROSS,
		CNCTR_SHAPE_DIAMOND,
		CNCTR_SHAPE_HEXAGON,
		CNCTR_SHAPE_INV_PENTAGON,
		CNCTR_SHAPE_INV_TRAPEZOID,
		CNCTR_SHAPE_INV_TRIANGLE,
		CNCTR_SHAPE_NONE,
		CNCTR_SHAPE_OCTAGON,
		CNCTR_SHAPE_PENTAGON,
		CNCTR_SHAPE_SQUARE,
		CNCTR_SHAPE_STAR,
		CNCTR_SHAPE_TRAPEZOID,
		CNCTR_SHAPE_TRIANGLE,
	}

	/**
	 * コネクタ名から対応する CNCTR_SHAPE を返す
	 * @param cnctrShapeName コネクタの形を表す文字列
	 * @param fileName コネクタの形が記述してあるjsonファイルの名前 (null可)
	 * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
	 * */
	public static CNCTR_SHAPE getConnectorTypeFromName(String cnctrShapeName, String fileName) {

		CNCTR_SHAPE type = shapeNameToConnectorShape.get(cnctrShapeName);
		if (type == null)
			throw new AssertionError(ConnectorShape.class.getSimpleName()
					+ "  invalid connector shape name " + cnctrShapeName + " (" + fileName + ")");

		return type;
	}
}









