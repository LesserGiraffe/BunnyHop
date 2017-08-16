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
package pflab.bunnyhop.view.connectorShape;

import java.util.Collection;

import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.view.BhNodeViewStyle.CNCTR_POS;

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
	public abstract Collection<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos);

	/**
	 * CNCTR_SHAPE に対応する ConnecrtorShape を取得する
	 * @param type コネクタの形を表す列挙子
	 * @return ConnecrtorShape オブジェクト
	 * */
	public static ConnectorShape genConnector(CNCTR_SHAPE type){

		switch (type) {
			case CNCTR_SHAPE_ARROW :         return new ConnectorArrow();
			case CNCTR_SHAPE_CHAR_T :        return new ConnectorCharT();
			case CNCTR_SHAPE_CHAR_U :        return new ConnectorCharU();
			case CNCTR_SHAPE_CHAR_V :        return new ConnectorCharV();
			case CNCTR_SHAPE_CROSS :         return new ConnectorCross();
			case CNCTR_SHAPE_DIAMOND :       return new ConnectorDiamond();
			case CNCTR_SHAPE_HEXAGON :       return new ConnectorHexagon();
			case CNCTR_SHAPE_INV_TRAPEZOID : return new ConnectorInvTrapezoid();
			case CNCTR_SHAPE_INV_TRIANGLE :  return new ConnectorInvTriangle();
			case CNCTR_SHAPE_NONE :          return new ConnectorNone();
			case CNCTR_SHAPE_OCTAGON :       return new ConnectorOctagon();
			case CNCTR_SHAPE_PENTAGON :      return new ConnectorPentagon();
			case CNCTR_SHAPE_SQUARE :     return new ConnectorSquare();
			case CNCTR_SHAPE_TRAPEZOID :     return new ConnectorTrapezoid();
			case CNCTR_SHAPE_TRIANGLE :      return new ConnectorTriangle();
			default :                        return new ConnectorNone();
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
		CNCTR_SHAPE_INV_TRAPEZOID,
		CNCTR_SHAPE_INV_TRIANGLE,
		CNCTR_SHAPE_NONE,
		CNCTR_SHAPE_OCTAGON,
		CNCTR_SHAPE_PENTAGON,
		CNCTR_SHAPE_SQUARE,
		CNCTR_SHAPE_TRAPEZOID,
		CNCTR_SHAPE_TRIANGLE,
	}

	/**
	 * コネクタの形を表す文字列から 対応する CNCTR_SHAPE を返す
	 * @param shapeStr コネクタの形を表す文字列
	 * @param fileName コネクタの形が記述してあるjsonファイルの名前
	 * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
	 * */
	public static CNCTR_SHAPE stringToCNCTR_SHAPE(String shapeStr, String fileName) {

		CNCTR_SHAPE type = null;
		if (shapeStr.equals(BhParams.NodeStyleDef.valNameArrow)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_ARROW;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameCharT)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_CHAR_T;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameCharU)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_CHAR_U;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameCharV)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_CHAR_V;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameCross)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_CROSS;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameDiamond)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_DIAMOND;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameHexagon)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_HEXAGON;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameInvTrapezoid)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_INV_TRAPEZOID;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameInvTriangle)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_INV_TRIANGLE;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameNone)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_NONE;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameOctagon)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_OCTAGON;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNamePentagon)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_PENTAGON;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameSuare)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_SQUARE;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameTrapezoid)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_TRAPEZOID;
		}
		else if (shapeStr.equals(BhParams.NodeStyleDef.valNameTriangle)) {
			type = CNCTR_SHAPE.CNCTR_SHAPE_TRIANGLE;
		}
		else {
			type = CNCTR_SHAPE.CNCTR_SHAPE_NONE;
			MsgPrinter.instance.ErrMsgForDebug(BhParams.NodeStyleDef.keyNameConnectorShape + " " + shapeStr + " is invalid.\n" + "\"" + fileName + "\"");
		}

		return type;
	}
}










