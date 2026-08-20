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

import java.util.SequencedCollection;
import java.util.regex.Pattern;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;

/**
 * 文字列から {@link SearchQuery} に一致する要素を見つける機能を提供するクラス.
 *
 * @author K.Koike
 */
public class StringSearcher {

  private final SearchQuery query;
  private final Pattern pattern;

  /**
   * コンストラクタ.
   *
   * @param query 検索ワード.
   */
  public StringSearcher(SearchQuery query) {
    this.query = query;
    Pattern tmp = null;
    try {
      tmp = query.getPattern();
    } catch (Exception e) {
      /* Do nothing. */
    } finally {
      pattern = tmp;
    }
  }

  /**
   * {@code text} から {@code query} に一致する部分文字列を見つけて
   * {@link ImmutableCircularList} に格納して返す.
   *
   * @param query 検索クエリ
   * @param text この文字列から {@code query} に一致する部分文字列を探す.
   * @param maxResults 取得する検索結果の上限. 負の数を指定すると全ての結果を返す.
   */
  public static ImmutableCircularList<Substring> search(
      SearchQuery query, String text, int maxResults) {
    return new StringSearcher(query).search(text, maxResults);
  }

  /**
   * {@code text} から {@code query} に一致する部分文字列を全て見つけて
   * {@link ImmutableCircularList} に格納して返す.
   *
   * @param query 検索クエリ
   * @param text この文字列から {@code query} に一致する部分文字列を探す.
   */
  public static ImmutableCircularList<Substring> search(SearchQuery query, String text) {
    return search(query, text, -1);
  }

  /**
   * {@code text} から {@code patterns} に一致する部分文字列を見つけて返す.
   *
   * @param pattern このパターンに一致する文字列を見つける
   * @param text この文字列から {@code query} に一致する部分文字列を探す.
   * @param maxResults 取得する検索結果の上限. 負の数を指定すると全ての結果を返す.
   */
  public static SequencedCollection<Substring> search(
      Pattern pattern, String text, int maxResults) {
    long max = maxResults < 0 ? Long.MAX_VALUE : maxResults;
    return pattern.matcher(text).results()
      .limit(max)
      .filter(found -> !found.group().isEmpty())
      .map(found -> new Substring(text, found.start(), found.group()))
      .toList();
  }

  /**
   * {@code text} から {@code patterns} に一致する部分文字列を全て見つけて返す.
   *
   * @param pattern このパターンに一致する文字列を見つける
   * @param text この文字列から {@code query} に一致する部分文字列を探す.
   */
  public static SequencedCollection<Substring> search(Pattern pattern, String text) {
    return search(pattern, text, -1);
  }

  /**
   * {@code text} から, このオブジェクトが持つ検索クエリに一致する部分文字列を見つけて
   * {@link ImmutableCircularList} に格納して返す.
   *
   * @param text この文字列から, このオブジェクトが持つ検索クエリに一致する部分文字列を探す.
   * @param maxResults 取得する検索結果の上限. 負の数を指定すると全ての結果を返す.
   */
  public ImmutableCircularList<Substring> search(String text, int maxResults) {
    if (pattern == null) {
      return new ImmutableCircularList<>();
    }
    SequencedCollection<Substring> results = search(pattern, text, maxResults);
    var result = new ImmutableCircularList<>(results);
    if (!query.isForward()) {
      result.movePrevious(1);
    }
    return result;
  }

  /**
   * {@code text} から, このオブジェクトが持つ検索クエリに一致する部分文字列を全て見つけて
   * {@link ImmutableCircularList} に格納して返す.
   *
   * @param text この文字列から, このオブジェクトが持つ検索クエリに一致する部分文字列を探す.
   */
  public ImmutableCircularList<Substring> search(String text) {
    return this.search(text, -1);
  }

  /** このオブジェクトに対応する検索クエリを返す. */
  public SearchQuery getQuery() {
    return query;
  }
}
