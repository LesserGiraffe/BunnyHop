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

package net.seapanda.bunnyhop.ui.model;

/**
 * 検索クエリ.
 *
 * @param word 検索ワード
 * @param isRegex {@code word} を正規表現として解釈する場合 true
 * @param isCaseSensitive {@code word} の大文字, 小文字を区別する場合 true
 * @param findNext 次の一致項目を検索する場合 true
 */
public record SearchQuery(
    String word,
    boolean isRegex,
    boolean isCaseSensitive,
    boolean findNext) {

  /**
   * {@code findNext} を考慮しない比較.
   */
  public boolean isEqualTo(SearchQuery other) {
    if (other == null) {
      return false;
    }
    return word.equals(other.word)
        && isRegex == other.isRegex
        && isCaseSensitive == other.isCaseSensitive;
  }
}
