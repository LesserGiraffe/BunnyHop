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
package net.seapanda.bunnyhop.view.bodyshape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.seapanda.bunnyhop.view.connectorshape.ConnectorNone;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle.CNCTR_POS;

/**
 * 描画しないボディ
 * @param K.Koike
 * */
public class BodyNone extends BodyShape {

	@Override
	public Collection<Double> createVertices(
		double bodyWidth,
		double bodyHeight,
		ConnectorShape connector,
		BhNodeViewStyle.CNCTR_POS cnctrPos,
		double cnctrWidth,
		double cnctrHeight,
		double cnctrShift,
		ConnectorShape notch,
		BhNodeViewStyle.NOTCH_POS notchPos,
		double notchWidth,
		double notchHeight) {

		if (connector instanceof ConnectorNone) {
			return new ArrayList<>();
		}
		List<Double> vertices = createConnectorVertices(connector, cnctrPos, cnctrWidth, cnctrHeight, cnctrShift);
		if (cnctrPos == CNCTR_POS.LEFT) {
			vertices.addAll(Arrays.asList(0.0, cnctrShift + cnctrHeight * 0.5));
		}
		else if (cnctrPos == CNCTR_POS.TOP) {
			vertices.addAll(Arrays.asList(cnctrShift + cnctrWidth * 0.5, 0.0));
		}
		return vertices;
	}
}
