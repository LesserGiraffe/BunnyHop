package pflab.bunnyHop.common;

/**
 * モデル構造表示用インタフェース (デバッグ用)
 * @author K.Koike
 * */
public interface Showable {

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	public void show(int depth);

	default String indent(int depth) {
		String ret = "";
		for (int i = 0; i < depth; ++i)
			ret += "	";
		return ret;
	}
}
