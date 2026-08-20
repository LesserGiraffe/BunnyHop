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

package net.seapanda.bunnyhop.search;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 検索クエリを表すクラス.
 *
 * @author koike
 */
public class SearchQuery {

  private final String word;
  private final boolean isRegex;
  private final boolean isCaseSensitive;
  private final boolean isForward;
  private Pattern pattern;

  /**
   * コンストラクタ.
   *
   * @param word 検索ワード
   * @param isRegex {@code word} を正規表現として解釈する場合 true
   * @param isCaseSensitive {@code word} の大文字, 小文字を区別する場合 true
   * @param isForward 次の一致項目を検索する場合 true
   */
  public SearchQuery(String word, boolean isRegex, boolean isCaseSensitive, boolean isForward) {
    Objects.requireNonNull(word);
    this.word = word;
    this.isRegex = isRegex;
    this.isCaseSensitive = isCaseSensitive;
    this.isForward = isForward;
  }

  /** 検索ワードを返す. */
  public String getWord() {
    return word;
  }

  /** 検索ワードを正規表現として解釈する場合 true を返す. */
  public boolean isRegex() {
    return isRegex;
  }

  /** 検索ワードの大文字, 小文字を区別する場合 true を返す. */
  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  /** 次の一致項目を検索する場合 true を返す. */
  public boolean isForward() {
    return isForward;
  }

  /** 検索語句が存在しない場合 true を返す. */
  public boolean isEmpty() {
    return word.isEmpty();
  }

  /**
   * このクエリに対応する検索用の {@link Pattern} を返す.
   *
   * @return 検索条件を表す {@link Pattern}
   * @throws PatternSyntaxException {@link #isRegex()} が {@code true} かつ検索ワードが不正な正規表現の場合
   */
  public Pattern getPattern() throws PatternSyntaxException {
    if (pattern == null) {
      String searchWord = isRegex ? word : Pattern.quote(word);
      int regexFlag = isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
      pattern = Pattern.compile(searchWord, regexFlag);
    }
    return pattern;
  }

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
