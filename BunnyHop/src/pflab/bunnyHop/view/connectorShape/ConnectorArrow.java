package pflab.bunnyHop.view.connectorShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import pflab.bunnyHop.view.BhNodeViewStyle.CNCTR_POS;

/**
 * 矢印型コネクタクラス
 * @author K.Koike
 * */
public class ConnectorArrow extends ConnectorShape {


	/** コネクタの頂点を算出する
	 * @param offsetX 頂点に加算するオフセットX
	 * @param offsetY 頂点に加算するオフセットY
	 * @param width   コネクタの幅
	 * @param height  コネクタの高さ
	 * @param pos     コネクタの位置 (Top or Left)
	 * */
	@Override
	public Collection<Double> createVertices(double offsetX, double offsetY, double width, double height, CNCTR_POS pos) {

		ArrayList<Double> vertices = null;
		//形制御用パラメータ
		double p = 4.0;
		double q = 1.0;
		double r = 3.0;
		double s = 1.8;


		if (pos == CNCTR_POS.LEFT) {
			vertices = new ArrayList<>(Arrays.asList(
				offsetX + width,             offsetY + height * (1.0 - q / p),
				offsetX + width * (s / r),   offsetY + height * (1.0 - q / p),
				offsetX + width * (s / r),   offsetY + height,
				offsetX + 0.0,               offsetY + height / 2.0,
				offsetX + width * (s / r),   offsetY + 0.0,
				offsetX + width * (s / r),   offsetY + height * (q / p),
				offsetX + width,             offsetY + height * (q / p)));
		}
		else if (pos == CNCTR_POS.TOP) {
			vertices = new ArrayList<>(Arrays.asList(
				offsetX + width * (q / p),        offsetY + height,
				offsetX + width * (q / p),        offsetY + height * (s / r),
				offsetX + 0.0,                    offsetY + height * (s / r),
				offsetX + width / 2.0,            offsetY + 0.0,
				offsetX + width,                  offsetY + height * (s / r),
				offsetX + width * (1.0 - q / p),  offsetY + height * (s / r),
				offsetX + width * (1.0 - q / p),  offsetY + height));
		}
		return vertices;
	}
}

