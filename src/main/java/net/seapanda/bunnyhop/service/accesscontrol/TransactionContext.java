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

import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;

/**
 * トランザクションに紐づく情報.
 *
 * @author K.Koike
 */
public class TransactionContext {

  private final UserOperation userOpe;

  final ConsumerInvoker<TransactionContext> onTransactionEnding;

  final TransactionNotificationService notifService;

  /**
   * コンストラクタ.
   *
   * @param userOpe undo/redo 用コマンドオブジェクト
   * @param onTransactionEnding トランザクション終了時に呼び出す関数のレジストリ
   */
  TransactionContext(
      TransactionNotificationService notifService,
      UserOperation userOpe,
      ConsumerInvoker<TransactionContext> onTransactionEnding) {
    this.notifService = notifService;
    this.userOpe = userOpe;
    this.onTransactionEnding = onTransactionEnding;
  }

  /** undo/redo 用コマンドオブジェクトを取得する. */
  public UserOperation userOpe() {
    return userOpe;
  }

  /** このコンテキストを管理している {@link TransactionNotificationService} を取得する. */
  public TransactionNotificationService getService() {
    return notifService;
  }

  /**
   * トランザクションが終了する直前に実行する処理のレジストリを取得する.
   *
   * <p>このレジストリに登録した処理の中で, {@link TransactionNotificationService#begin} を呼び出した場合,
   * このコンテキストのトランザクションは, {@link TransactionNotificationService#end} が呼び出されるまで延長される.<br>
   * 延長部分の処理は, それが呼ばれる直前にトランザクションに指定されていた {@link ExclusionId} を
   * {@link TransactionNotificationService#begin(ExclusionId...)} で取得できることが保証される.
   */
  public ConsumerInvoker<TransactionContext>.Registry onTransactionEnding() {
    return onTransactionEnding.getRegistry();
  }
}