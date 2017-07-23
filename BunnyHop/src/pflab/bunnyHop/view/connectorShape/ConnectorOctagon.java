package pflab.bunnyHop.view.connectorShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import pflab.bunnyHop.view.BhNodeViewStyle.CNCTR_POS;

/**
 * 丸型コネクタクラス
 * @author K.Koike
 * */
public class ConnectorOctagon extends ConnectorShape {

	/** コネクタの頂点を算出する
	 * @param offsetX 頂点に加算するオフセットX
	 * @param offsetY 頂点に加算するオフセットY
	 * @param width   コネクタの幅
	 * @param height  コネクタの高さ
	 * */
	@Override
	public Collection<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos) {

		ArrayList<Double> vertices = null;
		double p = 4.0;
		double q = 1.0;
		double r = 4.0;
		double s = 1.0;

		if (pos == CNCTR_POS.LEFT) {
			vertices = new ArrayList<>(Arrays.asList(
				offsetX + width,                  offsetY + height * (1.0 - q / p),
				offsetX + width * (1.0 - s / r),  offsetY + height,
				offsetX + width * (s / r),        offsetY + height,
				offsetX + 0.0,                    offsetY + height * (1.0 - q / p),
				offsetX + 0.0,                    offsetY + height * (q / p),
				offsetX + width * (s / r),        offsetY + 0.0,
				offsetX + width * (1.0 - s / r),  offsetY + 0.0,
				offsetX + width,                  offsetY + height * (q / p)));
		}
		else if (pos == CNCTR_POS.TOP) {
			vertices = new ArrayList<>(Arrays.asList(
				offsetX + width * (q / p),        offsetY + height,
				offsetX + 0.0,                    offsetY + height * (1.0 - s / r),
				offsetX + 0.0,                    offsetY + height * (s / r),
				offsetX + width * (q / p),        offsetY + 0.0,
				offsetX + width * (1.0 - q / p),  offsetY + 0.0,
				offsetX + width,                  offsetY + height * (s / r),
				offsetX + width,                  offsetY + height * (1.0 - s / r),
				offsetX + width * (1.0 - q / p),  offsetY + height));
		}
		return vertices;
	}
}

