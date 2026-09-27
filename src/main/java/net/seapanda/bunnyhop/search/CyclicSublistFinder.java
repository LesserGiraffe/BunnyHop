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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * あるリストの中の位置を指定して, それに最も近い位置にある, そのリストの部分リストの要素を探す機能を提供するクラス.
 *
 * <p>
 * 元のリストを B, その部分リストを A とする. この 2 つのリストは以下の条件を満たすこと.
 * <ul>
 *   <li>A ⊆ B である.
 *   <li>A の任意の 2 つの要素の前後関係は B の中でのそれと一致する.
 *   <li>A と B はそれぞれ同じ要素を 2 つ以上持たない.
 * </ul>
 *
 * <p>
 * このとき, B に対しインデックス i を指定して, 以下の条件を満たす a ∈ A を探す.
 *
 * <p>
 * 条件 : B[j] == a としたとき i から j までの循環距離が最小となる.
 *       循環距離とは B を有向循環リストと考えたときに, i から j まで進むのに必要な移動回数である.
 *       進む向きは探索を行うメソッドごとに決まっており,
 *       {@link #getItemAfter} はインデックスが増加する向き,
 *       {@link #getItemBefore} はインデックスが減少する向きである.
 *
 * @author K.Koike
 */
public class CyclicSublistFinder<T> {

  /** クラスコメントの A に相当する部分リスト. */
  private final List<T> sublist;
  /** 部分リストの各要素と, 部分リストの中でのインデックスの対応. */
  private final SequencedMap<T, Integer> sublistItemToIndex;
  /** 元のリストの各要素と, 元のリストの中でのインデックスの対応. */
  private final SequencedMap<T, Integer> srcListItemToIndex;

  /**
   * コンストラクタ.
   *
   * <p>
   * 渡されたリストの内容はこのオブジェクトにコピーされるので,
   * このオブジェクトを作成した後にリストを変更してもこのオブジェクトの動作は変わらない.
   *
   * @param sublist クラスコメントの A に相当する部分リスト
   * @param srcList クラスコメントの B に相当する元のリスト
   */
  public CyclicSublistFinder(List<T> sublist, List<T> srcList) {
    this.sublist = new ArrayList<>(sublist);
    sublistItemToIndex = createListItemToIndex(sublist);
    srcListItemToIndex = createListItemToIndex(srcList);
  }

  /** {@code list} の各要素と, {@code list} の中でのその要素のインデックスの対応を作成して返す. */
  private SequencedMap<T, Integer> createListItemToIndex(List<T> list) {
    var listItemToIndex = new LinkedHashMap<T, Integer>();
    for (int i = 0; i < list.size(); ++i) {
      listItemToIndex.put(list.get(i), i);
    }
    return listItemToIndex;
  }

  /**
   * 元のリストのインデックス {@code idx} からインデックスが増加する向きに見て, 最初に見つかる部分リストの要素を返す.
   *
   * <p>
   * {@code idx} より後ろに部分リストの要素が無い場合は, 元のリストの先頭に戻って探索を続ける.
   *
   * @param idx 元のリストの中でのインデックス. 元のリストのインデックスの範囲外の値を指定してもよい.
   * @return 見つかった要素. 部分リストが空の場合 {@link Optional#empty()}.
   */
  public Optional<Found<T>> getItemAfter(int idx) {
    if (sublist.isEmpty()) {
      return Optional.empty();
    }
    int lo = 0;
    int hi = sublist.size() - 1;
    T item = sublist.getFirst();
    while (lo <= hi) {
      int mid = (lo + hi) / 2;
      int idxInSrcList = srcListItemToIndex.get(sublist.get(mid));
      if (idxInSrcList > idx) {
        item = sublist.get(mid);
        hi = mid - 1;
      } else {
        lo = mid + 1;
      }
    }
    return Optional.of(
        new Found<>(item, sublistItemToIndex.get(item), srcListItemToIndex.get(item)));
  }

  /**
   * 元のリストの要素 {@code item} からインデックスが増加する向きに見て, 最初に見つかる部分リストの要素を返す.
   *
   * <p>
   * {@code item} より後ろに部分リストの要素が無い場合は, 元のリストの先頭に戻って探索を続ける.
   *
   * @param item 元のリストの要素. (nullable) <br>
   *             null を指定した場合, 元のリストの最後の要素を起点に検索をする.
   *
   * @return 見つかった要素. 以下の場合は {@link Optional#empty()} を返す. <br>
   *     - {@code item} が元のリストにない <br>
   *     - 部分リストが空である <br>
   */
  public Optional<Found<T>> getItemAfter(T item) {
    if (srcListItemToIndex.isEmpty()) {
      return Optional.empty();
    }
    if (item == null) {
      item = srcListItemToIndex.lastEntry().getKey();
    } else if (!srcListItemToIndex.containsKey(item)) {
      return Optional.empty();
    }
    return getItemAfter(srcListItemToIndex.get(item));
  }

  /**
   * 元のリストのインデックス {@code idx} からインデックスが減少する向きに見て, 最初に見つかる部分リストの要素を返す.
   *
   * <p>
   * {@code idx} より前に部分リストの要素が無い場合は, 元のリストの末尾に戻って探索を続ける.
   *
   * @param idx 元のリストの中でのインデックス. 元のリストのインデックスの範囲外の値を指定してもよい.
   * @return 見つかった要素. 部分リストが空の場合 {@link Optional#empty()}.
   */
  public Optional<Found<T>> getItemBefore(int idx) {
    if (sublist.isEmpty()) {
      return Optional.empty();
    }
    int lo = 0;
    int hi = sublist.size() - 1;
    T item = sublist.getLast();
    while (lo <= hi) {
      int mid = (lo + hi) / 2;
      int idxInSrcList = srcListItemToIndex.get(sublist.get(mid));
      if (idxInSrcList < idx) {
        item = sublist.get(mid);
        lo = mid + 1;
      } else {
        hi = mid - 1;
      }
    }
    return Optional.of(
        new Found<>(item, sublistItemToIndex.get(item), srcListItemToIndex.get(item)));
  }

  /**
   * 元のリストの要素 {@code item} からインデックスが減少する向きに見て, 最初に見つかる部分リストの要素を返す.
   *
   * <p>
   * {@code item} より前に部分リストの要素が無い場合は, 元のリストの末尾に戻って探索を続ける.
   *
   * @param item 元のリストの要素. (nullable) <br>
   *             null を指定した場合, 元のリストの最初の要素を起点に検索をする.
   *
   * @return 見つかった要素. 以下の場合は {@link Optional#empty()} を返す. <br>
   *     - {@code item} が元のリストにない <br>
   *     - 部分リストが空である <br>
   */
  public Optional<Found<T>> getItemBefore(T item) {
    if (srcListItemToIndex.isEmpty()) {
      return Optional.empty();
    }
    if (item == null) {
      item = srcListItemToIndex.firstEntry().getKey();
    } else if (!srcListItemToIndex.containsKey(item)) {
      return Optional.empty();
    }
    return getItemBefore(srcListItemToIndex.get(item));
  }


  /**
   * 探索によって見つかった要素と, その要素が元のリスト及び部分リストの中で
   * それぞれ何番目の位置にあるかを表すインデックスを保持するクラス.
   *
   * @param <T> 保持する要素の型
   */
  public static final class Found<T> {

    private final T item;
    private final int idxInSrcList;
    private final int idxInSublist;

    Found(T item, int idxInSublist, int idxInSrcList) {
      this.item = item;
      this.idxInSrcList = idxInSrcList;
      this.idxInSublist = idxInSublist;
    }

    public T getItem() {
      return item;
    }

    public int getIdxInSrcList() {
      return idxInSrcList;
    }

    public int getIdxInSublist() {
      return idxInSublist;
    }
  }
}
