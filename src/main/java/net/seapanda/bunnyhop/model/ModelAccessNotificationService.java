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

package net.seapanda.bunnyhop.model;

import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * モデル (ノード, ワークスペース等) へのアクセスの開始と終了を通知するメソッドを定義したインタフェース. <br>
 * {@link #beginWrite} と {@link #endWrite} は対で呼ぶこと. <br>
 * {@link #beginRead} と {@link #endRead} は対で呼ぶこと.
 *
 * <pre>-- スレッドと処理の設計方針 --
 * モデルを変更する可能性のある処理は必ず UI スレッドで行う.
 * これは, モデルの変更に伴って起きる UI 要素の変更をモデルの変更と不可分に行うためである.
 * UI スレッドで行うべきではない重い処理は Future パターンなどを用いて別のスレッドで行い, UI スレッドを適宜開放する.
 * その際, 複数のスレッドに分割した処理と排他的に実行しなければならない処理が存在しないか注意する.
 * UI スレッド以外はモデルの参照のみ行う.
 * </pre>
 *
 * @author K.Koike
 */
public interface ModelAccessNotificationService {

  /** モデルの変更開始をこのオブジェクトに通知する. */
  Context beginWrite();

  /** モデルの変更終了をこのオブジェクトに通知する. */
  void endWrite();

  /** モデルの参照開始をこのオブジェクトに通知する. */
  void beginRead();

  /** モデルの参照終了をこのオブジェクトに通知する. */
  void endRead();


  /** モデルの変更に紐づく情報. */
  record Context(UserOperation userOpe) { }
}
