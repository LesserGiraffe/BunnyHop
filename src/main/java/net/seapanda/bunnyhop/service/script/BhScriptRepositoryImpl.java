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

package net.seapanda.bunnyhop.service.script;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.service.LogManager;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;

/**
 * アプリケーション外部で定義されたスクリプトを保持するクラス.
 *
 * @author K.Koike
 */
public class BhScriptRepositoryImpl implements BhScriptRepository {

  /** スクリプト名とコンパイル済みスクリプトのマップ. */
  private final Map<String, Script> scriptNameToScript = new ConcurrentHashMap<>();

  /**
   * コンストラクタ.
   *
   * @param dirPaths このディレクトリの下にある *.js ファイルをコンパイルして保持する.
   * @throws IOException Javascript ファイルのコンパイルに失敗した場合
   */
  public BhScriptRepositoryImpl(Path... dirPaths) throws IOException {
    compile(dirPaths);
  }

  @Override
  public Script getScript(String fileName) {
    if (fileName == null) {
      return null;
    }
    return scriptNameToScript.get(fileName);
  }

  /**
   * JavaScript ファイルを読み込み、コンパイルする.
   *
   * @param dirPaths このディレクトリの下にある .js ファイルをコンパイルする
   */
  private void compile(Path... dirPaths) throws IOException {
    try (Context cx = ContextFactory.getGlobal().enterContext()) {
      for (Path dirPath : dirPaths) {
        Collection<Path> jsFilePaths = collectJavaScriptFilePaths(dirPath);
        compile(jsFilePaths, cx);
      }
    }
  }

  private void compile(Collection<Path> jsFilePaths, Context cx) throws IOException {
    for (Path path : jsFilePaths) {
      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        Script script = cx.compileReader(reader, path.getFileName().toString(), 1, null);
        scriptNameToScript.put(path.getFileName().toString(), script);
      }
    }
  }

  /** 引数で指定したディレクトリ以下にある .js ファイルのパスを取得する. */
  private Collection<Path> collectJavaScriptFilePaths(Path dirPath) throws IOException {
    try (var jsFilePaths = Files.walk(dirPath, FOLLOW_LINKS)) {
      return jsFilePaths
          .filter(path -> path.getFileName().toString().endsWith(".js")) // .jsファイルだけ収集
          .toList();
    } catch (IOException e) {
      LogManager.logger().error("Directory not found.  (%s)".formatted(dirPath));
      throw e;
    }
  }

  @Override
  public boolean allExist(String... scriptNames) {
    return allExistWithHandler(name -> {}, scriptNames);
  }

  @Override
  public boolean allExistWithHandler(Consumer<String> onNodeNotFound, String... scriptNames) {
    boolean allFound = true;
    for (String scriptName : scriptNames) {
      boolean found = scriptNameToScript.get(scriptName) != null;
      if (!found) {
        onNodeNotFound.accept(scriptName);
      }
      allFound &= found;
    }
    return allFound;
  }

  @Override
  public boolean allExistIgnoringEmpty(String... scriptNames) {
    return allExistIgnoringEmptyWithHandler(name -> {}, scriptNames);
  }

  @Override
  public boolean allExistIgnoringEmptyWithHandler(
      Consumer<String> onScriptNotFound, String... scriptNames) {
    String[] scriptNamesFiltered = Stream.of(scriptNames)
        .filter(scriptName -> scriptName != null && !scriptName.isEmpty())
        .toArray(String[]::new);

    return allExistWithHandler(onScriptNotFound, scriptNamesFiltered);
  }  
}
