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

package net.seapanda.bunnyhop.debugger.model.variable;

import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * デバッガの変数一覧の各要素を表すクラス.
 *
 * @author K.Koike
 */
public class VariableListItem {

  private final CallbackRegistry cbRegistry = new CallbackRegistry();
  public final Variable variable;
  public final long startIdx;
  public final long endIdx;
  /**
   * このオブジェクトが {@link ListVariable} を保持する場合, 対応するリストの範囲に含まれる要素数. <br>
   * このオブジェクトが{@link ScalarVariable} を保持する場合 1.
   */
  public final long numValues;

  /**
   * コンストラクタ.
   *
   * @param variable このオブジェクトに関連する変数情報
   */
  public VariableListItem(ScalarVariable variable) {
    this.variable = variable;
    this.startIdx = -1L;
    this.endIdx = -1L;
    this.numValues = 1L;
    variable.getCallbackRegistry().getOnValueChanged().add(event -> {
      var valChangedEvent = new ValueChangedEvent(this, event.oldVal(), event.newVal());
      cbRegistry.onValueChanged.invoke(valChangedEvent);
    });
  }

  /**
   * コンストラクタ.
   *
   * @param variable このオブジェクトに関連する変数情報
   * @param startIdx このオブジェクトが保持するリストの範囲の先頭インデックス
   * @param endIdx このオブジェクトが保持するリストの範囲の終端インデックス
   */
  public VariableListItem(ListVariable variable, long startIdx, long endIdx) {
    this.variable = variable;
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    this.numValues = Math.max(endIdx - startIdx + 1, 0);
    if (startIdx == endIdx) {
      variable.getCallbackRegistry().getOnValueChanged(startIdx).add(event -> {
        var valChangedEvent = new ValueChangedEvent(this, event.oldVal(), event.newVal());
        cbRegistry.onValueChanged.invoke(valChangedEvent);
      });
    }
  }

  /**
   * このオブジェクトが {@link ListVariable} を保持する場合, 対応するリストの範囲を分割した区間
   * を保持する {@link VariableListItem} を作成する.
   *
   * @return 作成した {@link VariableListItem} オブジェクトのリスト
   */
  public List<VariableListItem> createSubItems() {
    if (!(variable instanceof  ListVariable list) || numValues <= 0)  {
      return new ArrayList<>();
    }
    var maxChildren = BhSettings.Debug.maxListTreeChildren;
    long interval = (long) Math.pow(
        maxChildren,
        Math.floor(Math.log10(numValues - 1) / Math.log10(maxChildren)));
    long numChildren = (numValues + interval - 1) / interval;
    var children = new ArrayList<VariableListItem>((int) numChildren);
    for (long i = 0; i < numChildren; ++i) {
      long start = startIdx + i * interval;
      long end = Math.min(start + interval - 1, endIdx);
      children.add(new VariableListItem(list, start, end));
    }
    return children;
  }

  @Override
  public String toString() {
    if (variable instanceof ScalarVariable scalar) {
      String val = scalar.getValue().orElse("");
      return "%s = %s".formatted(variable.name, val);
    }

    if (variable instanceof ListVariable list) {
      if (startIdx == endIdx) {
        String val = list.getItem(startIdx).map(ListVariable.Item::val).orElse("");
        return "%s [%s] = %s".formatted(variable.name, startIdx, val);
      }
      if (startIdx < endIdx) {
        return "%s [%s ... %s]".formatted(variable.name, startIdx, endIdx);
      }
      return "%s (%s)".formatted(variable.name, TextDefs.Debugger.VarInspection.emptyList.get());
    }
    return "";
  }

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link VariableListItem} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistry {

    /**
     * {@link VariableListItem} が保持する {@link Variable} の値が
     * 変わったときのイベントハンドラを管理するオブジェクト.
     */
    private final ConsumerInvoker<ValueChangedEvent> onValueChanged = new SimpleConsumerInvoker<>();

    /**
     * {@link VariableListItem} が保持する {@link Variable} の値が
     * 変わったときのイベントハンドラのレジストリを取得する.
     *
     * <p>{@link VariableListItem#variable} が {@link ListVariable} である場合,
     * {@link VariableListItem} の保持する範囲が 1 であるときにのみ
     * (つまり {@link VariableListItem#startIdx} = {@link VariableListItem#endIdx} の場合),
     * {@link Variable} の値が変わったと見なされる.
     */
    public ConsumerInvoker<ValueChangedEvent>.Registry getOnValueChanged() {
      return onValueChanged.getRegistry();
    }
  }

  /**
   * {@link VariableListItem} が保持する {@link Variable} の値が変わったときの情報を格納したレコード.
   *
   * @param item 管理している {@link Variable} の値が変わった {@link }VariableListItem}
   * @param oldVal 変更前の値.  存在しない場合 null.
   * @param newVal 変更後の値.  存在しない場合 null.
   */
  public record ValueChangedEvent(VariableListItem item, String oldVal, String newVal) {}
}
