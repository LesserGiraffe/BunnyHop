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

package net.seapanda.bunnyhop.ui.service.search;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;

/**
 * 複数の要素から {@link SearchQuery} に一致する要素を見つける機能を提供するクラス.
 *
 * @author K.Koike
 */
public class StringSearcher<T> {

  private StringSearcher() {}

  /**
   * {@code text} から {@code query} に一致する部分文字列をすべて見つけて
   * {@link ImmutableCircularList<Substring>} に格納して返す.
   *
   * @param query 検索クエリ
   * @param text この文字列から {@code query} に一致する部分文字列を探す.
   */
  public static ImmutableCircularList<Substring> search(SearchQuery query, String text) {
    return new StringSearcher<>().searchImpl(query, text);
  }

  private ImmutableCircularList<Substring> searchImpl(SearchQuery query, String text) {
    ImmutableCircularList<Substring> result = new ImmutableCircularList<>();
    try {
      String searchWord = query.isRegex() ? query.word() : Pattern.quote(query.word());
      int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
      Pattern pattern = Pattern.compile(searchWord, regexFlag);
      Matcher matcher = pattern.matcher(text);
      List<Substring> results = matcher.results()
          .map(found -> new Substring(found.start(), found.group()))
          .toList();
      result = new ImmutableCircularList<>(results);
      if (!query.findNext()) {
        result.movePrevious(1);
      }
    } catch (PatternSyntaxException e) { /* Do nothing */ }
    return result;
  }

  /**
   * 部分文字列を格納するレコード.
   *
   * @param pos 部分文字列の元の文字列の中の位置
   * @param text 部分文字列
   */
  public record Substring(int pos, String text) {}
}
