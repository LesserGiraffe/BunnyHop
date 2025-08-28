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

package net.seapanda.bunnyhop.bhprogram.debugger;

import net.seapanda.bunnyhop.bhprogram.debugger.variable.ListVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.ScalarVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.Variable;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * デバッガの変数一覧の各要素を表すクラス.
 *
 * @author K.Koike
 */
public class VariableListItem {

  private final CallbackRegistry cbRegistry = new CallbackRegistry();
  public final Variable variable;
  private final String alias;
  public final long startIdx;
  public final long endIdx;

  /**
   * コンストラクタ.
   *
   * @param variable このエントリに関連する変数情報
   */
  public VariableListItem(ScalarVariable variable) {
    this.variable = variable;
    this.alias = variable.node.getAlias();
    this.startIdx = -1L;
    this.endIdx = -1L;
    variable.getCallbackRegistry().getOnValueChanged().add(event -> {
      var valChangedEvent = new ValueChangedEvent(this, event.oldVal(), event.newVal());
      cbRegistry.onValueChanged.invoke(valChangedEvent);
    });
  }

  /**
   * コンストラクタ.
   *
   * @param variable このエントリに関連する変数情報
   * @param startIdx このエントリが保持するリストの範囲の先頭インデックス
   * @param endIdx このエントリが保持するリストの範囲の終端インデックス
   */
  public VariableListItem(ListVariable variable, long startIdx, long endIdx) {
    this.variable = variable;
    this.alias = variable.node.getAlias();
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    if (startIdx == endIdx) {
      variable.getCallbackRegistry().getOnValueChanged(startIdx).add(event -> {
        var valChangedEvent = new ValueChangedEvent(this, event.oldVal(), event.newVal());
        cbRegistry.onValueChanged.invoke(valChangedEvent);
      });
    }
  }

  @Override
  public String toString() {
    if (variable instanceof ScalarVariable scalar) {
      return "%s = %s".formatted(alias, scalar.getValue());
    }

    if (variable instanceof ListVariable list) {
      if (startIdx == endIdx) {
        String val = list.getItem(startIdx).map(ListVariable.Item::val).orElse(null);
        if (val == null) {
          return "%s[%s] : %s".formatted(
              alias, startIdx, TextDefs.Debugger.VarInspection.valueNotFound.get());
        }
        val = val.isEmpty() ? TextDefs.Debugger.VarInspection.emptyString.get() : val;
        return "%s[%s] = %s".formatted(alias, startIdx, val);
      }
      return "%s[%s ... %s]".formatted(alias, startIdx, endIdx);
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
    private final ConsumerInvoker<ValueChangedEvent> onValueChanged = new ConsumerInvoker<>();

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
