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

/**
 * 共有検索ボックスが検索イベントを委譲する相手を規定するインタフェース.
 *
 * @author K.Koike
 */
public interface SharedSearchBoxDelegate {

  /**
   * 検索がリクエストされたときに呼ばれる.
   *
   * @param query 検索条件
   * @return 検索結果. 検索を行わなかった場合は null.
   */
  default SearchQueryResult onSearchRequested(SearchQuery query) {
    return null;
  }

  /** この検索ボックスが閉じられたときに呼ばれる. */
  default void onClosed() {}

  /**
   * この検索ボックスの利用者を表すオブジェクトを返す.
   *
   * <p>検索ボックスはこのメソッドの戻り値を使って, 検索ボックスを共有する複数の利用者を区別する.
   *
   * @return この検索ボックスの利用者を表すオブジェクト
   */
  default Object getUser() {
    return this;
  }
}
