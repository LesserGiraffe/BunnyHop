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

package net.seapanda.bunnyhop.bhprogram.debugger.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.bhprogram.common.message.variable.ListVariable.Slice;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * リスト変数の情報を格納するクラス.
 *
 * @author K.Koike
 */
public class ListVariable extends Variable {

  /** リストの長さ. */
  public final int length;
  /** リストのインデックスと値のマップ. */
  private final Map<Long, String> vals = new HashMap<>();

  /** コンストラクタ. */
  public ListVariable(BhNode node, int length) {
    super(node);
    this.length = length;
  }

  /**
   * コンストラクタ.
   *
   * @param node リスト変数に対応する {@link BhNode}
   * @param length リストの長さ
   * @param slices リストが保持する要素. (一部でも良い)
   */
  public ListVariable(BhNode node, int length, Collection<Slice> slices) {
    super(node);
    this.length = length;
    for (Slice slice : slices) {
      for (int i = 0; i < slice.vals().size(); ++i) {
        vals.put(slice.startIdx() + i, slice.vals().get(i));
      }
    }
  }

  /**
   * このオブジェクトに対応するリストの要素を取得する.
   *
   * <p>必ずしも, リストの全ての要素を返す訳ではないことに注意.
   */
  public Collection<Item> getItems() {
    return vals.entrySet().stream()
        .map(entry -> new Item(entry.getKey(), entry.getValue()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * リストの要素を追加する.
   *
   * <p>既に存在するインデックスの値は {@code items} の値で上書きされる.
   *
   * @param items 追加する要素.
   */
  public void addItems(Collection<Item> items) {
    for (Item item : items) {
      vals.put(item.idx(), item.val());
    }
  }

  /**
   * リストの要素を表すレコード.
   *
   * @param idx 要素のインデックス
   * @param val 要素の値
   */
  public record Item(long idx, String val) {}
}
