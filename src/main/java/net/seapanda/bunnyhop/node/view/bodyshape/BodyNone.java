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

package net.seapanda.bunnyhop.node.view.bodyshape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorNone;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;

/**
 * 何も描画しないボディ.
 *
 * @author K.Koike
 */
public class BodyNone extends BodyShape {

  @Override
  public Collection<Double> createVertices(
      BhNodeViewStyle style,
      double bodyWidth,
      double bodyHeight,
      ConnectorShape connector,
      ConnectorShape notch) {

    double cnctrWidth = style.connectorWidth;
    double cnctrHeight = style.connectorHeight;
    double cnctrShift = style.connectorShift;

    if (connector instanceof ConnectorNone) {
      return new ArrayList<>();
    }
    List<Double> vertices = createConnectorVertices(
        connector, style, cnctrWidth, cnctrHeight, cnctrShift, bodyWidth, bodyHeight);
    if (style.connectorPos == ConnectorPos.LEFT) {
      vertices.addAll(Arrays.asList(0.0, cnctrShift + cnctrHeight * 0.5));
    } else if (style.connectorPos == ConnectorPos.TOP) {
      vertices.addAll(Arrays.asList(cnctrShift + cnctrWidth * 0.5, 0.0));
    }
    return vertices;
  }
}
