package pflab.bunnyHop.common;

import java.io.Serializable;

/**
 * @author K.Koike
 */
public class Point2D implements Serializable {

	public double x;
	public double y;

	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object point) {
		
		if (point instanceof Point2D)
			return (x == ((Point2D)point).x) && (y == ((Point2D)point).y);
		else
			return false;
	}
}
