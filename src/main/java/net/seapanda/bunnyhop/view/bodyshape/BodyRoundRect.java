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

package net.seapanda.bunnyhop.view.bodyshape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;

/**
 * 丸角長方形ボディ.
 *
 * @author K.Koike
 */
public class BodyRoundRect extends BodyShapeBase {

  @Override
  public Collection<Double> createVertices(
      double bodyWidth,
      double bodyHeight,
      ConnectorShape connector,
      BhNodeViewStyle.ConnectorPos cnctrPos,
      double cnctrWidth,
      double cnctrHeight,
      double cnctrShift,
      ConnectorShape notch,
      BhNodeViewStyle.NotchPos notchPos,
      double notchWidth,
      double notchHeight) {

    ArrayList<Double> bodyVertices = null;
    bodyVertices = new ArrayList<>(Arrays.asList(
       0.0,                                        0.0 + 0.2 * BhConstants.LnF.NODE_SCALE,
       0.0 + 0.2 * BhConstants.LnF.NODE_SCALE,        0.0,
       bodyWidth - 0.2 * BhConstants.LnF.NODE_SCALE,  0.0,
       bodyWidth,                                  0.0 + 0.2 * BhConstants.LnF.NODE_SCALE,
       bodyWidth,                                  bodyHeight - 0.2 * BhConstants.LnF.NODE_SCALE,
       bodyWidth - 0.2 * BhConstants.LnF.NODE_SCALE,  bodyHeight,
        0.0 + 0.2 * BhConstants.LnF.NODE_SCALE,       bodyHeight,
        0.0,                                       bodyHeight - 0.2 * BhConstants.LnF.NODE_SCALE));

    List<Double> notchVertices =
        createNotchVertices(notch, notchPos, notchWidth, notchHeight, bodyWidth, bodyHeight);
    if (notchPos == BhNodeViewStyle.NotchPos.RIGHT) {
      bodyVertices.addAll(8, notchVertices);
    } else if (notchPos == BhNodeViewStyle.NotchPos.BOTTOM) {
      bodyVertices.addAll(12, notchVertices);
    }

    List<Double> cnctrVertices =
        createConnectorVertices(connector, cnctrPos, cnctrWidth, cnctrHeight, cnctrShift);
    if (cnctrPos == BhNodeViewStyle.ConnectorPos.LEFT) {
      bodyVertices.addAll(cnctrVertices);
    } else if (cnctrPos == BhNodeViewStyle.ConnectorPos.TOP) {
      bodyVertices.addAll(4, cnctrVertices);
    }
    return bodyVertices;
  }
}
