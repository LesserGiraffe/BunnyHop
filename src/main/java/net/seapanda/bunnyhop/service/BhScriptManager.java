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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.common.BhConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * JavaScript コードを管理するクラス.
 *
 * @author K.Koike
 */
public class BhScriptManager {

  /** スクリプト名とコンパイル済みスクリプトのマップ. */
  private final Map<String, Script> scriptNameToScript = new ConcurrentHashMap<>();
  /** スクリプト共通で使う Javascript オブジェクト. */
  private volatile Object commonJsObj = null;

  /**
   * コンストラクタ.
   *
   * @param dirPaths このディレクトリの下にある *.js ファイルをコンパイルして保持する.
   * @throws IOException Javascript ファイルのコンパイルに失敗した場合
   */
  BhScriptManager(Path... dirPaths) throws IOException {
    compile(dirPaths);
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
  private void compile(Path... dirPaths) throws IOException {
    for (Path dirPath : dirPaths) {
      List<Path> jsFilePaths = new ArrayList<>();
      try {
        jsFilePaths = Files.walk(dirPath, FOLLOW_LINKS)
            .filter(path -> path.getFileName().toString().endsWith(".js")) // .jsファイルだけ収集
            .toList();
      } catch (IOException e) {
        BhService.msgPrinter().errForDebug("Directory not found.  (%s)".formatted(dirPath));
        throw e;
      }
      Context cx = ContextFactory.getGlobal().enterContext();
      cx.setLanguageVersion(Context.VERSION_ES6);
      cx.setOptimizationLevel(9);
      for (Path path : jsFilePaths) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
          Script script = cx.compileReader(reader, path.getFileName().toString(), 1, null);
          scriptNameToScript.put(path.getFileName().toString(), script);
        } catch (IOException e) {
          throw e;
        }
      }
      Context.exit();
    }
  }

  /** スクリプトで共通の処理が定義されたオブジェクトを取得する. */
  public Object getCommonJsObj() {
    // サービスクラスの準備が終わってから JavaScript オブジェクトを作りたいので遅延初期化する.
    if (commonJsObj != null) {
      return commonJsObj;
    }

    synchronized (this) {
      if (commonJsObj == null) {
        commonJsObj = genCommonJsObj();
      }
      return commonJsObj;
    }
  }

  /** 各スクリプトが共通で使う JavaScript オブジェクトを生成する. */
  private Object genCommonJsObj() {
    if (!scriptNameToScript.containsKey(BhConstants.Path.COMMON_FUNCS_JS)) {
      return new NativeObject();
    }
    try {
      Context cx = Context.enter();
      Object jsObj = scriptNameToScript.get(BhConstants.Path.COMMON_FUNCS_JS)
            .exec(cx, createScriptScope(cx));
      return (jsObj instanceof NativeObject) ? jsObj : new NativeObject();
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "Failed to execute %s\n%s".formatted(BhConstants.Path.COMMON_FUNCS_JS, e));
    } finally {
      Context.exit();
    }
    return new NativeObject();
  }

  private ScriptableObject createScriptScope(Context cx) {
    ScriptableObject scope = cx.initStandardObjects();
    Object val = Context.javaToJS(BhService.bhNodeFactory(), scope);
    scope.put(BhConstants.JsIdName.BH_NODE_FACTORY, scope, val);
    val = Context.javaToJS(BhService.bhNodePlacer(), scope);
    scope.put(BhConstants.JsIdName.BH_NODE_PLACER, scope, val);
    val = Context.javaToJS(BhService.textDb(), scope);
    scope.put(BhConstants.JsIdName.BH_TEXT_DB, scope, val);
    return scope;
  }

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   *
   * @param fileName スクリプト名の書いてあるファイル名
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  public boolean allExist(String fileName, String... scriptNames) {
    boolean allFound = true;
    for (String scriptName : scriptNames) {
      boolean found = scriptNameToScript.get(scriptName) != null;
      if (!found) {
        BhService.msgPrinter().errForDebug(
            "Cannot find '%s'.  file: %s".formatted(scriptName, fileName));
      }
      allFound &= found;
    }
    return allFound;
  }

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   * ただし, スクリプト名が null か空文字だった場合, そのスクリプトの存在は調べない.
   *
   * @param fileName スクリプト名の書いてあるファイル名
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  public boolean allExistIgnoringEmpty(String fileName, String... scriptNames) {
    String[] scriptNamesFiltered = Stream.of(scriptNames)
        .filter(scriptName -> scriptName != null && !scriptName.isEmpty())
        .toArray(String[]::new);

    return allExist(fileName, scriptNamesFiltered);
  }
}
