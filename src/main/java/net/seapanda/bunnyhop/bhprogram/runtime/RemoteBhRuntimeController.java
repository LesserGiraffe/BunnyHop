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
 * リモートで動作する BhRuntime (BhProgram の実行環境) の操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface RemoteBhRuntimeController {
  
  /**
   * BhRuntime を起動して BhProgram を実行する.
   *
   * @param filePath 実行するファイルのパス
   * @param hostName BhRuntime を起動するマシンのホスト名.
   * @param uname BhRuntime を起動するマシンにログインする際のユーザ名
   * @param password BhRuntime を起動するマシンにログインする際のパスワード
   * @return 成功した場合 true
   */
  boolean start(Path filePath, String hostName, String uname, String password);

  /**
   * 現在動作中の BhRuntime を終了する.
   *
   * @param hostName BhRuntime を終了するマシンのホスト名.
   * @param uname BhRuntime を終了するマシンにログインする際のユーザ名
   * @param password BhRuntime を終了するマシンにログインする際のパスワード
   * @return 成功した場合 true
   */
  boolean terminate(String hostName, String uname, String password);

  /**
   * BhRuntime に接続する.
   *
   * @param hostName 接続するマシンのホスト名.
   * @param uname 接続するマシンにログインする際のユーザ名
   * @param password 接続するマシンにログインする際のパスワード
   * @return 成功した場合 true
   */
  boolean connect(String hostName, String uname, String password);

  /** 現在接続中の BhRuntime との接続を切る. */
  boolean disconnect();

  /**
   * 現在接続中の BhRuntime に {@code message} を送る.
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

  /** {@link RemoteBhRuntimeController} に対するイベントハンドラの登録および削除操作を規定したインタフェース. */
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
      RemoteBhRuntimeController ctrl,
      BhProgramMessageCarrier oldCarrier,
      BhProgramMessageCarrier newCarrier) {}    
}
