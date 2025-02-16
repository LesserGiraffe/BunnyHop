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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * ローカル環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class LocalBhRuntimeManager {

  private final BhRuntimeManagerCommon common;
  private Process process;
  /** プログラム実行中なら true. */
  private final AtomicReference<Boolean> programRunning = new AtomicReference<>(false);

  /** コンストラクタ. */
  LocalBhRuntimeManager(SimulatorCmdProcessor simCmdProcessor) {
    common = new BhRuntimeManagerCommon(simCmdProcessor);
  }

  /**
   * BhProgram の実行環境を立ち上げ、BhProgram を実行する.
   *
   * @param filePath BhProgram のファイルパス
   * @param ipAddr BhProgram を実行するマシンのIPアドレス
   * @return BhProgram 実行タスクのFutureオブジェクト
   */
  public Future<Boolean> executeAsync(Path filePath, String ipAddr) {
    return common.executeAsync(() -> execute(filePath, ipAddr));
  }

  /**
   * BhProgram の実行環境を立ち上げ、BhProgram を実行する.
   *
   * @param filePath BhProgram のファイルパス
   * @param ipAddr BhProgram を実行するマシンのIPアドレス
   * @return BhProgram の実行に成功した場合 true
   */
  private synchronized boolean execute(Path filePath, String ipAddr) {
    boolean success = true;
    if (programRunning.get()) {
      terminate();
    }
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Local.preparingToRun.get());
    if (success) {
      process = startRuntimeProcess();
      if (process == null) {
        success &= false;
      }
    }
    if (process != null) {
      String fileName = filePath.getFileName().toString();
      success &= common.runBhProgram(fileName, ipAddr, process.getInputStream());
    }
    if (!success) {  // リモートでのスクリプト実行失敗
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Local.failedToRun.get());
      BhService.msgPrinter().errForDebug(
          "Failed to run %s. (local)".formatted(filePath.getFileName()));
      terminate();
    } else {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Local.startToRun.get());
      programRunning.set(true);
    }
    return success;
  }

  /**
   * 現在実行中の BhProgram を強制終了する.
   *
   * @return BhProgram 強制終了タスクのFutureオブジェクト.
   */
  public Future<Boolean> terminateAsync() {
    if (!programRunning.get()) {
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Local.hasAlreadyEnded.get());
      return common.terminateAsync(() -> false);
    }
    return common.terminateAsync(() -> terminate());
  }

  /**
   * 現在実行中の BhProgram を強制終了する.
   * BhProgram実行環境を終了済みの場合に呼んでも問題ない.
   *
   * @return 強制終了に成功した場合 true
   */
  private synchronized boolean terminate() {
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Local.preparingToEnd.get());
    boolean success = common.haltTransceiver();
    if (process != null) {
      success &= BhRuntimeManagerCommon.killProcess(
          process, BhConstants.BhRuntime.PROC_END_TIMEOUT);
    }
    process = null;
    if (!success) {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Local.failedToEnd.get());
    } else {
      BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Local.hasTeminated.get());
      programRunning.set(false);
    }
    return success;
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 接続タスクの {@link Future} オブジェクト. タスクを実行しなかった場合 null.
   */
  public Future<Boolean> connectAsync() {
    return common.connectAsync();
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 切断タスクの {@link Future} オブジェクト. タスクを実行しなかった場合 null.
   */
  public Future<Boolean> disconnectAsync() {
    return common.disconnectAsync();
  }

  /**
   * 引数で指定した {@link BhProgramMessage} を BhProgram の実行環境に送る.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  public BhRuntimeStatus sendAsync(BhProgramMessage msg) {
    return common.sendAsync(msg);
  }

  /**
   * BhProgram のランタイムプロセスをスタートする.
   *
   * @return スタートしたプロセスのオブジェクト. スタートに失敗した場合 null.
   */
  private Process startRuntimeProcess() {
    // ""でパスを囲まない
    Process proc = null;
    ProcessBuilder procBuilder = new ProcessBuilder(
        Utility.javaPath,
        "-cp",
        Paths.get(Utility.execPath, "Jlib").toString() + Utility.fs  + "*",
        BhConstants.BhRuntime.BH_PROGRAM_EXEC_MAIN_CLASS,
        "true");  // localFlag == true

    procBuilder.redirectErrorStream(true);
    try {
      proc = procBuilder.start();
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Failed to start BhRuntime\n" +  e);
    }
    return proc;
  }

  /**
   * 終了処理をする.
   *
   * @return 終了処理が正常に完了した場合 true
   */
  public boolean end() {
    boolean success = terminate();
    success &= common.end();
    return success;
  }
}
