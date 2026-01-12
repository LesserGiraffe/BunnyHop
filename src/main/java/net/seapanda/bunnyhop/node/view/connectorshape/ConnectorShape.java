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

package net.seapanda.bunnyhop.node.view.connectorshape;

import java.util.List;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;

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

  /**
   * コネクタ名から対応する CNCTR_SHAPE を返す.
   *
   * @param cnctrShapeName コネクタの形を表す文字列
   * @param fileName コネクタの形が記述してあるjsonファイルの名前 (nullable)
   * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
   */
  public static ConnectorShapeType getConnectorTypeFromName(String cnctrShapeName, String fileName) {
    ConnectorShapeType type = ConnectorShapeType.getByName(cnctrShapeName);
    if (type == null) {
      throw new IllegalArgumentException(
          "Invalid connector shape name %s  (%s)".formatted(cnctrShapeName, fileName));
    }
    return type;
  }
}
