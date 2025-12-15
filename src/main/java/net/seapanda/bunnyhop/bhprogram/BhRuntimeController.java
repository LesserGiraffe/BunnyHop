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

package net.seapanda.bunnyhop.bhprogram;

import java.nio.file.Path;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageCarrier;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;

/**
 * BhRuntime (BhProgram の実行環境) の操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhRuntimeController {

  /**
   * BhRuntime に {@code message} を送る.
   *
   * @param message 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus send(BhProgramMessage message);

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();

  /** {@link BhRuntimeController} に対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  interface CallbackRegistry {

    /** BhRuntime との通信用オブジェクトが置き換わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MessageCarrierRenewedEvent>.Registry getOnMsgCarrierRenewed();

    /** BhRuntime との通信が有効または無効になったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ConnectionEvent>.Registry getOnConnectionConditionChanged();

    /** BhProgram を開始したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<StartEvent>.Registry getOnBhProgramStarted();

    /** BhProgram を終了したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<TerminationEvent>.Registry getOnBhProgramTerminated();
  }

  /**
   * BhRuntime との通信用オブジェクトが置き換わったときの情報を格納したレコード.
   *
   * @param ctrl このオブジェクトが持つ BhRuntime との通信用オブジェクトが新しいものと置き換わった
   * @param oldCarrier {@code newCarrier} と置き換わる前の通信用オブジェクト
   * @param newCarrier {@code oldCarrier} と置き換わった通信用オブジェクト
   */
  record MessageCarrierRenewedEvent(
      BhRuntimeController ctrl,
      BhProgramMessageCarrier oldCarrier,
      BhProgramMessageCarrier newCarrier) {}

  /**
   * BhRuntime との通信が有効または無効になったときの情報を格納したレコード.
   *
   * @param ctrl 通信が有効または無効になった {@link BhRuntimeController} オブジェクト
   * @param isConnected BhRuntime との通信が有効になった場合 true, 無効になった場合 false.
   */
  record ConnectionEvent(BhRuntimeController ctrl, boolean isConnected) {}

  /**
   * BhProgram が開始されたときの情報を格納したレコード.
   *
   * @param ctrl BhProgram を開始した {@link BhRuntimeController} オブジェクト
   * @param path 開始された BhProgram のファイルパス.
   */
  record StartEvent(BhRuntimeController ctrl, Path path) {}

  /**
   * BhProgram を終了したときの情報を格納したレコード.
   *
   * @param ctrl BhProgram を終了した {@link BhRuntimeController} オブジェクト
   */
  record TerminationEvent(BhRuntimeController ctrl) {}
}
