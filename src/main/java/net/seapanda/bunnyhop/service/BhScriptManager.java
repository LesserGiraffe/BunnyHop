/*
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

package net.seapanda.bunnyhop.service;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.json.JsonParser;

/**
 * JavaScript コードを管理するクラス.
 *
 * @author K.Koike
 */
public class BhScriptManager {

  public static final BhScriptManager INSTANCE = new BhScriptManager();
  /** スクリプト名とコンパイル済みスクリプトのマップ. */
  private final HashMap<String, Script> scriptNameToScript = new HashMap<>();
  /** スクリプト共通で使う Javascript オブジェクト. */
  private Object commonJsObj;

  /**
   * JavaScript 実行時の変数スコープを新たに作成する.
   *
   * @return JavaScript 実行時の変数スコープ
   */
  public ScriptableObject createScriptScope() {
    Context cx = ContextFactory.getGlobal().enterContext();
    ScriptableObject scope = cx.initStandardObjects();
    Context.exit();
    return scope;
  }

  /**
   * JavaScriptのファイルパスからコンパイル済みスクリプトを取得する.
   *
   * @param fileName 取得したいスクリプトのファイル名. null 許可.
   * @return {@code fileName} で指定した名前のスクリプト. fileName が null の場合は, null.
   */
  public Script getCompiledScript(String fileName) {
    if (fileName == null) {
      return null;
    }
    return scriptNameToScript.get(fileName);
  }

  /**
   * JavaScript ファイルを読み込み、コンパイルする.
   *
   * @param dirPaths このフォルダの下にある.jsファイルをコンパイルする
   * @return ひとつでもコンパイル不能なJSファイルがあった場合 false を返す
   */
  public boolean genCompiledCode(Path... dirPaths) {
    boolean success = true;
    for (Path dirPath : dirPaths) {
      Stream<Path> paths;  //読み込むファイルパスリスト
      try {
        paths = Files.walk(dirPath, FOLLOW_LINKS)
            .filter(path -> path.getFileName().toString().endsWith(".js")); //.jsファイルだけ収集
      } catch (IOException e) {
        MsgPrinter.INSTANCE.errMsgForDebug("Directory not found.  (%s)".formatted(dirPath));
        success &= false;
        continue;
      }
      Context cx = ContextFactory.getGlobal().enterContext();
      cx.setLanguageVersion(Context.VERSION_ES6);
      cx.setOptimizationLevel(9);
      success &= paths
        .map(path -> {
          try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Script script = cx.compileReader(reader, path.getFileName().toString(), 1, null);
            scriptNameToScript.put(path.getFileName().toString(), script);
          } catch (IOException e) {
            MsgPrinter.INSTANCE.errMsgForDebug(e + "  " + path);
            return false;
          }
          return true;
        })
        .allMatch(Boolean::valueOf);
      Context.exit();
    }
    success &= genCommonObj();
    return success;
  }

  /**
   * 各スクリプトが共通で使うオブジェクトを生成する.
   *
   * @return オブジェクトの作成に成功した場合 true, 失敗した場合 false.
   */
  private boolean genCommonObj() {
    if (scriptNameToScript.containsKey(BhConstants.Path.COMMON_FUNCS_JS)) {
      try {
        commonJsObj = ContextFactory.getGlobal().call(
          cx -> scriptNameToScript
              .get(BhConstants.Path.COMMON_FUNCS_JS)
              .exec(cx, newScriptScope())
        );
      } catch (Exception e) {
        MsgPrinter.INSTANCE.errMsgForDebug(
            "Failed to execute %s\n%s".formatted(BhConstants.Path.COMMON_FUNCS_JS, e));
        return false;
      }
    }
    return true;
  }

  private ScriptableObject newScriptScope() {
    ScriptableObject scriptScope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_FACTORY, BhNodeFactory.INSTANCE);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_HANDLER, BhNodeHandler.INSTANCE);
    return scriptScope;
  }

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   *
   * @param fileName スクリプト名の書いてあるファイル名
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  public boolean scriptsExist(String fileName, String... scriptNames) {
    Stream<String> scriptNameStream = Stream.of(scriptNames);
    return scriptNameStream
      .map(scriptName -> {
        boolean found = scriptNameToScript.get(scriptName) != null;
        if (!found) {
          MsgPrinter.INSTANCE.errMsgForDebug(
              "Cannot find '%s'.  file: %s".formatted(scriptNames, fileName));
        }
        return found;
      })
      .allMatch(Boolean::valueOf);
  }

  /**
   * Json ファイルをパースしてオブジェクトにして返す.
   *
   * @param filePath Json ファイルのパス
   * @return Json ファイルをパースしてできたオブジェクト. 失敗した場合 empty.
   */
  public Optional<NativeObject> parseJsonFile(Path filePath) {
    Object jsonObj = null;
    try {
      byte[] contents = Files.readAllBytes(filePath);
      String jsCode = new String(contents, StandardCharsets.UTF_8);
      Context cx = ContextFactory.getGlobal().enterContext();
      jsonObj = (new JsonParser(cx, cx.initStandardObjects())).parseValue(jsCode);
      Context.exit();

    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("Cannot read json file.  %s\n%s".formatted(filePath, e));
      return Optional.empty();

    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug("Cannot parse json file.  %s\n%s".formatted(filePath, e));
      return Optional.empty();
    }
    if (!(jsonObj instanceof NativeObject)) {
      MsgPrinter.INSTANCE.errMsgForDebug("Cannot parse json file.  " + filePath);
      return Optional.empty();
    }
    return Optional.of((NativeObject) jsonObj);
  }

  /** スクリプトが共通で使うオブジェクトを返す. */
  public Object getCommonJsObj() {
    return commonJsObj;
  }
}
