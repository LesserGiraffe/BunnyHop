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
import java.util.concurrent.locks.ReentrantLock;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.common.BhRuntimeFacade;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.concurrent.event.ConcurrentConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;

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
  private final ReentrantLock lock = new ReentrantLock();

  /** コンストラクタ. */
  public RmiLocalBhRuntimeController(
      SimulatorCmdProcessor simCmdProcessor, MessageService msgService) {
    this.simCmdProcessor = simCmdProcessor;
    this.msgService = msgService;
  }

  @Override
  public boolean start(Path filePath) {
    if (!lock.tryLock()) {
      return false;
    }
    try {
      return startBhProgram(filePath);
    } finally {
      lock.unlock();
    }
  }

  private boolean startBhProgram(Path filePath) {
    if (programRunning.get()) {
      terminate();
    }
    msgService.info(TextDefs.BhRuntime.Local.preparingToRun.get());
    try {
      BhRuntimeFacade facade = startUpRuntime().orElseThrow();
      boolean success = facade.runScript(filePath.toAbsolutePath().toString());
      if (success) {
        return invokeStartMethod(filePath);
      }
    } catch (Exception e) {
      msgService.error(TextDefs.BhRuntime.Local.failedToRun.get());
      LogManager.logger().error("Failed to run %s. (local)".formatted(filePath.getFileName()));
      terminate();
      return false;
    }
    return false;
  }

  /**
   * BhRuntime を起動して初期化する.
   *
   * @return 起動した BhRuntime を操作するためのオブジェクト.
   *         処理に失敗した場合は empty.
   */
  private Optional<BhRuntimeFacade> startUpRuntime() {
    try {
      process = startRuntimeProcess().orElseThrow();
      BhRuntimeFacade facade = getBhRuntimeFacade().orElseThrow();
      setupTransceiver(facade);
      if (!transceiver.connect()) {
        return Optional.empty();
      }
      // BhProgram の開始前に実行したい処理に対応するため, イベントハンドラをここで呼ぶ
      cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, true));
      return Optional.of(facade);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /** {@link #transceiver} を初期化し, メッセージキャリア更新イベントを発行する. */
  private void setupTransceiver(BhRuntimeFacade facade) {
    var oldCarrier = (transceiver == null) ? null : transceiver.getMessageCarrier();
    transceiver = new BhRuntimeTransceiver(facade);
    var event = new MessageCarrierRenewedEvent(this, oldCarrier, transceiver.getMessageCarrier());
    cbRegistry.onMsgCarrierRenewed.invoke(event);
    transceiver.start();
  }

  /** BhProgram の開始時に実行する処理を呼ぶ. */
  private boolean invokeStartMethod(Path filePath) {
    msgService.info(TextDefs.BhRuntime.Local.hasStarted.get());
    var startEvent = new BhProgramEvent(
        BhProgramEvent.Name.PROGRAM_START, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    send(startEvent);
    programRunning.set(true);
    cbRegistry.onBhProgramStarted.invoke(new StartEvent(this, filePath));
    return true;
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
  public boolean terminate() {
    if (!lock.tryLock()) {
      return false;
    }
    try {
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
        cbRegistry.onBhProgramTerminated.invoke(new TerminationEvent(this));
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean connect() {
    if (!lock.tryLock()) {
      return false;
    }
    try {
      if (transceiver == null) {
        msgService.error(TextDefs.BhRuntime.Local.noRuntimeToConnectTo.get());
        return false;
      }
      msgService.info(TextDefs.BhRuntime.Local.hasConnected.get());
      boolean success = transceiver.connect();
      if (success) {
        cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, true));
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean disconnect() {
    if (!lock.tryLock()) {
      return false;
    }
    try {
      if (transceiver == null) {
        msgService.error(TextDefs.BhRuntime.Local.noRuntimeToDisconnectFrom.get());
        return false;
      }
      msgService.info(TextDefs.BhRuntime.Local.hasDisconnected.get());
      boolean success = transceiver.disconnect();
      if (success) {
        cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, false));
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public BhRuntimeStatus send(BhProgramMessage message) {
    if (!lock.tryLock()) {
      return BhRuntimeStatus.BUSY;
    }
    try {
      if (transceiver == null) {
        return BhRuntimeStatus.SEND_WHEN_DISCONNECTED;
      }
      return transceiver.getMessageCarrier().pushMessage(message);
    } finally {
      lock.unlock();
    }
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
        Paths.get(Utility.execPath, "Jlib") + Utility.fs  + "*",
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
    cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, false));
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

  /** {@link BhRuntimeController} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistryImpl implements BhRuntimeController.CallbackRegistry {
    
    /** BhRuntime との通信用オブジェクトが置き換わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MessageCarrierRenewedEvent> onMsgCarrierRenewed =
        new ConcurrentConsumerInvoker<>();

    /** BhRuntime との通信が有効または無効になったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ConnectionEvent> onConnCondChanged =
        new ConcurrentConsumerInvoker<>();

    /** BhProgram を開始したときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<StartEvent> onBhProgramStarted =
        new ConcurrentConsumerInvoker<>();

    /** BhProgram を終了したときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<TerminationEvent> onBhProgramTerminated =
        new ConcurrentConsumerInvoker<>();

    @Override
    public ConsumerInvoker<MessageCarrierRenewedEvent>.Registry getOnMsgCarrierRenewed() {
      return onMsgCarrierRenewed.getRegistry();
    }

    @Override
    public ConsumerInvoker<ConnectionEvent>.Registry getOnConnectionConditionChanged() {
      return onConnCondChanged.getRegistry();
    }

    @Override
    public ConsumerInvoker<StartEvent>.Registry getOnBhProgramStarted() {
      return onBhProgramStarted.getRegistry();
    }

    @Override
    public ConsumerInvoker<TerminationEvent>.Registry getOnBhProgramTerminated() {
      return onBhProgramTerminated.getRegistry();
    }
  }
}
