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

package net.seapanda.bunnyhop.compile;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * コンパイルに関する設定を格納するクラス.
 *
 * @author K.Koike
 */
public class CompileConfig {

  private static final String LANGUAGE_PLACEHOLDER = "${language}";

  /** 設定の ID. */
  private final String id;

  /** コンパイルに使用するライブラリのパス. */
  @SerializedName("libs")
  private ArrayList<String> libPaths;

  /** コンパイラの拡張要素を記述したファイルのパス. */
  @SerializedName("extensions")
  private ArrayList<String> extensionPaths;

  /** メインルーチンの名前. */
  @SerializedName("mainRoutine")
  private final String mainRoutineName;

  /** コンストラクタ (デシリアライズ用). */
  private CompileConfig() {
    id = "";
    libPaths = new ArrayList<>();
    extensionPaths = new ArrayList<>();
    mainRoutineName = "_bhMain";
  }

  public String getId() {
    return id;
  }

  public List<String> getLibPaths() {
    return libPaths;
  }

  public List<String> getExtensionPaths() {
    return extensionPaths;
  }

  public String getMainRoutineName() {
    return mainRoutineName;
  }

  /**
   * {@code paths} の各要素に含まれる言語プレースホルダを {@code language} で置換し,
   * {@code base} を基準とした正規化済みの絶対パス文字列に変換する.
   *
   * @param paths 変換対象のパス文字列のリスト (nullable)
   * @param base 相対パスの解決に使用する基準ディレクトリ
   * @param language プレースホルダ ({@value #LANGUAGE_PLACEHOLDER}) を置換する言語名
   * @return 変換後のパス文字列のリスト. {@code paths} が null の場合は null.
   */
  private static ArrayList<String> resolvePathList(List<String> paths, Path base, String language) {
    if (paths == null) {
      return null;
    }
    return paths.stream()
        .map((String path) -> resolvePlaceHolder(path, language))
        .map(lib -> base.resolve(lib).normalize().toString())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * {@code configFilePath} で指定された設定ファイルを読み込み, ID と {@link CompileConfig} の対応関係を取得する.
   *
   * @param configFilePath コンパイルに関する設定が書かれたファイルのパス
   * @param language 言語設定
   * @return ID と {@link CompileConfig} の対応関係を格納したマップ
   * @throws IOException {@code configFilePath} のファイルの読み込みに失敗した場合
   */
  public static Map<String, CompileConfig> readConfig(Path configFilePath, String language)
      throws IOException {
    Data data;
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(configFilePath.toString()))) {
      data = gson.fromJson(jr, Data.class);
    }
    data.configurations.forEach(config -> config.libPaths =
        resolvePathList(config.libPaths, configFilePath.getParent(), language));
    data.configurations.forEach(config -> config.extensionPaths =
        resolvePathList(config.extensionPaths, configFilePath.getParent(), language));

    return data.configurations.stream()
      .collect(Collectors.toMap(CompileConfig::getId, Function.identity()));
  }

  private static Path resolvePlaceHolder(String path, String language) {
    String[] dirs = path.split("/");
    for (int i = 0; i < dirs.length; ++i) {
      if (dirs[i].equals(LANGUAGE_PLACEHOLDER)) {
        dirs[i] = language;
      }
    }
    return Paths.get("", dirs);
  }

  /** デシリアライズしたデータを格納するためのクラス. */
  private static class Data {
    private final ArrayList<CompileConfig> configurations;

    /** コンストラクタ (デシリアライズ用). */
    private Data() {
      configurations = new ArrayList<>();
    }
  }
}
