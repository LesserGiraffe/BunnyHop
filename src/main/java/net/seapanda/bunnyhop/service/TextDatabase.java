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

import java.util.ArrayList;
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

  private TextDatabase() {}

  /**
   * テキストを取得する.
   *
   * @param id 取得するテキストの ID
   * @param objs 書式指定してある文字列に渡すオブジェクト
   * @return フォーマット済みテキスト
   */
  public String text(TextId id, Object... objs) {
    try {
      return database.get(id).formatted(objs);
    } catch (Exception e) {
      String objsStr = Stream.of(objs)
          .map(obj -> obj.toString())
          .reduce("", (a, b) -> "%s, %s".formatted(a, b));
      BhService.msgPrinter().errForDebug(String.format(
          "Failed to generate a text.  id = %s, objs = %s\n%s".formatted(id, objsStr, e)));
      return "";
    }
  }

  /**
   * テキストを取得する.
   *
   * @param path JSON 内でテキストが定義された位置.
   * @param objs 書式指定してある文字列に渡すオブジェクト
   * @return フォーマット済みテキスト
   */
  public String text(List<String> path, Object... objs) {
    try {
      return database.get(new TextId(path)).formatted(objs);
    } catch (Exception e) {
      String pathStr = path.stream().reduce("", (a, b) -> "%s, %s".formatted(a, b));
      String objsStr = Stream.of(objs)
          .map(obj -> obj.toString())
          .reduce("", (a, b) -> "%s, %s".formatted(a, b));      
      BhService.msgPrinter().errForDebug(String.format(
          "Failed to generate a text.  id = %s, objs = %s\n%s".formatted(pathStr, objsStr, e)));
      return "";
    }
  }

  /** テキストの識別子. */
  public static class TextId {

    private final List<String> path;

    private TextId(List<String> path) {
      if (path == null) {
        path = new ArrayList<String>();
      }
      this.path = path;
    }

    private TextId(String... path) {
      this.path = List.of(path);
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
