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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.DetectColorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.MeasureDistanceCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.MoveBackwardRaspiCarCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.MoveForwardRaspiCarCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.SetBothEyesColorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.SetLeftEyeColorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.SetRightEyeColorCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.StopRaspiCarCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.TurnLeftRaspiCarCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhSimulatorCmd.TurnRightRaspiCarCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.OutputTextCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoResp.InputTextResp;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;

/**
 * {@link BhProgramMessage} を適切なクラスに渡す.
 *
 * @author K.Koike
 */
public class BhProgramMessageDispatcher {
  
  /** {@link BhProgramMessage} を送受信するためのオブジェクト. */
  AtomicReference<BhRuntimeTransceiver> transceiver = new AtomicReference<>();
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
   * BhProgram との通信に使う {@link BhRuntimeTransceiver} を交換する.
   * 既に設定済みのものは使用されなくなる.
   *
   * @param transceiver 新しく設定する通信用オブジェクト.
   * @return このメソッドを呼び出す前に使用していた通信用オブジェクト.
   */
  public Optional<BhRuntimeTransceiver> replaceTransceiver(BhRuntimeTransceiver transceiver) {
    transceiver.setOnNotifReceived(msg -> dispatch(transceiver, msg));
    transceiver.setOnRespReceived(resp -> dispatch(resp));
    return Optional.ofNullable(this.transceiver.getAndSet(transceiver));
  }

  /** このオブジェクトが, 現在 BhProgram との通信に使用している {@link BhRuntimeTransceiver} を取得する. */
  public Optional<BhRuntimeTransceiver> getTransceiver() {
    return Optional.ofNullable(transceiver.get());
  }

  /**
   * {@code msg} を適切なクラスへと渡す.
   *
   * @param transceiver {@code msg} を受信した {@link BhRuntimeTransceiver}.
   * @param notification 処理する通知.
   */
  private void dispatch(BhRuntimeTransceiver transceiver, BhProgramNotification notif) {
    switch (notif) {
      case OutputTextCmd
          cmd -> transceiver.pushSendResp(msgProcessor.process(cmd));
      case BhProgramException
          exception -> msgProcessor.process(exception);
      case MoveForwardRaspiCarCmd
          cmd -> simCmdProcessor.process(cmd, resp -> transceiver.pushSendResp(resp));
      case MoveBackwardRaspiCarCmd
          cmd -> simCmdProcessor.process(cmd, resp -> transceiver.pushSendResp(resp));
      case TurnRightRaspiCarCmd
          cmd -> simCmdProcessor.process(cmd, resp -> transceiver.pushSendResp(resp));
      case TurnLeftRaspiCarCmd
          cmd -> simCmdProcessor.process(cmd, resp -> transceiver.pushSendResp(resp));
      case StopRaspiCarCmd
          cmd -> simCmdProcessor.process(cmd, resp -> transceiver.pushSendResp(resp));
      case MeasureDistanceCmd
          cmd -> transceiver.pushSendResp(simCmdProcessor.process(cmd));
      case DetectColorCmd
          cmd -> transceiver.pushSendResp(simCmdProcessor.process(cmd));
      case SetLeftEyeColorCmd
          cmd -> transceiver.pushSendResp(simCmdProcessor.process(cmd));
      case SetRightEyeColorCmd
          cmd -> transceiver.pushSendResp(simCmdProcessor.process(cmd));
      case SetBothEyesColorCmd
          cmd -> transceiver.pushSendResp(simCmdProcessor.process(cmd));
      default -> notifyInvalidNotif(notif);
    }
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
