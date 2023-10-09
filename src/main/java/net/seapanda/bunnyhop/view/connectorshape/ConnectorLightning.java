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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;

/**
 * 雷型コネクタ
 * @author K.Koike
 * */
public class ConnectorLightning extends ConnectorShape {


  /** コネクタの頂点を算出する
   * @param offsetX 頂点に加算するオフセットX
   * @param offsetY 頂点に加算するオフセットY
   * @param width   コネクタの幅
   * @param height  コネクタの高さ
   * */
  @Override
  public List<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos) {

    ArrayList<Double> vertices = null;
    double p = 2.0 / 3.0;
    double q = 1.0 - p;

    if (pos == CNCTR_POS.LEFT) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width,     offsetY + height * p,
        offsetX + width * p, offsetY + height * 0.5,
        offsetX + width * p, offsetY + height,
        offsetX + 0.0,       offsetY + height * p,
        offsetX + 0.0,       offsetY + height * q,
        offsetX + width * q, offsetY + height * 0.5,
        offsetX + width * q, offsetY + 0.0,
        offsetX + width,     offsetY + height * q));
    }
    else if (pos == CNCTR_POS.TOP) {
      vertices = new ArrayList<>(Arrays.asList(
        offsetX + width * q,   offsetY + height,
        offsetX + width * 0.5, offsetY + height * p,
        offsetX + 0.0,         offsetY + height * p,
        offsetX + width * q,   offsetY + 0.0,
        offsetX + width * p,   offsetY + 0.0,
        offsetX + width * 0.5, offsetY + height * q,
        offsetX + width,       offsetY + height * q,
        offsetX + width * p,   offsetY + height));
    }
    return vertices;
  }
}
