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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;

/**
 * 六角形コネクタクラス.
 *
 * @author K.Koike
 */
public class ConnectorHexagon extends ConnectorShape {

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
    double p = 4.0;
    double q = 1.0;

    if (pos == ConnectorPos.LEFT) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width,       offsetY + height * (1.0 - q / p),
        offsetX + width / 2.0, offsetY + height,
        offsetX + 0.0,         offsetY + height * (1.0 - q / p),
        offsetX + 0.0,         offsetY + height * (q / p),
        offsetX + width / 2.0, offsetY + 0.0,
        offsetX + width,       offsetY + height * (q / p)));
    } else if (pos == ConnectorPos.TOP) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width * (q / p),       offsetY + height,
        offsetX + 0.0, offsetY + height / 2.0,
        offsetX + width * (q / p),         offsetY,
        offsetX + width * (1.0 - q / p),         offsetY + 0.0,
        offsetX + width, offsetY + height / 2.0,
        offsetX + width * (1.0 - q / p),       offsetY + height));
    }
    return vertices;
  }
}
