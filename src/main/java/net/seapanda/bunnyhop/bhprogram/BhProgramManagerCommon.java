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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.seapanda.bunnyhop.bhprogram.common.BhProgramHandler;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent.Name;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageDispatcher;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageProcessor;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramTransceiver;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;

/**
 * BhProgramの実行環境を操作するクラスが共通で持つ機能と変数をまとめたクラス.
 *
 * @author K.Koike
 */
class BhProgramManagerCommon {
  /** BhProgram実行用. */
  private final ExecutorService runBhProgramExec = Executors.newSingleThreadExecutor();
  /** 接続, 切断処理用. */
  private final ExecutorService connectTaskExec = Executors.newSingleThreadExecutor();
  /** プロセス終了用. */
  private final ExecutorService terminationExec = Executors.newSingleThreadExecutor();
  /** BhProgram の実行環境から受信したデータを処理するオブジェクト. */
  private final BhProgramMessageDispatcher dispatcher;

  public BhProgramManagerCommon(SimulatorCmdProcessor simCmdProcessor) {
    dispatcher = new BhProgramMessageDispatcher(new BhProgramMessageProcessor(), simCmdProcessor);
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
   * BhProgramを実行する.
   *
   * @param execFunc BhProgram実行関数
   * @return BhProgram実行タスクのFutureオブジェクト
   */
  Future<Boolean> executeAsync(Callable<Boolean> execFunc) {
    return runBhProgramExec.submit(execFunc);
  }

  /**
   * BhProgramを終了する.
   *
   * @param terminationFunc BhProgram終了関数
   * @return BhProgram終了タスクのFutureオブジェクト
   */
  Future<Boolean> terminateAsync(Callable<Boolean> terminationFunc) {
    return terminationExec.submit(terminationFunc);
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 接続タスクのFutureオブジェクト
   */
  Future<Boolean> connectAsync() {
    var xcvr = dispatcher.getTransceiver();
    if (xcvr.isEmpty()) {
      BhService.msgPrinter().errForUser("!! 接続失敗 (プログラム未実行) !!\n");
      return connectTaskExec.submit(() -> false);
    }
    return connectTaskExec.submit(() -> xcvr.get().connect());
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 切断タスクのFutureオブジェクト
   */
  Future<Boolean> disconnectAsync() {
    var xcvr = dispatcher.getTransceiver();
    if (xcvr.isEmpty()) {
      BhService.msgPrinter().errForUser("!! 切断失敗 (プログラム未実行) !!\n");
      return connectTaskExec.submit(() -> false);
    }
    return connectTaskExec.submit(() -> xcvr.get().disconnect());
  }

  /**
   * 引数で指定した {@link BhProgramMessage} を BhProgram の実行環境に送る.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus sendAsync(BhProgramMessage msg) {
    return dispatcher.getTransceiver()
        .map(xcvr -> xcvr.pushSendMsg(msg))
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
      BhService.msgPrinter().errForDebug("Failed to close the IO stream.\n" + e);
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
      BhService.msgPrinter().errForDebug("Failed to wait for the process to end.\n" + e);
    }
    return success;
  }

  /**
   * このオブジェクトの終了処理をする.
   *
   * @return 終了処理が正常に完了した場合true
   */
  boolean end() {
    runBhProgramExec.shutdownNow();
    connectTaskExec.shutdownNow();
    terminationExec.shutdownNow();
    boolean success = true;
    try {
      success &= runBhProgramExec.awaitTermination(
          BhConstants.EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
      success &= connectTaskExec.awaitTermination(
          BhConstants.EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
      success &= terminationExec.awaitTermination(
          BhConstants.EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      success &= false;
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
    BhService.msgPrinter().infoForUser("-- 通信準備中 --\n");
    boolean success = true;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      BhProgramHandler programHandler = getBhProgramHandler(ipAddr, br);
      var transceiver = new BhProgramTransceiver(programHandler);
      dispatcher.replaceTransceiver(transceiver).ifPresent(old -> old.halt());
      transceiver.start();
      success &= transceiver.connect();
      success &= runScript(fileName, programHandler);
    } catch (IOException | NotBoundException | NumberFormatException | TimeoutException e) {
      BhService.msgPrinter().errForDebug("Failed to run BhProgram\n" + e);
      success &= false;
    }
    if (!success) {
      BhService.msgPrinter().errForUser("!! 通信準備失敗 !!\n");
    }
    return success;
  }

  /** BhProgram が公開する RMI オブジェクトを取得する. */
  private BhProgramHandler getBhProgramHandler(String ipAddr, BufferedReader br)
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
    BhProgramHandler programHandler = (BhProgramHandler) findRemoteObj(
        ipAddr, port, BhProgramHandler.class.getSimpleName());
    return programHandler;
  }

  /** BhProgram の main メソッドを実行する. */
  private boolean runScript(String fileName, BhProgramHandler programHandler)
      throws RemoteException {
    var startEvent = new BhProgramEvent(
        Name.PROGRAM_START, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    return programHandler.runScript(fileName, startEvent);
  }

  /**
   * <pre>
   * 引数で指定したサフィックスが付いた1行をBufferedReaderから読み込んで, サフィックスを取り除いて返す.
   * 指定したサフィックスが付いていな行は読み飛ばす.
   * EOFのある行は判定対象外.
   * </pre>
   *
   * @param br テキストを読み込むbuffered reader
   * @param suffix このサフィックスが付いた行を返す
   * @param timeout 読み取りを試みる時間 (sec)
   * @return 引数で指定したサフィックスが付いた1行からサフィックスを取り除いた文字列
   * @throws IOException 入出力エラーが発生した際の例外
   * @throws TimeoutException タイムアウトした際の例外
   */
  private String getSuffixedLine(BufferedReader br, String suffix, long timeout)
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
