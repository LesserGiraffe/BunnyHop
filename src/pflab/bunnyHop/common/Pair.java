package pflab.bunnyHop.common;

/**
 * タプル
 * @author K.Koike
 * */
public class Pair<T1, T2> {

	public Pair(T1 _1, T2 _2) {
		this._1 = _1;
		this._2 = _2;
	}

	public Pair(){}

	public T1 _1;
	public T2 _2;

	@Override
	public String toString() {
		return "1:" + _1.toString() + "  " + "2:" + _2.toString();
	}
}
