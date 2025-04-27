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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageProcessor;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * ローカル環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class LocalBhRuntimeController implements BhRuntimeController {

  private final MessageService msgService;
  private final BhRuntimeHelper helper;
  private Process process;
  /** プログラム実行中なら true. */
  private final AtomicReference<Boolean> programRunning = new AtomicReference<>(false);

  /** コンストラクタ. */
  public LocalBhRuntimeController(
      BhProgramMessageProcessor msgProcessor,
      SimulatorCmdProcessor simCmdProcessor,
      MessageService msgService) {
    this.msgService = msgService;
    helper = new BhRuntimeHelper(msgProcessor, simCmdProcessor, msgService);
  }

  @Override
  public boolean start(Path filePath, String ipAddr, String uname, String password)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot start a remote BhRuntime.");
  }

  @Override
  public synchronized boolean start(Path filePath) {
    boolean success = true;
    if (programRunning.get()) {
      terminate();
    }
    msgService.info(TextDefs.BhRuntime.Local.preparingToRun.get());
    if (success) {
      process = startRuntimeProcess();
      if (process == null) {
        success &= false;
      }
    }
    if (process != null) {
      success &= helper.runBhProgram(
          filePath.toString(), BhConstants.BhRuntime.LOCAL_HOST, process.getInputStream());
    }
    if (!success) {  // リモートでのスクリプト実行失敗
      msgService.error(TextDefs.BhRuntime.Local.failedToRun.get());
      LogManager.logger().error(
          "Failed to run %s. (local)".formatted(filePath.getFileName()));
      terminate();
    } else {
      msgService.info(TextDefs.BhRuntime.Local.startToRun.get());
      programRunning.set(true);
    }
    return success;
  }
  
  @Override
  public synchronized boolean terminate() {
    if (!programRunning.get()) {
      msgService.error(TextDefs.BhRuntime.Local.hasAlreadyEnded.get());
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Local.preparingToEnd.get());
    boolean success = helper.haltTransceiver();
    if (process != null) {
      success &= BhRuntimeHelper.killProcess(process, BhConstants.BhRuntime.PROC_END_TIMEOUT);
    }
    process = null;
    if (!success) {
      msgService.info(TextDefs.BhRuntime.Local.failedToEnd.get());
    } else {
      msgService.info(TextDefs.BhRuntime.Local.hasTeminated.get());
      programRunning.set(false);
    }
    return success;
  }

  @Override
  public synchronized boolean connect() {
    return helper.connect();
  }

  @Override
  public synchronized boolean disconnect() {
    return helper.disconnect();
  }

  @Override
  public synchronized BhRuntimeStatus send(BhProgramNotification notif) {
    return helper.send(notif);
  }

  /**
   * BhProgram のランタイムプロセスをスタートする.
   *
   * @return スタートしたプロセスのオブジェクト. スタートに失敗した場合 null.
   */
  private Process startRuntimeProcess() {
    // ""でパスを囲まない
    Process proc = null;
    var procBuilder = new ProcessBuilder(
        Utility.javaPath,
        "-cp",
        Paths.get(Utility.execPath, "Jlib").toString() + Utility.fs  + "*",
        BhConstants.BhRuntime.BH_PROGRAM_EXEC_MAIN_CLASS);

    procBuilder.redirectErrorStream(true);
    try {
      proc = procBuilder.start();
    } catch (IOException e) {
      LogManager.logger().error("Failed to start BhRuntime\n" +  e);
    }
    return proc;
  }

  /**
   * 終了処理をする.
   *
   * @return 終了処理が正常に完了した場合 true
   */
  public boolean end() {
    return terminate();
  }
}
