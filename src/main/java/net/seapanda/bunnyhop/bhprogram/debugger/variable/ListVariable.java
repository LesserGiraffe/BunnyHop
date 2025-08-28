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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.seapanda.bunnyhop.bhprogram.common.message.variable.ListVariable.Slice;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * リスト変数の情報を格納するクラス.
 *
 * @author K.Koike
 */
public class ListVariable extends Variable {

  private final CallbackRegistry cbRegistry = new CallbackRegistry();
  /** リストの長さ. */
  public final int length;
  /** リストのインデックスと値のマップ. */
  private final Map<Long, Item> idxToItem = new HashMap<>();

  /**
   * コンストラクタ.
   *
   * @param node リスト変数に対応する {@link BhNode}
   * @param length リストの長さ
   */
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
        long idx = slice.startIdx() + i;
        idxToItem.put(idx, new Item(idx, slice.vals().get(i)));
      }
    }
  }

  /**
   * このオブジェクトに対応するリストの要素を取得する.
   *
   * <p>必ずしも, リストの全ての要素を返す訳ではないことに注意.
   */
  public synchronized Collection<Item> getItems() {
    return new ArrayList<>(idxToItem.values());
  }

  /**
   * このオブジェクトが保持するリストの要素の {@code idx} 番目の値を取得する.
   *
   * @param idx 取得したいリストの要素のインデックス
   * @return リストの {@code idx} 番目の要素に対応する {@link Item} オブジェクト.  <br>
   *         このオブジェクトが {@code idx} 番目の情報を持たない場合 empty.
   */
  public synchronized Optional<Item> getItem(long idx) {
    if (!idxToItem.containsKey(idx)) {
      return Optional.empty();
    }
    return Optional.of(idxToItem.get(idx));
  }

  /**
   * リストの要素を追加する.
   *
   * <p>既に存在するインデックスの値は {@code items} の値で上書きされる.
   *
   * @param items 追加する要素.
   */
  public synchronized void addItems(Collection<Item> items) {
    var events = new ArrayList<ValueChangedEvent>();
    for (Item item : items) {
      Item curretItem = idxToItem.get(item.idx);
      if (curretItem != null && Objects.equals(curretItem.val, item.val)) {
        continue;
      }
      idxToItem.put(item.idx(), item);
      String oldVal = (curretItem == null) ? null : curretItem.val;
      events.add(new ValueChangedEvent(this, item.idx, oldVal, item.val));
    }
    for (ValueChangedEvent event : events) {
      if (cbRegistry.idxToOnValueChanged.containsKey(event.idx)) {
        cbRegistry.idxToOnValueChanged.get(event.idx).invoke(event);
      }
    }
  }

  /**
   * リストの要素を表すレコード.
   *
   * @param idx 要素のインデックス
   * @param val 要素の値
   */
  public record Item(long idx, String val) {}

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link ListVariable} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistry {

    /**
     * 関連する {@link ListVariable} の値が変わったときのイベントハンドラを
     * インデックスごとに管理するオブジェクトを格納するマップ.
     */
    private final Map<Long, ConsumerInvoker<ValueChangedEvent>> idxToOnValueChanged =
        new ConcurrentHashMap<>();

    /** 関連する {@link ListVariable} の {@code idx} 番目の要素の値が変わったときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<ValueChangedEvent>.Registry getOnValueChanged(long idx) {
      if (!idxToOnValueChanged.containsKey(idx)) {
        idxToOnValueChanged.put(idx, new ConsumerInvoker<>());
      }
      return idxToOnValueChanged.get(idx).getRegistry();
    }
  }

  /**
   * {@link ListVariable} の値が変わったときの情報を格納したレコード.
   *
   * @param variable 値が変わった {@link ListVariable}
   * @param idx 値が変わった要素のインデックス
   * @param oldVal 変更前の値.  存在しない場合 null.
   * @param newVal 変更後の値.  存在しない場合 null.
   */
  public record ValueChangedEvent(ListVariable variable, long idx, String oldVal, String newVal) {}
}
