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
 * V 字型コネクタクラス.
 *
 * @author K.Koike
 */
public class ConnectorCharV extends ConnectorShape {

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
    double t = 1.0 / 2.5;
    ArrayList<Double> vertices = null;
    if (pos == ConnectorPos.LEFT) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width,             offsetY + height / 2.0,
        offsetX + (1 - t) * width,   offsetY + height,
        offsetX,                     offsetY + height,
        offsetX + width * t,         offsetY + height / 2.0,
        offsetX,                     offsetY,
        offsetX + (1 - t) * width,   offsetY,
        offsetX + width,             offsetY + height / 2.0));
    } else if (pos == ConnectorPos.TOP) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + 0.0,          offsetY + height,
        offsetX + 0.0,          offsetY + 0.0,
        offsetX + width / 2.0,  offsetY + height,
        offsetX + width,        offsetY + 0.0,
        offsetX + width,        offsetY + height));
    }
    return vertices;
  }
}
