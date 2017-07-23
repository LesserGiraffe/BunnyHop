package pflab.bunnyHop.bhProgram;

import java.io.Serializable;

/**
 * BunnyHopとスクリプトの実行環境間で送受信されるデータ
 * @author K.Koike
 */
public class BhProgramData implements Serializable {
	
	public final TYPE type;
	public final String str;
	
	public BhProgramData(TYPE type, String str) {
		this.type = type;
		this.str = str;
	}
	
	/**
	 * データの種類
	 */
	public enum TYPE implements Serializable {
		OUTPUT_STR,
		INPUT_STR,
	}
}
