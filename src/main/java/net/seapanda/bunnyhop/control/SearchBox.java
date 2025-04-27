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

package net.seapanda.bunnyhop.control;

import java.util.function.Consumer;

/**
 * 検索クエリを受け取る UI コンポーネントのインタフェース.
 *
 * @author K.Koike
 */
public interface SearchBox {
  
  /**
   * 検索クエリを受け取ったときに実行するイベントハンドラを設定する.
   *
   * @param handler  検索クエリを受け取ったときに実行するイベントハンドラ.
   */
  void setOnSearchRequested(Consumer<Query> handler);

  /** 検索クエリの入力を有効化する. */
  void enable();

  /** 検索クエリの入力を無効化する. */
  void disable();

  /**
   * 検索クエリ.
   *
   * @param word 検索ワード
   * @param isRegex {@code word} を正規表現として解釈する場合 true
   * @param isCaseSensitive {@code word} の大文字, 小文字を区別する場合 true
   * @param findNext 次の一致項目を検索する場合 true
   */
  public record Query(String word, boolean isRegex, boolean isCaseSensitive, boolean findNext) {}
}
