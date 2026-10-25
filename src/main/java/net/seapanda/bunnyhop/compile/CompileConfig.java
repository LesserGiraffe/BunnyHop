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
import net.seapanda.bunnyhop.common.configuration.BhSettings;

/**
 * コンパイルに関する設定を格納するクラス.
 *
 * @author K.Koike
 */
public class CompileConfig {

  private static final String LANGUAGE_PLACEHOLDER = "${language}";

  /** 設定の ID. */
  private String id = "";

  /** コンパイルに使用するライブラリのパス. */
  @SerializedName("libs")
  private List<String> libPaths = new ArrayList<>();

  /** メインルーチンの名前. */
  @SerializedName("mainRoutine")
  private String mainRoutineName = "_bhMain";

  public String getId() {
    return id;
  }

  public List<String> getLibPaths() {
    return libPaths;
  }

  public String getMainRoutineName() {
    return mainRoutineName;
  }

  private void resolveLibPaths(Path base) {
    if (libPaths == null) {
      return;
    }
    libPaths = libPaths.stream()
        .map(CompileConfig::resolvePlaceHolder)
        .map(lib -> base.resolve(lib).normalize().toString())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * {@code configFilePath} で指定された設定ファイルを読み込み, ID と {@link CompileConfig} の対応関係を取得する.
   *
   * @param configFilePath コンパイルに関する設定が書かれたファイルのパス
   * @return ID と {@link CompileConfig} の対応関係を格納したマップ
   * @throws IOException {@code configFilePath} のファイルの読み込みに失敗した場合
   */
  public static Map<String, CompileConfig> readConfig(Path configFilePath)
      throws IOException {
    Data data;
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(configFilePath.toString()))) {
      data = gson.fromJson(jr, Data.class);
    }
    data.configurations.forEach(config -> config.resolveLibPaths(configFilePath.getParent()));

    return data.configurations.stream()
      .collect(Collectors.toMap(CompileConfig::getId, Function.identity()));
  }

  private static Path resolvePlaceHolder(String path) {
    String[] dirs = path.split("/");
    for (int i = 0; i < dirs.length; ++i) {
      if (dirs[i].equals(LANGUAGE_PLACEHOLDER)) {
        dirs[i] = BhSettings.language;
      }
    }
    return Paths.get("", dirs);
  }

  /** デシリアライズしたデータを格納するためのクラス. */
  private static class Data {
    private ArrayList<CompileConfig> configurations;
  }
}
