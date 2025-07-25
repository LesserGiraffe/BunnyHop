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

package net.seapanda.bunnyhop.bhprogram.runtime;

import java.nio.file.Path;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageCarrier;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * ローカルで動作する BhRuntime (BhProgram の実行環境) の操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface LocalBhRuntimeController {

  /**
   * BhRuntime を起動して BhProgram を実行する.
   *
   * @param filePath 実行するファイルのパス
   * @return 成功した場合 true
   */
  boolean start(Path filePath);

  /**
   * 現在動作中の BhRuntime を終了する.
   *
   * @return 成功した場合 true
   */
  boolean terminate();

  /**
   * BhRuntime との通信を有効化する.
   *
   * @return 成功した場合 true
   */
  boolean connect();

  /**
   * BhProgram との通信を無効化する.
   *
   * @return 成功した場合 true
   */
  boolean disconnect();

  /**
   * 現在通信が有効になっている BhProgram に {@code message} を送る.
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

  /** {@link LocalBhRuntimeController} に対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public interface CallbackRegistry {

    /** BhRuntime との通信用オブジェクトが置き換わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MessageCarrierRenewedEvent>.Registry getOnMsgCarrierRenewed();
  }

  /**
   * BhRuntime との通信用オブジェクトが置き換わったときの情報を格納したレコード.
   *
   * @param ctrl このオブジェクトが持つ BhRuntime との通信用オブジェクトが新しいものと置き換わった
   * @param oldCarrier {@code newCarrier} と置き換わる前の通信用オブジェクト
   * @param newCarrier {@code oldCarrier} と置き換わった通信用オブジェクト
   */
  public record MessageCarrierRenewedEvent(
      LocalBhRuntimeController ctrl,
      BhProgramMessageCarrier oldCarrier,
      BhProgramMessageCarrier newCarrier) {}  
}
