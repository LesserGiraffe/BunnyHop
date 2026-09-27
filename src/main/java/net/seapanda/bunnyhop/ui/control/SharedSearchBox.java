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

import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.search.SharedSearchBoxDelegate;

/**
 * 検索クエリを受け取る UI コンポーネントのインタフェース.
 *
 * @author K.Koike
 */
public interface SharedSearchBox {

  /** 検索クエリの入力を有効化する. */
  void open(SharedSearchBoxDelegate delegate);

  /** 検索クエリの入力を無効化する. */
  void close();

  /**
   * 同じユーザが同じ検索ハンドラと検索クエリ (次 or 前は除く) で連続して検索された回数を取得する.
   *
   * <p>検索クエリの入力を無効化された場合, この回数はリセットされる.
   *
   * @return 同じ検索ハンドラと検索クエリで連続して検索された回数.
   */
  long getNumConsecutiveSameRequests();

  /**
   * 検索結果を設定する.
   *
   * @param result 検索処理の結果を格納するオブジェクト. (nullable) <br>
   *               null を指定した場合は, 検索結果の表示をクリアする.
   */
  void setSearchResult(SearchQueryResult result);

  /** 検索ボックスの現在の利用者を表すオブジェクトを返す. */
  Object getUser();
}
