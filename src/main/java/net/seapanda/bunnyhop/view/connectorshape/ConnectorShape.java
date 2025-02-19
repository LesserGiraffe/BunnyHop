/*
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
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;

/**
 * コネクタ描画クラスの基底クラス.
 *
 * @author K.Koike
 */
public abstract class ConnectorShape {

  /** コネクタの頂点を算出する.
   *
   * @param offsetX 頂点に加算するオフセットX
   * @param offsetY 頂点に加算するオフセットY
   * @param width   コネクタの幅
   * @param height  コネクタの高さ
   * @param pos     コネクタの位置 (Top or Left)
   * @return コネクタを形成する点群
   * */
  public abstract List<Double> createVertices(
      double offsetX, double offsetY, double width, double height, ConnectorPos pos);

  /** コネクタの形の識別子を定義した列挙型. */
  public enum CnctrShape {
    ARROW("ARROW", new ConnectorArrow()),
    CHAR_T("CHAR_T", new ConnectorCharT()),
    CHAR_U("CHAR_U", new ConnectorCharU()),
    CHAR_V("CHAR_V", new ConnectorCharV()),
    CROSS("CROSS", new ConnectorCross()),
    DIAMOND("DIAMOND", new ConnectorDiamond()),
    HEXAGON("HEXAGON", new ConnectorHexagon()),
    INV_PENTAGON("INV_PENTAGON", new ConnectorInvPentagon()),
    INV_TRAPEZOID("INV_TRAPEZOID", new ConnectorInvTrapezoid()),
    INV_TRIANGLE("INV_TRIANGLE", new ConnectorInvTriangle()),
    NONE("NONE", new ConnectorNone()),
    OCTAGON("OCTAGON", new ConnectorOctagon()),
    PENTAGON("PENTAGON", new ConnectorPentagon()),
    SQUARE("SQUARE", new ConnectorSquare()),
    STAR("STAR", new ConnectorStar()),
    TRAPEZOID("TRAPEZOID", new ConnectorTrapezoid()),
    TRIANGLE("TRIANGLE", new ConnectorTriangle()),
    LIGHTNING("LIGHTNING", new ConnectorLightning()),
    SANDGLASS("SANDGLASS", new ConnectorSandglass());

    public final String name;
    public final ConnectorShape shape;
    private static final Map<String, CnctrShape> shapeNameToConnectorShape =
        new HashMap<>() {{
            put(CnctrShape.ARROW.name, CnctrShape.ARROW);
            put(CnctrShape.CHAR_T.name, CnctrShape.CHAR_T);
            put(CnctrShape.CHAR_U.name, CnctrShape.CHAR_U);
            put(CnctrShape.CHAR_V.name, CnctrShape.CHAR_V);
            put(CnctrShape.CROSS.name, CnctrShape.CROSS);
            put(CnctrShape.DIAMOND.name, CnctrShape.DIAMOND);
            put(CnctrShape.HEXAGON.name, CnctrShape.HEXAGON);
            put(CnctrShape.INV_PENTAGON.name, CnctrShape.INV_PENTAGON);
            put(CnctrShape.INV_TRAPEZOID.name, CnctrShape.INV_TRAPEZOID);
            put(CnctrShape.INV_TRIANGLE.name, CnctrShape.INV_TRIANGLE);
            put(CnctrShape.NONE.name, CnctrShape.NONE);
            put(CnctrShape.OCTAGON.name, CnctrShape.OCTAGON);
            put(CnctrShape.PENTAGON.name, CnctrShape.PENTAGON);
            put(CnctrShape.SQUARE.name, CnctrShape.SQUARE);
            put(CnctrShape.STAR.name, CnctrShape.STAR);
            put(CnctrShape.TRAPEZOID.name, CnctrShape.TRAPEZOID);
            put(CnctrShape.TRIANGLE.name,  CnctrShape.TRIANGLE);
            put(CnctrShape.LIGHTNING.name,  CnctrShape.LIGHTNING);
            put(CnctrShape.SANDGLASS.name,  CnctrShape.SANDGLASS);
          }};

    private CnctrShape(String shapeName, ConnectorShape shape) {
      name = shapeName;
      this.shape = shape;
    }

    /** コネクタ名からコネクタの識別子を取得する. */
    public static CnctrShape getByName(String name) {
      return CnctrShape.shapeNameToConnectorShape.get(name);
    }
  }

  /**
   * コネクタ名から対応する CNCTR_SHAPE を返す.
   *
   * @param cnctrShapeName コネクタの形を表す文字列
   * @param fileName コネクタの形が記述してあるjsonファイルの名前 (nullable)
   * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
   */
  public static CnctrShape getConnectorTypeFromName(String cnctrShapeName, String fileName) {
    CnctrShape type = CnctrShape.getByName(cnctrShapeName);
    if (type == null) {
      throw new IllegalArgumentException(
          "Invalid connector shape name %s  (%s)".formatted(cnctrShapeName, fileName));
    }
    return type;
  }
}
