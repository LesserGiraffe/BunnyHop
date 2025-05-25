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

import java.io.BufferedReader;
import java.io.IOException;
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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.service.LogManager;

/**
 * BhProgram の実行環境を操作するクラスが共通で持つ機能と変数をまとめたクラス.
 *
 * @author K.Koike
 */
class BhRuntimeHelper {

  /**
   * RMI オブジェクトを探す.
   *
   * @param hostname リモートオブジェクトのIPアドレス
   * @param port RMIレジストリのポート
   * @param name オブジェクトバインド時の名前
   * @return Remoteオブジェクト
   */
  private static Remote findRemoteObj(String hostname, int port, String name)
      throws MalformedURLException, NotBoundException, RemoteException {
    return Naming.lookup("rmi://%s:%s/%s".formatted(hostname, port, name));
  }

  /**
   * 引数で指定したプロセスの終了処理を待つ.
   *
   * @param process 終わらせるプロセス (null 不可)
   * @param timeout 終了待ちタイムアウト時間 (ms)
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
      success = process.waitFor(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LogManager.logger().error("Failed to wait for the process to end.\n" + e);
    }
    return success;
  }

  /**
   * BhProgram が公開する RMI オブジェクトを取得する.
   *
   * @param timeout タイムアウト (ms)
   */
  static BhRuntimeFacade getBhRuntimeFacade(String hostname, BufferedReader br, int timeout)
      throws IOException,
      TimeoutException, 
      MalformedURLException, 
      NotBoundException, 
      RemoteException {
    String portStr = getSuffixedLine(
        br,
        BhConstants.BhRuntime.RMI_TCP_PORT_SUFFIX,
        timeout);
    int port = Integer.parseInt(portStr);
    // リモートオブジェクト取得
    return (BhRuntimeFacade) findRemoteObj(
        hostname, port, BhRuntimeFacade.class.getSimpleName());
  }

  /**
   * BhProgram を実行する.
   *
   * @param fileName 実行するプログラムが書かれたファイルの名前
   * @param facade BhRemote との通信用オブジェクト
   * @return 成功した場合 true, 失敗した場合 false
   */
  public static boolean runScript(String fileName, BhRuntimeFacade facade)
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
   * @param timeout 読み取りを試みる時間 (ms)
   * @return 引数で指定したサフィックスが付いた1行からサフィックスを取り除いた文字列
   * @throws IOException 入出力エラーが発生した際の例外
   * @throws TimeoutException タイムアウトした際の例外
   */
  private static String getSuffixedLine(BufferedReader br, String suffix, long timeout)
      throws IOException, TimeoutException {
    String readStr = "";
    long begin = System.currentTimeMillis();
    List<Character> charCodeList = new ArrayList<>();
    try {
      while (true) {
        if ((System.currentTimeMillis() - begin) > timeout) {
          throw new TimeoutException("getSuffixedLine timeout");
        }
        if (br.ready()) {  // 次の読み出し結果が EOF の場合 false
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

            default:  // 改行以外の文字コード
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
