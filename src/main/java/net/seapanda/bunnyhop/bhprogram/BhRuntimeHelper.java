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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.seapanda.bunnyhop.bhprogram.common.BhRuntimeFacade;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent.Name;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageDispatcher;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageProcessor;
import net.seapanda.bunnyhop.bhprogram.message.BhRuntimeTransceiver;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;

/**
 * BhProgram の実行環境を操作するクラスが共通で持つ機能と変数をまとめたクラス.
 *
 * @author K.Koike
 */
class BhRuntimeHelper {

  /** BhProgram の実行環境から受信したデータを処理するオブジェクト. */
  private final BhProgramMessageDispatcher dispatcher;
  /** アプリケーションユーザにメッセージを出力するためのオブジェクト. */
  private final MessageService msgService;

  public BhRuntimeHelper(
      BhProgramMessageProcessor msgProcessor,
      SimulatorCmdProcessor simCmdProcessor,
      MessageService msgService) {
    dispatcher = new BhProgramMessageDispatcher(msgProcessor, simCmdProcessor);
    this.msgService = msgService;
  }

  /**
   * RMI オブジェクトを探す.
   *
   * @param ipAddr リモートオブジェクトのIPアドレス
   * @param port RMIレジストリのポート
   * @param name オブジェクトバインド時の名前
   * @return Remoteオブジェクト
   */
  Remote findRemoteObj(String ipAddr, int port, String name)
      throws MalformedURLException, NotBoundException, RemoteException {
    return Naming.lookup("rmi://%s:%s/%s".formatted(ipAddr, port, name));
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 成功した場合 true
   */
  boolean connect() {
    var xcvr = dispatcher.getTransceiver();
    if (xcvr.isEmpty()) {
      msgService.error(TextDefs.BhRuntime.Communication.noRuntimeToConnectTo.get());
      return false;
    }
    return xcvr.get().connect();
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 成功した場合 true
   */
  boolean disconnect() {
    var xcvr = dispatcher.getTransceiver();
    if (xcvr.isEmpty()) {
      msgService.error(TextDefs.BhRuntime.Communication.noRuntimeToDisconnectFrom.get());
      return false;
    }
    return xcvr.get().disconnect();
  }

  /**
   * 引数で指定した {@link BhProgramNotification} を BhProgram の実行環境に送る.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus send(BhProgramNotification notif) {
    return dispatcher.getTransceiver()
        .map(xcvr -> xcvr.pushSendNotif(notif))
        .orElse(BhRuntimeStatus.SEND_WHEN_DISCONNECTED);
  }

  /**
   * 引数で指定したプロセスの終了処理を待つ.
   *
   * @param process 終わらせるプロセスのオブジェクト (nullだめ)
   * @param timeout 終了待ちタイムアウト時間 (sec)
   * @return プロセスを正常に終了できた場合true.
   */
  static boolean killProcess(Process process, int timeout) {
    boolean success = closeStreams(process);
    process.destroy();
    success &= waitForProcessEnd(process, timeout);
    return success;
  }

  private static boolean closeStreams(Process process) {
    boolean success = true;
    try {
      process.getErrorStream().close();
      process.getInputStream().close();
      process.getOutputStream().close();
    } catch (IOException e) {
      LogManager.logger().error("Failed to close the IO stream.\n" + e);
      success = false;
    }
    return success;
  }

  private static boolean waitForProcessEnd(Process process, int timeout) {
    boolean success = true;
    try {
      success = process.waitFor(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LogManager.logger().error("Failed to wait for the process to end.\n" + e);
    }
    return success;
  }

  /**
   * BhProgramTransceiver の処理を終了する.
   *
   * @return 終了処理が成功した場合 true. トランシーバが登録されていない場合も true を返す.
   */
  boolean haltTransceiver() {
    return dispatcher.getTransceiver()
        .map(xcvr -> xcvr.halt())
        .orElse(true);
  }

  /**
   * BhProgram の実行環境に BhProgram の開始を命令する.
   *
   * @param fileName BhProgram のファイル名
   * @param ipAddr BhProgram の実行環境が動作しているマシンのIPアドレス
   * @param is BhProgram 実行環境からの出力を受け取るための InputStream
   * @return 正常に BhProgram を開始できた場合 true
   */
  boolean runBhProgram(String fileName, String ipAddr, InputStream is) {
    msgService.info(TextDefs.BhRuntime.Communication.preparingToCommunicate.get());
    boolean success = true;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      BhRuntimeFacade facade = getBhRuntimeFacade(ipAddr, br);
      var transceiver = new BhRuntimeTransceiver(facade, msgService);
      dispatcher.replaceTransceiver(transceiver).ifPresent(old -> old.halt());
      transceiver.start();
      success &= transceiver.connect();
      success &= runScript(fileName, facade);
    } catch (IOException | NotBoundException | NumberFormatException | TimeoutException e) {
      LogManager.logger().error("Failed to run BhProgram\n" + e);
      success &= false;
    }
    if (!success) {
      msgService.info(TextDefs.BhRuntime.Communication.failedToEstablishConnection.get());
    }
    return success;
  }

  /** BhProgram が公開する RMI オブジェクトを取得する. */
  private BhRuntimeFacade getBhRuntimeFacade(String ipAddr, BufferedReader br)
      throws IOException,
      TimeoutException, 
      MalformedURLException, 
      NotBoundException, 
      RemoteException {
    String portStr = getSuffixedLine(
        br,
        BhConstants.BhRuntime.RMI_TCP_PORT_SUFFIX,
        BhConstants.BhRuntime.TCP_PORT_READ_TIMEOUT);
    int port = Integer.parseInt(portStr);
    // リモートオブジェクト取得
    var facade = (BhRuntimeFacade) findRemoteObj(
        ipAddr, port, BhRuntimeFacade.class.getSimpleName());
    return facade;
  }

  /** BhProgram の main メソッドを実行する. */
  private boolean runScript(String fileName, BhRuntimeFacade facade)
      throws RemoteException {
    var startEvent = new BhProgramEvent(
        Name.PROGRAM_START, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    return facade.runScript(fileName, startEvent);
  }

  /**
   * <pre>
   * 引数で指定したサフィックスが付いた1行をBufferedReaderから読み込んで, サフィックスを取り除いて返す.
   * 指定したサフィックスが付いていな行は読み飛ばす.
   * EOFのある行は判定対象外.
   * </pre>
   *
   * @param br このオブジェクトからテキストを読み出す
   * @param suffix このサフィックスが付いた行を返す
   * @param timeout 読み取りを試みる時間 (sec)
   * @return 引数で指定したサフィックスが付いた1行からサフィックスを取り除いた文字列
   * @throws IOException 入出力エラーが発生した際の例外
   * @throws TimeoutException タイムアウトした際の例外
   */
  private static String getSuffixedLine(BufferedReader br, String suffix, long timeout)
      throws IOException, TimeoutException {
    timeout *= 1000;
    String readStr = "";
    long begin = System.currentTimeMillis();
    List<Character> charCodeList = new ArrayList<>();
    try {
      while (true) {
        if ((System.currentTimeMillis() - begin) > timeout) {
          throw new TimeoutException("getSuffixedLine timeout");
        }
        if (br.ready()) {  //次の読み出し結果がEOFの場合 false
          int charCode = br.read();
          switch (charCode) {
            case '\r':
            case '\n':
              char[] charCodeArray = new char[charCodeList.size()];
              for (int i = 0; i < charCodeArray.length; ++i) {
                charCodeArray[i] = charCodeList.get(i);
              }
              readStr = new String(charCodeArray);  //サイズ0の配列の場合 readStr == '\0'
              charCodeList.clear();
              break;

            default:  //改行以外の文字コード
              charCodeList.add((char) charCode);
              continue;
          }
        } else {
          Thread.sleep(100);
          continue;
        }

        if (readStr.endsWith(suffix)) {
          break;
        }
      }
    } catch (InterruptedException  e) { /* do nothing */ }
    readStr = readStr.substring(0, readStr.length() - suffix.length());
    return readStr;
  }
}
