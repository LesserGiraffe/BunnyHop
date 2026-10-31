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

package net.seapanda.bunnyhop.bhruntime.message;

import net.seapanda.bunnyhop.bhruntime.ctrl.BhRuntimeController;
import net.seapanda.bunnyhop.bhruntime.ctrl.BhRuntimeTransceiver;
import net.seapanda.bunnyhop.debugger.model.DebugMessageProcessor;
import net.seapanda.bunnyhop.runtime.api.message.BhRuntimeMessage;
import net.seapanda.bunnyhop.runtime.api.message.BhRuntimeNotification;
import net.seapanda.bunnyhop.runtime.api.message.BhRuntimeResponse;
import net.seapanda.bunnyhop.runtime.api.message.debug.GetEntryPointsResp;
import net.seapanda.bunnyhop.runtime.api.message.debug.GetGlobalListValsResp;
import net.seapanda.bunnyhop.runtime.api.message.debug.GetGlobalVarsResp;
import net.seapanda.bunnyhop.runtime.api.message.debug.GetLocalListValsResp;
import net.seapanda.bunnyhop.runtime.api.message.debug.GetLocalVarsResp;
import net.seapanda.bunnyhop.runtime.api.message.io.InputTextResp;
import net.seapanda.bunnyhop.runtime.api.message.io.OutputTextCmd;
import net.seapanda.bunnyhop.runtime.api.message.simulator.BhSimulatorCmd;
import net.seapanda.bunnyhop.runtime.api.message.simulator.StringBhSimulatorCmd;
import net.seapanda.bunnyhop.runtime.api.message.simulator.StringBhSimulatorResp;
import net.seapanda.bunnyhop.runtime.api.message.thread.BhThreadContext;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
import net.seapanda.bunnyhop.simulator.api.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.ui.view.ViewUtil;

/**
 * {@link BhRuntimeMessage} を適切なクラスに渡す.
 *
 * @author K.Koike
 */
public class BhRuntimeMessageDispatcher {
  
  /** BhProgram とやりとりする IO メッセージを処理するオブジェクト. */
  private final IoMessageProcessor ioMsgProcessor;
  /** BhProgram とやりとりするデバッグメッセージを処理するオブジェクト. */
  private final DebugMessageProcessor debugMsgProcessor;
  /** {@link BhSimulatorCmd} を処理するオブジェクト. */
  private final SimulatorCmdProcessor simCmdProcessor;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final TransactionNotificationService notifService;

  /**
   * コンストラクタ.
   *
   * @param ioMessageProcessor {@link BhSimulatorCmd} 以外の {@link BhRuntimeNotification} を処理するオブジェクト
   * @param simCmdProcessor {@link BhSimulatorCmd} を処理するオブジェクト
   */
  public BhRuntimeMessageDispatcher(
      IoMessageProcessor ioMessageProcessor,
      DebugMessageProcessor debugMsgProcessor,
      SimulatorCmdProcessor simCmdProcessor,
      BhRuntimeController runtimeCtrl,
      TransactionNotificationService notifService) {
    this.ioMsgProcessor = ioMessageProcessor;
    this.debugMsgProcessor = debugMsgProcessor;
    this.simCmdProcessor = simCmdProcessor;
    this.notifService = notifService;
    runtimeCtrl.getCallbackRegistry().getOnMsgCarrierRenewed()
        .add(event -> replaceMsgCarrier(event.oldCarrier(), event.newCarrier()));
  }
  
  /**
   * BhProgram との通信に使う {@link BhRuntimeMessageCarrier} を交換する.
   *
   * @param oldCarrier 古い通信用オブジェクト.  このオブジェクトに登録したイベントハンドラを登録解除する.
   * @param newCarrier 新しい通信用オブジェクト.  このオブジェクトにイベントハンドラを登録する.
   */
  private synchronized void replaceMsgCarrier(
      BhRuntimeMessageCarrier oldCarrier, BhRuntimeMessageCarrier newCarrier) {
    if (oldCarrier != null) {
      oldCarrier.setOnNotifReceived(null);
      oldCarrier.setOnRespReceived(null);
    }
    newCarrier.setOnNotifReceived(msg -> dispatchNotifAsync(newCarrier, msg));
    newCarrier.setOnRespReceived(this::dispatchRespAsync);
  }

  /**
   * {@code notif} を適切なクラスへと渡す.
   *
   * @param carrier {@code msg} を受信した {@link BhRuntimeTransceiver}.
   * @param notif 処理する通知.
   */
  private synchronized void dispatchNotifAsync(
      BhRuntimeMessageCarrier carrier, BhRuntimeNotification notif) {
    ViewUtil.runSafe(() -> {
      notifService.begin();
      try {
        dispatchNotif(carrier, notif);
      } finally {
        notifService.end();
      }
    });
  }

  private void dispatchNotif(BhRuntimeMessageCarrier carrier, BhRuntimeNotification notif) {
    switch (notif) {
      case OutputTextCmd
          cmd -> carrier.pushResponse(ioMsgProcessor.process(cmd));
      case BhThreadContext
          context -> debugMsgProcessor.process(context);
      case StringBhSimulatorCmd
          cmd -> dispatchSimulatorCmd(cmd, carrier);
      default -> { /* Do nothing. */ }
    }
  }

  /** {@link StringBhSimulatorCmd} をシミュレータに送る. */
  private void dispatchSimulatorCmd(StringBhSimulatorCmd cmd, BhRuntimeMessageCarrier carrier) {
    simCmdProcessor.process(
        cmd.getComponents(),
        (success, resp) -> {
          var response = new StringBhSimulatorResp(cmd.getId(), success, resp);
          carrier.pushResponse(response);
        });
  }

  /** {@code resp} を適切なクラスへと渡す. */
  private synchronized void dispatchRespAsync(BhRuntimeResponse response) {
    ViewUtil.runSafe(() -> {
      notifService.begin();
      try {
        dispatchResp(response);
      } finally {
        notifService.end();
      }
    });
  }

  private void dispatchResp(BhRuntimeResponse response) {
    switch (response) {
      case InputTextResp resp -> ioMsgProcessor.process(resp);
      case GetLocalVarsResp resp -> debugMsgProcessor.process(resp);
      case GetLocalListValsResp resp -> debugMsgProcessor.process(resp);
      case GetGlobalVarsResp resp -> debugMsgProcessor.process(resp);
      case GetGlobalListValsResp resp -> debugMsgProcessor.process(resp);
      case GetEntryPointsResp resp -> debugMsgProcessor.process(resp);
      default -> { /* Do nothing. */ }
    }
  }
}
