package pflab.bunnyHop.view.connectorShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import pflab.bunnyHop.view.BhNodeViewStyle.CNCTR_POS;

/**
 * V字型コネクタクラス
 * @author K.Koike
 * */
public class ConnectorCharV extends ConnectorShape {


	/** コネクタの頂点を算出する
	 * @param offsetX 頂点に加算するオフセットX
	 * @param offsetY 頂点に加算するオフセットY
	 * @param width   コネクタの幅
	 * @param height  コネクタの高さ
	 * */
	@Override
	public Collection<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos) {

		ArrayList<Double> vertices = null;
		double p = 3.0;
		double q = 1.0;
		double r = 3.0;
		double s = 3.0;

		if (pos == CNCTR_POS.LEFT) {
			vertices = new ArrayList<>(Arrays.asList(
				offsetX + width,             offsetY + height,
				offsetX + 0.0,               offsetY + height,
				offsetX + width,             offsetY + height / 2.0,
				offsetX + 0.0,               offsetY + 0.0,
				offsetX + width,             offsetY + 0.0));
		}
		else if (pos == CNCTR_POS.TOP) {
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

