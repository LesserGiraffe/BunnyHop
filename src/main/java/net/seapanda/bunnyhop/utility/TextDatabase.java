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

package net.seapanda.bunnyhop.utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * アプリケーションの出力テキスト管理クラス.
 *
 * @author K.Koike
 */
public class TextDatabase {
  
  /** テキストのデータベース. */
  Map<TextId, String> database = new ConcurrentHashMap<>();

  /**
   * {@code filePath} で指定されたファイルからテキストデータのデータベースを構築する.
   *
   * @param filePath テキストデータが定義された JSON ファイルのパス.
   */
  public TextDatabase(Path filePath) throws
      IOException,
      JsonIOException,
      JsonSyntaxException {
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);
      createDatabase(new LinkedList<String>(), jsonObj);
    }
  }

  /**
   * テキストデータを {@link #database} に格納する.
   *
   * @param keyPath トップオブジェクトから {@code value} を参照するまでに参照した key のスタック.
   *                スタックのトップが {@code value} に対応する JSON key となる.
   * @param value {@code key} に対応する JSON value
   */
  private void createDatabase(Deque<String> keyPath, JsonElement value) {
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      var id = TextId.of(new ArrayList<>(keyPath));
      database.put(id, value.getAsString());
      return;
    }
    if (value.isJsonObject()) {
      JsonObject jsonObj = value.getAsJsonObject();
      for (String key : jsonObj.keySet()) {
        keyPath.addLast(key);
        createDatabase(keyPath, jsonObj.get(key));
        keyPath.removeLast();
      }
    }
  }

  /**
   * テキストを取得する.
   *
   * @param id 取得するテキストの ID
   * @param objs 空でない場合, {@code path} で取得したテキストデータに {@link String#format} を適用し,
   *             その引数にこれらのオブジェクトを渡す.
   * @return {@code id} と {@code objs} から作成された文字列.
   *         {@code id} に対応するテキストデータが見つからない場合は, 空の文字列を返す.
   *         {@code objs} によるフォーマットに失敗した場合は,  空の文字列を返す.
   */
  public String get(TextId id, Object... objs) {
    try {
      if (!database.containsKey(id)) {
        return "";
      }
      if (objs.length == 0) {
        return database.get(id);
      }
      return database.get(id).formatted(objs);
    } catch (Exception e) {
      String objsStr = Stream.of(objs)
          .map(obj -> obj.toString())
          .reduce("", (a, b) -> "%s, %s".formatted(a, b));
      System.err.println(String.format(
          "Failed to generate a text.  id = %s, objs = %s\n%s".formatted(id, objsStr, e)));
      return "";
    }
  }

  /**
   * テキストを取得する.
   *
   * @param path このリストの要素を JSON key として JSON のトップオブジェクトから順に JSON value を参照したときの
   *             最後の JSON value に対応するテキストデータを取得する.
   * 
   * @param objs 空でない場合, {@code path} で取得したテキストデータに {@link String#format} を適用し,
   *             その引数にこれらのオブジェクトを渡す.
   * @return {@code path} と {@code objs} から作成された文字列.
   *         {@code path} に対応するテキストデータが見つからない場合は, 空の文字列を返す.
   *         {@code objs} によるフォーマットに失敗した場合は,  空の文字列を返す.
   */
  public String get(List<String> path, Object... objs) {
    try {
      var id = TextId.of(path);
      if (!database.containsKey(id)) {
        return "";
      }
      if (objs.length == 0) {
        return database.get(id);
      }
      return database.get(id).formatted(objs);
    } catch (Exception e) {
      String pathStr = path.stream().reduce("", (a, b) -> "%s, %s".formatted(a, b));
      String objsStr = Stream.of(objs)
          .map(obj -> obj.toString())
          .reduce("", (a, b) -> "%s, %s".formatted(a, b));      
      System.err.println(String.format(
          "Failed to generate a text.  id = %s, objs = %s\n%s".formatted(pathStr, objsStr, e)));
      return "";
    }
  }

  /**
   * テキストを取得する.
   *
   * @param path この配列の要素を JSON key として JSON のトップオブジェクトから順に JSON value を参照したときの
   *             最後の JSON value に対応するテキストデータを取得する.
   * @return テキスト
   */
  public String get(String... path) {
    return database.getOrDefault(TextId.of(path), "");
  }


  /** JSON フォーマットで定義されたテキストデータの識別子. */
  public static class TextId {

    /** テキストデータの ID が存在しないことを表すオブジェクト. */
    public static final TextId NONE = TextId.of();

    private final List<String> path;

    private TextId(List<String> path) {
      if (path == null) {
        path = new ArrayList<String>();
      }
      this.path = path;
    }

    private TextId(String... path) {
      if (path == null) {
        path = new String[] {};
      }
      this.path = List.of(path);
    }

    /**
     * {@link TextId} を作成する.
     *
     * @param path このリストの要素を JSON key として JSON のトップオブジェクトから順に JSON value を参照したときの
     *             最後の JSON value に対応する ID が作成される.
     * @return {@link TextId} オブジェクト.
     */
    public static TextId of(List<String> path) {
      return new TextId(path);
    }

    /**
     * {@link TextId} を作成する.
     *
     * @param path この配列の要素を JSON key として JSON のトップオブジェクトから順に JSON value を参照したときの
     *             最後の JSON value に対応する ID が作成される.
     * @return {@link TextId} オブジェクト.
     */
    public static TextId of(String... path) {
      return new TextId(path);
    }

    @Override
    public String toString() {
      return path.stream().reduce("", (a, b) -> "%s.%s".formatted(a, b));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TextId other) {
        return path.equals(other.path);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }
  }
}
