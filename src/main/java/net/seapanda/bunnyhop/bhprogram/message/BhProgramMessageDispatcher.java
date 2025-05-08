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

import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.StrBhSimulatorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorResp.StrBhSimulatorResp;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.OutputTextCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoResp.InputTextResp;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeTransceiver;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;

/**
 * {@link BhProgramMessage} を適切なクラスに渡す.
 *
 * @author K.Koike
 */
public class BhProgramMessageDispatcher {
  
  /** {@link BhSimulatorCmd} 以外の {@link BhProgramNotification} を処理するオブジェクト. */
  private final BhProgramMessageProcessor msgProcessor;
  /** {@link BhSimulatorCmd} を処理するオブジェクト. */
  private final SimulatorCmdProcessor simCmdProcessor;

  /**
   * コンストラクタ.
   *
   * @param msgProcessor {@link BhSimulatorCmd} 以外の {@link BhProgramNotification} を処理するオブジェクト
   * @param simCmdProcessor {@link BhSimulatorCmd} を処理するオブジェクト
   */
  public BhProgramMessageDispatcher(
      BhProgramMessageProcessor msgProcessor,
      SimulatorCmdProcessor simCmdProcessor) {
    this.msgProcessor = msgProcessor;
    this.simCmdProcessor = simCmdProcessor;
  }

  /**
   * BhProgram との通信に使う {@link BhProgramMessageCarrier} を交換する.
   * 既に設定済みのものは使用されなくなる.
   *
   * @param carrier 新しく設定する通信用オブジェクト.
   */
  public void replaceMsgCarrier(BhProgramMessageCarrier carrier) {
    carrier.setOnNotifReceived(msg -> dispatch(carrier, msg));
    carrier.setOnRespReceived(resp -> dispatch(resp));
  }

  /**
   * {@code msg} を適切なクラスへと渡す.
   *
   * @param carrier {@code msg} を受信した {@link BhRuntimeTransceiver}.
   * @param notification 処理する通知.
   */
  private void dispatch(BhProgramMessageCarrier carrier, BhProgramNotification notif) {
    switch (notif) {
      case OutputTextCmd
          cmd -> carrier.pushSendResp(msgProcessor.process(cmd));
      case BhProgramException
          exception -> msgProcessor.process(exception);
      case StrBhSimulatorCmd
          cmd ->  dispatchSimulatorCmd(cmd, carrier);

      default -> notifyInvalidNotif(notif);
    }
  }

  /** {@link StrBhSimulatorCmd} をシミュレータに送る. */
  private void dispatchSimulatorCmd(StrBhSimulatorCmd cmd, BhProgramMessageCarrier carrier) {
    simCmdProcessor.process(
        cmd.getComponents(),
        (success, resp) -> {
            var response = new StrBhSimulatorResp(cmd.getId(), success, resp);
            carrier.pushSendResp(response);
        });
  }

  /** {@code resp} を適切なクラスへと渡す. */
  private void dispatch(BhProgramResponse resp) {
    switch (resp) {
      case InputTextResp inputTestResp -> msgProcessor.process(inputTestResp);
      default -> notifyInvalidResp(resp);
    }
  }

  private void notifyInvalidNotif(BhProgramNotification notif) {
    LogManager.logger().error("Received an invalid message.  (%s)".formatted(notif));
  }

  private void notifyInvalidResp(BhProgramResponse resp) {
    LogManager.logger().error("Received an invalid response.  (%s)".formatted(resp));
  }
}
