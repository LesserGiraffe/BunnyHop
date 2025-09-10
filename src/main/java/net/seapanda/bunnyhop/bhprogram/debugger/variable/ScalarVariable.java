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

import java.util.Objects;
import java.util.Optional;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * スカラ変数の情報を格納するクラス.
 *
 * @author K.Koike
 */
public class ScalarVariable extends Variable {

  private final CallbackRegistry cbRegistry = new CallbackRegistry();
  /** 変数の値. */
  private volatile String val;

  /**
   * コンストラクタ.
   *
   * @param id 変数の ID
   * @param name 変数名
   * @param node 変数に対応する {@link BhNode}. (nullable)
   */
  public ScalarVariable(BhSymbolId id, String name, BhNode node) {
    this(id, name, node, null);
  }

  /**
   * コンストラクタ.
   *
   * @param id 変数の ID
   * @param name 変数名
   * @param node 変数に対応する {@link BhNode}. (nullable)
   * @param val 変数の値. (nullable)
   */
  public ScalarVariable(BhSymbolId id, String name, BhNode node, String val) {
    super(id, name, node);
    this.val = val;
  }

  /**
   * スカラ変数の値を設定する.
   *
   * @param val 設定するスカラ変数の値. (nullable)
   */
  public synchronized void setValue(String val) {
    if (Objects.equals(this.val, val)) {
      return;
    }
    String oldVal = this.val;
    this.val = val;
    cbRegistry.onValueChanged.invoke(new ValueChangedEvent(this, oldVal, val));
  }

  /** スカラ変数の値を取得する. */
  public synchronized Optional<String> getValue() {
    return Optional.ofNullable(val);
  }

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link ScalarVariable} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistry {

    /** 関連する {@link ScalarVariable} の値が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ValueChangedEvent> onValueChanged = new ConsumerInvoker<>();

    /** 関連する {@link ScalarVariable} の値が変わったときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<ValueChangedEvent>.Registry getOnValueChanged() {
      return onValueChanged.getRegistry();
    }
  }

  /**
   * {@link ScalarVariable} の値が変わったときの情報を格納したレコード.
   *
   * @param variable 値が変わった {@link ScalarVariable}
   * @param oldVal 変更前の値.  存在しない場合 null.
   * @param newVal 変更後の値.  存在しない場合 null.
   */
  public record ValueChangedEvent(ScalarVariable variable, String oldVal, String newVal) {}
}
