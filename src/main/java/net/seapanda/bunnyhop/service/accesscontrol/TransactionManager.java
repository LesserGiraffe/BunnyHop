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

package net.seapanda.bunnyhop.service.accesscontrol;

/**
 * アプリケーションにおける 1 つのまとまった処理を規定するための機能を定義したインタフェース. <br>
 *
 * <p>{@link #begin} を呼び出してから {@link #end} を呼び出すまでの処理を「トランザクション」と呼ぶ.
 * このインタフェースの実装クラスは異なるスレッドの「トランザクション」が同時に実行されないことを保証すること.
 *
 *<p>
 * {@link #begin()} と {@link #end()} は対で呼ぶこと.
 *
 * <pre>-- スレッドと処理の設計方針 --
 * モデルを変更する可能性のある処理は必ず UI スレッドで行う.
 * これは, モデルの変更に伴って起きる UI 要素の変更をモデルの変更と不可分に行うためである.
 * UI スレッドで行うべきではない重い処理は Future パターンなどを用いて別のスレッドで行い, UI スレッドを適宜開放する.
 * UI スレッド以外はモデルの参照のみ行う.
 * </pre>
 *
 * @author K.Koike
 */
public interface TransactionManager {

  /**
   * トランザクションを開始する.
   *
   * @return トランザクションに紐づく情報を格納したオブジェクト
   */
  TransactionContext begin();

  /** トランザクションを終了する. */
  void end();
}
