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

import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガが持つ機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface Debugger {

  /** デバッガが持つ全ての情報をクリアする. */
  void clear();

  /**
   * このデバッガに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();

  // requestStackFrame

  // stopThread

  /** デバッガに対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public interface CallbackRegistry {
    
    /** {@link ThreadContext} を取得したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ThreadContextGotEvent>.Registry getOnThreadContextGot();

    /** {@link ThreadContext} を取得したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ClearEvent>.Registry getOnCleared();
  }

  /**
   * デバッガが {@link ThreadContext} を取得したときの情報を格納したレコード.
   *
   * @param debugger {@code context} を取得したデバッガ
   * @param context {@code debugger} が取得した {@link ThreadContext}
   */
  public record ThreadContextGotEvent(Debugger debugger, ThreadContext context) {}

  /**
   * デバッガの持つ情報がクリアされたときの情報を格納したレコード.
   *
   * @param debugger 保持する情報をクリアされたデバッガ
   */
  public record ClearEvent(Debugger debugger) {}
}
