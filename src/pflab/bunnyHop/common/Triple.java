package pflab.bunnyHop.common;

/**
 * タプル
 * @author K.Koike
 */
public class Triple<T1, T2, T3> {

	public Triple(T1 _1, T2 _2, T3 _3) {
		this._1 = _1;
		this._2 = _2;
		this._3 = _3;
	}
	
	public Triple() {}

	public T1 _1;
	public T2 _2;
	public T3 _3;

	@Override
	public String toString() {
		return "1:" + _1.toString() + "  " + "2:" + _2.toString() + "  " + "3:" + _3.toString();
	}
}
