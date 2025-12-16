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

package net.seapanda.bunnyhop.bhprogram.message;

import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetEntryPointsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalListValsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalVarsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalListValsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalVarsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.io.InputTextResp;
import net.seapanda.bunnyhop.bhprogram.common.message.io.OutputTextCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.simulator.BhSimulatorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.simulator.StringBhSimulatorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.simulator.StringBhSimulatorResp;
import net.seapanda.bunnyhop.bhprogram.common.message.thread.BhThreadContext;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeTransceiver;
import net.seapanda.bunnyhop.debugger.model.DebugMessageProcessor;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import net.seapanda.bunnyhop.ui.view.ViewUtil;

/**
 * {@link BhProgramMessage} を適切なクラスに渡す.
 *
 * @author K.Koike
 */
public class BhProgramMessageDispatcher {
  
  /** BhProgram とやりとりする IO メッセージを処理するオブジェクト. */
  private final IoMessageProcessor ioMsgProcessor;
  /** BhProgram とやりとりするデバッグメッセージを処理するオブジェクト. */
  private final DebugMessageProcessor debugMsgProcessor;
  /** {@link BhSimulatorCmd} を処理するオブジェクト. */
  private final SimulatorCmdProcessor simCmdProcessor;
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final ModelAccessNotificationService notifService;

  /**
   * コンストラクタ.
   *
   * @param ioMessageProcessor {@link BhSimulatorCmd} 以外の {@link BhProgramNotification} を処理するオブジェクト
   * @param simCmdProcessor {@link BhSimulatorCmd} を処理するオブジェクト
   */
  public BhProgramMessageDispatcher(
      IoMessageProcessor ioMessageProcessor,
      DebugMessageProcessor debugMsgProcessor,
      SimulatorCmdProcessor simCmdProcessor,
      BhRuntimeController runtimeCtrl,
      ModelAccessNotificationService notifService) {
    this.ioMsgProcessor = ioMessageProcessor;
    this.debugMsgProcessor = debugMsgProcessor;
    this.simCmdProcessor = simCmdProcessor;
    this.notifService = notifService;
    runtimeCtrl.getCallbackRegistry().getOnMsgCarrierRenewed()
        .add(event -> replaceMsgCarrier(event.oldCarrier(), event.newCarrier()));
  }
  
  /**
   * BhProgram との通信に使う {@link BhProgramMessageCarrier} を交換する.
   *
   * @param oldCarrier 古い通信用オブジェクト.  このオブジェクトに登録したイベントハンドラを登録解除する.
   * @param newCarrier 新しい通信用オブジェクト.  このオブジェクトにイベントハンドラを登録する.
   */
  private synchronized void replaceMsgCarrier(
      BhProgramMessageCarrier oldCarrier, BhProgramMessageCarrier newCarrier) {
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
      BhProgramMessageCarrier carrier, BhProgramNotification notif) {
    ViewUtil.runSafe(() -> {
      notifService.beginWrite();
      try {
        dispatchNotif(carrier, notif);
      } finally {
        notifService.endWrite();
      }
    });
  }

  private void dispatchNotif(BhProgramMessageCarrier carrier, BhProgramNotification notif) {
    switch (notif) {
      case OutputTextCmd
               cmd -> carrier.pushResponse(ioMsgProcessor.process(cmd));
      case BhThreadContext
               context -> debugMsgProcessor.process(context);
      case StringBhSimulatorCmd
               cmd -> dispatchSimulatorCmd(cmd, carrier);
      default -> { }
    }
  }

  /** {@link StringBhSimulatorCmd} をシミュレータに送る. */
  private void dispatchSimulatorCmd(StringBhSimulatorCmd cmd, BhProgramMessageCarrier carrier) {
    simCmdProcessor.process(
        cmd.getComponents(),
        (success, resp) -> {
          var response = new StringBhSimulatorResp(cmd.getId(), success, resp);
          carrier.pushResponse(response);
        });
  }

  /** {@code resp} を適切なクラスへと渡す. */
  private synchronized void dispatchRespAsync(BhProgramResponse response) {
    ViewUtil.runSafe(() -> {
      notifService.beginWrite();
      try {
        dispatchResp(response);
      } finally {
        notifService.endWrite();
      }
    });
  }

  private void dispatchResp(BhProgramResponse response) {
    switch (response) {
      case InputTextResp resp -> ioMsgProcessor.process(resp);
      case GetLocalVarsResp resp -> debugMsgProcessor.process(resp);
      case GetLocalListValsResp resp -> debugMsgProcessor.process(resp);
      case GetGlobalVarsResp resp -> debugMsgProcessor.process(resp);
      case GetGlobalListValsResp resp -> debugMsgProcessor.process(resp);
      case GetEntryPointsResp resp -> debugMsgProcessor.process(resp);
      default -> { }
    }
  }
}
