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

package net.seapanda.bunnyhop.ui.control;

import java.util.function.Function;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.ui.model.SearchQueryResult;

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
  void setOnSearchRequested(Function<? super SearchQuery, ? extends SearchQueryResult> handler);

  /**
   * 検索クエリを受け取ったときに実行するイベントハンドラを解除する.
   *
   * @param handler このハンドラがこのオブジェクトに設定されている場合, 設定を解除する.
   *                そうでない場合何もしない.
   * @return 設定を解除した場合 true, 何もしなかった場合 false
   */
  boolean unsetOnSearchRequested(Object handler);

  /** 検索クエリの入力を有効化する. */
  void enable();

  /** 検索クエリの入力を無効化する. */
  void disable();

  /**
   * 同じ検索ハンドラと検索クエリ (次 or 前は除く) で連続して検索された回数を取得する.
   *
   * <p>検索クエリの入力を無効化された場合, この回数はリセットされる.
   *
   * @return 同じ検索ハンドラと検索クエリで連続して検索された回数.
   */
  long getNumConsecutiveSameRequests();

  /** 検索結果をクリアする. */
  void clearSearchResult();
}
