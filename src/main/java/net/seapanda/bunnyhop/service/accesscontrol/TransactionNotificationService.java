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

import java.util.Optional;

/**
 * アプリケーションにおける 1 つのまとまった処理を規定するための機能を定義したインタフェース. <br>
 *
 * <p>{@link #begin} を呼び出してから {@link #end} を呼び出すまでの処理を「トランザクション」と呼ぶ.
 * このインタフェースの実装クラスは異なるスレッドの「トランザクション」が同時に実行されないことを保証すること.
 *
 *<p>
 * {@link #begin()} と {@link #end()} は対で呼ぶこと. <br>
 * {@link #begin(ExclusionId...)} が成功した場合 {@link #end(ExclusionId...)} を同じパラメータを指定して呼ぶこと. <br>
 *
 * <pre>-- スレッドと処理の設計方針 --
 * モデルを変更する可能性のある処理は必ず UI スレッドで行う.
 * これは, モデルの変更に伴って起きる UI 要素の変更をモデルの変更と不可分に行うためである.
 * UI スレッドで行うべきではない重い処理は Future パターンなどを用いて別のスレッドで行い, UI スレッドを適宜開放する.
 * その際, 異なるスレッドに分割した処理と排他的に実行しなければならない処理が存在しないか注意する.
 * UI スレッド以外はモデルの参照のみ行う.
 * </pre>
 *
 * @author K.Koike
 */
public interface TransactionNotificationService {

  /**
   * トランザクションの開始をこのオブジェクトに通知する.
   *
   * <p>このメソッドは, {@code ids} に指定した {@link ExclusionId} に関連付けられたトランザクションを開始する.
   * このメソッドが {@code ids} に関連付けられたトランザクションが既に開始されているとき, このメソッドは失敗する.
   * このメソッドが失敗したとき {@link #end(ExclusionId...)} は呼ばないこと.
   *
   * @param ids トランザクションに関連付ける {@link ExclusionId}
   * @return トランザクションに紐づく情報を格納したオブジェクト.  失敗した場合は empty.
   */
  Optional<TransactionContext> begin(ExclusionId... ids);

  /** トランザクションの開始をこのオブジェクトに通知する. */
  TransactionContext begin();

  /**
   * トランザクションの終了をこのオブジェクトに通知する.
   *
   * @param ids トランザクションとの関連付けを解除する {@link ExclusionId}
   */
  void end(ExclusionId... ids);

  /** トランザクションの終了をこのオブジェクトに通知する. */
  void end();

}
