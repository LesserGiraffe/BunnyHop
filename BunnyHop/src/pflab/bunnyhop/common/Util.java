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
package pflab.bunnyhop.common;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import java.io.File;
/*
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;*/
import javafx.scene.text.Font;

/**
 * @author K.Koike
 */
public class Util {
	
	public static final String EXEC_PATH;	//実行時jarパス
	public static final String LF;
	private static int serialID = 0;

	static {
		String path = System.getProperty("java.class.path");
		File jarFile = new File(path);
		//Path jarPath = Paths.get(jarFile.getAbsolutePath());
		//String root = (jarPath.getRoot() == null) ? "" : jarPath.getRoot().toString();
		//EXEC_PATH = root + jarPath.subpath(0, jarPath.getNameCount()-1).toString();
		EXEC_PATH = System.getProperty("user.dir");
		LF = System.getProperty("line.separator");
	}
	
	/**
	 * ワイルドカード比較機能つき文字列一致検査.
	 * @param whole 比較対象の文字列. wildcard指定不可.
	 * @param part 比較対象の文字列. wildcard指定可.
	 * @return partにwildcard がある場合, wholeがpartを含んでいればtrue. <br>
	 * partにwildcard が無い場合, wholeとpartが一致すればtrue.
	 */
	public static boolean equals(String whole, String part) {
		
		if (whole == null || part == null)
			return false;
		
		if (!part.contains("*"))
			return whole.equals(part);
		
		return whole.contains(part.substring(0, part.indexOf("*")));
	}
	
	/**
	 * 引数で指定した文字列の表示幅を計算する
	 * @param str 表示幅を計算する文字列
	 * @param font 表示時のフォント
	 * @return 文字列を表示したときの幅
	 */
	public static double calcStrWidth(String str, Font font) {
		FontMetrics fm = Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
		return fm.computeStringWidth(str);
	}
	
	/**
	 * シリアルIDを取得する
	 * @return シリアルID
	 */
	public static String genSerialID() {
		return Integer.toHexString(serialID++) + "";
	}
}






