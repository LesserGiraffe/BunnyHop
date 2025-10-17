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
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;

/**
 * 複数の要素から {@link SearchQuery} に一致する要素を見つける機能を提供するクラス.
 *
 * @author K.Koike
 */
public class ItemSearcher<T> {

  private ItemSearcher() {}

  /**
   * {@code items} から {@code query} に一致する要素をすべて見つけて {@link ImmutableCircularList<T>} に格納して返す.
   *
   * @param query 検索クエリ
   * @param items このコレクションの中から {@code query} に一致する要素を探す.
   * @param toString {@code items} の各要素から文字列を取得するための関数オブジェクト
   */
  public static <T> ImmutableCircularList<T> search(
      SearchQuery query, SequencedCollection<T> items, Function<T, String> toString) {
    return new ItemSearcher<T>().searchImpl(query, items, toString);
  }

  private ImmutableCircularList<T> searchImpl(
      SearchQuery query, SequencedCollection<T> items, Function<T, String> toString) {
    ImmutableCircularList<T> result = new ImmutableCircularList<>();
    try {
      String searchWord = query.isRegex() ? query.word() : Pattern.quote(query.word());
      int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
      Pattern pattern = Pattern.compile(searchWord, regexFlag);
      List<T> results = items.stream()
          .filter(item -> pattern.matcher(toString.apply(item)).find())
          .toList();
      result = new ImmutableCircularList<>(results);
      if (!query.findNext()) {
        result.movePrevious(1);
      }
    } catch (PatternSyntaxException e) { /* Do nothing */ }
    return result;
  }
}
