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
 * モデル (ノード, ワークスペース等) へのアクセスの開始と終了を通知するメソッドを定義したインタフェース.
 * {@link #begin} と {@link #end} は対で呼ぶこと.
 *
 * @author K.Koike
 */
public interface ModelAccessNotificationService {

  /** モデルへのアクセス開始をこのオブジェクトに通知する. */
  Context begin();

  /** モデルへのアクセス終了をこのオブジェクトに通知する. */
  void end();

  /** モデルへアクセスする処理に紐づく情報. */
  record Context(UserOperation userOpe) { }
}
