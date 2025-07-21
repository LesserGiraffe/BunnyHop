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
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.bhprogram.common.BhRuntimeFacade;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * ローカル環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class RmiLocalBhRuntimeController implements LocalBhRuntimeController {

  private final MessageService msgService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  private final SimulatorCmdProcessor simCmdProcessor;
  private Process process;
  /** BhRuntime との通信用オブジェクト. */
  private volatile BhRuntimeTransceiver transceiver;
  /** BhProgram を実行中かどうかを表すフラグ. */
  private final AtomicReference<Boolean> programRunning = new AtomicReference<>(false);


  /** コンストラクタ. */
  public RmiLocalBhRuntimeController(
      SimulatorCmdProcessor simCmdProcessor, MessageService msgService) {
    this.simCmdProcessor = simCmdProcessor;
    this.msgService = msgService;
  }

  @Override
  public synchronized boolean start(Path filePath) {
    if (programRunning.get()) {
      terminate();
    }
    msgService.info(TextDefs.BhRuntime.Local.preparingToRun.get());
    try {
      process = startRuntimeProcess().orElseThrow();
      BhRuntimeFacade facade = getBhRuntimeFacade().orElseThrow();
      var oldCarrier = (transceiver == null) ? null : transceiver.getMessageCarrier();
      transceiver = new BhRuntimeTransceiver(facade);
      var event = new MessageCarrierRenewedEvent(this, oldCarrier, transceiver.getMessageCarrier());
      cbRegistry.onMsgCarrierRenewed.invoke(event);
      transceiver.start();
      boolean success = transceiver.connect();
      if (success) {
        success &= BhRuntimeHelper.runScript(filePath.toAbsolutePath().toString(), facade);
      }
      if (success) {
        msgService.info(TextDefs.BhRuntime.Local.hasStarted.get());
        programRunning.set(true);
        return true;
      }
      throw new Exception();
    } catch (Exception e) {
      msgService.error(TextDefs.BhRuntime.Local.failedToRun.get());
      LogManager.logger().error(
          "Failed to run %s. (local)".formatted(filePath.getFileName()));
      terminate();
    }
    return false;
  }

  private Optional<BhRuntimeFacade> getBhRuntimeFacade() {
    BhRuntimeFacade facade = null;
    try (var br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      facade = BhRuntimeHelper.getBhRuntimeFacade(
          BhConstants.BhRuntime.LOCAL_HOST, br, BhConstants.BhRuntime.Timeout.LOCAL_START);
    } catch (Exception e) {
      LogManager.logger().error("Failed to get BhRuntime facade.\n%s".formatted(e));
    }
    return Optional.ofNullable(facade);
  }
  
  @Override
  public synchronized boolean terminate() {
    if (!programRunning.get()) {
      msgService.error(TextDefs.BhRuntime.Local.hasAlreadyEnded.get());
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Local.preparingToEnd.get());
    boolean success = discardTransceiver(0);
    if (process != null) {
      success &= BhRuntimeHelper.killProcess(process, BhConstants.BhRuntime.Timeout.PROC_END);
    }
    process = null;
    simCmdProcessor.halt();
    if (!success) {
      msgService.error(TextDefs.BhRuntime.Local.failedToEnd.get());
    } else {
      msgService.info(TextDefs.BhRuntime.Local.hasEnded.get());
      programRunning.set(false);
    }
    return success;
  }

  @Override
  public synchronized boolean connect() {
    if (transceiver == null) {
      msgService.error(TextDefs.BhRuntime.Local.noRuntimeToConnectTo.get());
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Remote.hasConnected.get());
    return transceiver.connect();
  }

  @Override
  public synchronized boolean disconnect() {
    if (transceiver == null) {
      msgService.error(TextDefs.BhRuntime.Local.noRuntimeToDisconnectFrom.get());
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Remote.hasDisconnected.get());
    return transceiver.disconnect();
  }

  @Override
  public synchronized BhRuntimeStatus send(BhProgramMessage message) {
    if (transceiver == null) {
      return BhRuntimeStatus.SEND_WHEN_DISCONNECTED;
    }
    return transceiver.getMessageCarrier().pushMessage(message);
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /**
   * BhProgram のランタイムプロセスをスタートする.
   *
   * @return スタートしたプロセスのオブジェクト.
   */
  private Optional<Process> startRuntimeProcess() {
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
    return Optional.ofNullable(proc);
  }

  /**
   * {@link #transceiver} を破棄する.
   *
   * @param timeout トランシーバの終了処理のタイムアウト時間 (ms)
   */
  private boolean discardTransceiver(int timeout) {
    if (transceiver == null) {
      return true;
    }
    boolean success = transceiver.halt(timeout);
    transceiver = null;
    return success;
  }

  /**
   * 終了処理をする.
   *
   * @return 終了処理が正常に完了した場合 true
   */
  public boolean end() {
    return terminate();
  }

  /** {@link RmiLocalBhRuntimeController} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistryImpl implements LocalBhRuntimeController.CallbackRegistry {
    
    /** BhRuntime との通信用オブジェクトが置き換わったときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<MessageCarrierRenewedEvent> onMsgCarrierRenewed =
        new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<MessageCarrierRenewedEvent>.Registry getOnMsgCarrierRenewed() {
      return onMsgCarrierRenewed.getRegistry();
    }        
  }
}
