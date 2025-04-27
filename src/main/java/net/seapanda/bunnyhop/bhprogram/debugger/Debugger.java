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

import java.util.function.Consumer;

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
  EventManager getEventManager();

  // requestStackFrame

  // stopThread

  /**
   * デバッガに対するイベントハンドラの登録および削除操作を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link Debugger} を "target" と呼ぶ
   * </p>
   */
  public interface EventManager {
    
    /**
     * "target" が {@link ThreadContext} を取得したときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    void addOnThreadContextGet(Consumer<? super ThreadContext> handler);

    /**
     * "target" が {@link ThreadContext} を取得したときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    void removeOnThreadContextGet(Consumer<? super ThreadContext> handler);  

    /**
     * "target" の持つデバッグ情報がクリアされたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    void addOnCleared(Runnable handler);

    /**
     * "target" の持つデバッグ情報がクリアされたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    void removeOnCleared(Runnable handler);
  }
}
