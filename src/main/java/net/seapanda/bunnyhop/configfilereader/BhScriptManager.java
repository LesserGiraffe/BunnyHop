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
package net.seapanda.bunnyhop.configfilereader;

import static java.nio.file.FileVisitOption.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.json.JsonParser;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;

/**
 * Javascriptを管理するクラス
 * @author K.Koike
 * */
public class BhScriptManager {

	public static final BhScriptManager INSTANCE = new BhScriptManager();	//!< シングルトンインスタンス
	private final HashMap<String, Script> scriptName_script = new HashMap<>();	//!< スクリプト名とコンパイル済みスクリプトのマップ
	private Object commonJsObj;	//!< スクリプト共通で使うJavascriptオブジェクト

	/**
	 * Javascript 実行時の変数スコープを新たに作成する
	 * @return Javascript 実行時の変数スコープ
	 */
	public ScriptableObject createScriptScope() {

		Context cx = ContextFactory.getGlobal().enterContext();
		ScriptableObject scope = cx.initStandardObjects();
		Context.exit();
		return scope;
	}

	/**
	 * Javascriptのファイルパスからコンパイル済みスクリプトを取得する
	 * @param fileName 取得したいスクリプトのファイル名
	 * @return jsPath で指定したスクリプト
	 * */
	public Script getCompiledScript(String fileName) {
		return scriptName_script.get(fileName);
	}

	/**
	 * Javascriptファイルを読み込み、コンパイルする
	 * @param dirPaths このフォルダの下にある.jsファイルをコンパイルする
	 * @return ひとつでもコンパイル不能なJSファイルがあった場合 false を返す
	 * */
	public boolean genCompiledCode(Path... dirPaths) {

		boolean success = true;
		for (Path dirPath : dirPaths) {
			Stream<Path> paths;	//読み込むファイルパスリスト
			try {
				paths = Files.walk(dirPath, FOLLOW_LINKS)
							 .filter(path -> path.getFileName().toString().endsWith(".js")); //.jsファイルだけ収集
			}
			catch (IOException e) {
				MsgPrinter.INSTANCE.errMsgForDebug(BhParams.Path.FUNCTIONS_DIR + " directory not found " + dirPath);
				success &= false;
				continue;
			}

			Context cx = ContextFactory.getGlobal().enterContext();
			cx.setLanguageVersion(Context.VERSION_ES6);
			cx.setOptimizationLevel(9);
			success &= paths.map(path -> {

				try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
					Script script = cx.compileReader(reader, path.getFileName().toString(), 1, null);
					scriptName_script.put(path.getFileName().toString(), script);
				}
				catch (IOException e) {
					MsgPrinter.INSTANCE.errMsgForDebug(e.toString() + "  " + path.toString());
					return false;
				}
				return true;
			}).allMatch(bool -> bool);
			Context.exit();
		}
		success &= genCommonObj();
		return success;
	}

	/**
	 * 各スクリプトが共通で使うオブジェクトを生成する
	 * @return オブジェクトの作成に成功した場合true, 失敗した場合false.
	 * */
	private boolean genCommonObj() {

		if (scriptName_script.containsKey(BhParams.Path.COMMON_EVENT_JS)) {
			try {
				commonJsObj = ContextFactory.getGlobal().call(cx -> {
					return scriptName_script.get(BhParams.Path.COMMON_EVENT_JS).exec(cx, cx.initStandardObjects());
				});
			}
			catch (Exception e) {
				MsgPrinter.INSTANCE.errMsgForDebug("exec " + BhParams.Path.COMMON_EVENT_JS + "\n" + e.toString() + "\n");
				return false;
			}
		}
		return true;
	}

	/**
	 * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする
 	 * @param fileName スクリプト名の書いてあるファイル名
	 * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
	 * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合true
	 */
	public boolean scriptsExist(String fileName, String... scriptNames) {

		Stream<String> scriptNameStream = Stream.of(scriptNames);
		return scriptNameStream.allMatch(scriptName ->{
			boolean found = scriptName_script.get(scriptName) != null;
			if (!found) {
				MsgPrinter.INSTANCE.errMsgForDebug(scriptName + " が見つかりません.  file: " + fileName);
			}
			return found;
		});
	}

	/**
	 * Jsonファイルをパースしてオブジェクトにして返す
	 * @param filePath Jsonファイルのパス
	 * @return Jsonファイルをパースしてできたオブジェクト
	 */
	public NativeObject parseJsonFile(Path filePath) {

		Object jsonObj = null;
		try {
			byte[] contents = Files.readAllBytes(filePath);
			String jsCode = new String(contents, StandardCharsets.UTF_8);
			Context cx = ContextFactory.getGlobal().enterContext();
			jsonObj = (new JsonParser(cx, cx.initStandardObjects())).parseValue(jsCode);
			Context.exit();
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug("cannot read json file.  " + filePath + "\n" + e.toString() + "\n");
			return null;
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug("cannot parse json file.  " + filePath + "\n" + e.toString() + "\n");
			return null;
		}
		if (!(jsonObj instanceof NativeObject)) {
			MsgPrinter.INSTANCE.errMsgForDebug("cannot parse json file.  " + filePath);
			return null;
		}
		return (NativeObject)jsonObj;
	}

	/**
	 * スクリプトが共通で使うオブジェクトを返す
	 */
	public Object getCommonJsObj() {
		return commonJsObj;
	}
}
