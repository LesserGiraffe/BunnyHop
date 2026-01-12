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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;

/**
 * 砂時計コネクタクラス.
 *
 * @author K.Koike
 */
public class ConnectorSandglass extends ConnectorShape {

  /** コネクタの頂点を算出する.
   *
   * @param offsetX 頂点に加算するオフセットX
   * @param offsetY 頂点に加算するオフセットY
   * @param width   コネクタの幅
   * @param height  コネクタの高さ
   */
  @Override
  public List<Double> createVertices(
      double offsetX, double offsetY, double width, double height, ConnectorPos pos) {
    ArrayList<Double> vertices = null;
    final double p = 0.3;
    final double q = 0.2;
    final double r = 0.25;
    final double s = 0.45;

    if (pos == ConnectorPos.LEFT) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width,             offsetY + height,
        offsetX + width * (1.0 - q), offsetY + height,
        offsetX + width * (1.0 - s), offsetY + height * (1.0 - p),
        offsetX + width * r,         offsetY + height,
        offsetX,                     offsetY + height,
        offsetX,                     offsetY,
        offsetX + width * r,         offsetY,
        offsetX + width * (1.0 - s), offsetY + height * p,
        offsetX + width * (1.0 - q), offsetY,
        offsetX + width,             offsetY));
    } else if (pos == ConnectorPos.TOP) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX,                     offsetY + height,
        offsetX,                     offsetY + height * (1.0 - q),
        offsetX + width * p,         offsetY + height * (1.0 - s),
        offsetX,                     offsetY + height * r,
        offsetX,                     offsetY,
        offsetX + width,             offsetY,
        offsetX + width,             offsetY + height * r,
        offsetX + width * (1.0 - p), offsetY + height * (1.0 - s),
        offsetX + width,             offsetY + height * (1.0 - q),
        offsetX + width,             offsetY + height));
    }
    return vertices;
  }
}
