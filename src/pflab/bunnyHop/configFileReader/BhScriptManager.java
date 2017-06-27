package pflab.bunnyHop.configFileReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.root.MsgPrinter;

/**
 * Javascriptを管理するクラス
 * @author K.Koike
 * */
public class BhScriptManager {

	public static final BhScriptManager instance = new BhScriptManager();	//!< シングルトンインスタンス
	private final HashMap<String, CompiledScript> scriptName_script = new HashMap<>();	//!< スクリプト名とコンパイル済みスクリプトのハッシュ
	private final ScriptEngine engine = (new NashornScriptEngineFactory()).getScriptEngine("--language=es6");
	private Object commonJsObj;	//!< スクリプト共通で使うJavascriptオブジェクト

	/**
	 * Javascript 実行時の変数スコープを新たに作成する
	 * @return Javascript 実行時の変数スコープ
	 */
	public Bindings createScriptScope() {
		return engine.createBindings();
	}
	
	/**
	 * Javascriptのファイルパスからコンパイル済みスクリプトを取得する
	 * @param jsPath 取得したいスクリプトのパス
	 * @return jsPath で指定したスクリプト
	 * */
	public CompiledScript getCompiledScript(String jsPath) {
		return scriptName_script.get(jsPath);
	}

	/**
	 * Javascriptファイルを読み込み、コンパイルする
	 * @return ひとつでもコンパイル不能なJSファイルがあった場合 false を返す
	 * */
	public boolean genCompiledCode() {

		Path dirPath = Paths.get(Util.execPath, BhParams.Path.bhDefDir, BhParams.Path.javascriptDir);
		Stream<Path> paths;	//読み込むファイルパスリスト
		try {
			paths = Files.walk(dirPath).filter(path -> path.getFileName().toString().endsWith(".js")); //.jsファイルだけ収集
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug(BhParams.Path.javascriptDir + " directory not found " + dirPath);
			return false;
		}
		
		boolean success = paths.map(path -> {
			
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
				CompiledScript cs = ((Compilable)engine).compile(reader);
				scriptName_script.put(path.getFileName().toString(), cs);
			}
			catch (IOException e) {
				MsgPrinter.instance.ErrMsgForDebug(e.getMessage());
				return false;
			}
			catch (ScriptException e) {
				MsgPrinter.instance.ErrMsgForDebug(path.toUri() + " がコンパイルできません.");
				MsgPrinter.instance.ErrMsgForDebug(e.getMessage());
				return false;
			}
			return true;
		}).allMatch(bool -> bool);

		if (scriptName_script.containsKey(BhParams.Path.commonJS)) {
			try {
				commonJsObj = scriptName_script.get(BhParams.Path.commonJS).eval();
			} catch (ScriptException e) {
				MsgPrinter.instance.ErrMsgForDebug("exec " + BhParams.Path.commonJS + "\n" + e.getMessage() + "\n");
				return false;
			}
		}		
		return success;
	}
	
	/**
	 * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする
	 * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
	 * @param fileName スクリプト名の書いてあるファイル名
	 * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合true
	 */
	public boolean checkIfScriptsExist(List<String> scriptNames, String fileName) {
		Stream<String> scriptNameStream = scriptNames.stream();
		return scriptNameStream.allMatch(scriptName ->{
			boolean found = scriptName_script.get(scriptName) != null;
			if (!found) {
				MsgPrinter.instance.ErrMsgForDebug(scriptName + " が見つかりません. " + fileName);
			}	
			return found;
		});
	}
	
	/**
	 * Jsonファイルをパースしてオブジェクトにして返す
	 * @param filePath Jsonファイルのパス
	 * @return Jsonファイルをパースしてできたオブジェクト
	 */
	public ScriptObjectMirror parseJsonFile(Path filePath) {
		
		Object jsonObj = null;
		try {
			byte[] contents = Files.readAllBytes(filePath);
			String jsCode = new String(contents, StandardCharsets.UTF_8);
			jsCode = "var content = " + jsCode + ";" + Util.LF;
			jsCode += "(function () {return content;})();";
			CompiledScript cs = ((Compilable)engine).compile(jsCode);
			jsonObj = cs.eval();
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("cannot read json file.  " + filePath + "\n" + e.getMessage() + "\n");
			return null;
		}
		catch (ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("cannot parse json file.  " + filePath + "\n" + e.getMessage() + "\n");
			return null;
		}
		if (!(jsonObj instanceof ScriptObjectMirror)) {
			MsgPrinter.instance.ErrMsgForDebug("cannot parse json file.  " + filePath);
			return null;
		}
		return (ScriptObjectMirror)jsonObj;
	}
	
	/**
	 * スクリプトが共通で使うオブジェクトを返す
	 */
	public Object getCommonJsObj() {
		return commonJsObj;
	}
}
