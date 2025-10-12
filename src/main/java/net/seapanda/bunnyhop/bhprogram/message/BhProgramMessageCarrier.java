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

package net.seapanda.bunnyhop.bhprogram.message;

import java.util.function.Consumer;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;

/**
 * BhProgramMessage を送受信する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhProgramMessageCarrier {

  /**
   * 接続状態のとき, 引数で指定した {@link BhProgramNotification} を送信キューに追加する.
   *
   * @param notif 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus pushNotification(BhProgramNotification notif);

  /**
   * 引数で指定した {@link BhProgramResponse} を送信キューに追加する.
   *
   * @param resp 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus pushResponse(BhProgramResponse resp);

  /**
   * {@code message} の型により {@link #pushNotification} か {@link #pushResponse} を呼ぶ.
   *
   * @param message 送信データ
   * @return ステータスコード
   * @throws IllegalArgumentException {@code message} が不正なデータであった場合.
   */
  default BhRuntimeStatus pushMessage(BhProgramMessage message) {
    if (message instanceof BhProgramNotification notif) {
      return pushNotification(notif);
    } else if (message instanceof BhProgramResponse resp) {
      return pushResponse(resp);
    }
    throw new IllegalArgumentException("Failed to send an invalid message.\n%s".formatted(message));
  }

  /**
   * このオブジェクトが {@link BhProgramNotification} を受信したときのイベントハンドラを設定する.
   *
   * @param handler 設定するイベントハンドラ.  null を指定した場合, イベントハンドラの設定を解除する.
   */
  void setOnNotifReceived(Consumer<BhProgramNotification> handler);

  /**
   * このオブジェクトが {@link BhProgramResponse} を受信したときのイベントハンドラを設定する.
   *
   * @param handler 設定するイベントハンドラ.  null を指定した場合, イベントハンドラの設定を解除する.
   */
  void setOnRespReceived(Consumer<BhProgramResponse> handler);
}
