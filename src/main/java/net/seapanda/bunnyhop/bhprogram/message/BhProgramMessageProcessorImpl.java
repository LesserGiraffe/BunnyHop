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


import net.seapanda.bunnyhop.bhprogram.common.message.io.InputTextResp;
import net.seapanda.bunnyhop.bhprogram.common.message.io.OutputTextCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.io.OutputTextResp;
import net.seapanda.bunnyhop.bhprogram.common.message.thread.BhThreadContext;
import net.seapanda.bunnyhop.bhprogram.debugger.DebugMessageProcessor;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;

/**
 * BhProgram の実行環境から受信した {@link BhProgramMessage} を
 * 処理する機能を規定したインタフェース.
 *
 * @author K.koike
 */
public class BhProgramMessageProcessorImpl implements BhProgramMessageProcessor {

  private final MessageService msgService;
  private final DebugMessageProcessor debugMsgProcessor;

  /**
   * コンストラクタ.
   *
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   * @param debugMsgProcessor このオブジェクトが受け取ったデバッグ情報を処理するオブジェクト.
   */
  public BhProgramMessageProcessorImpl(
      MessageService msgService, DebugMessageProcessor debugMsgProcessor) {
    this.msgService = msgService;
    this.debugMsgProcessor = debugMsgProcessor;
  }

  @Override
  public OutputTextResp process(OutputTextCmd cmd) {
    msgService.info(cmd.text);
    return new OutputTextResp(cmd.getId(), true, cmd.text);
  }

  @Override
  public void process(InputTextResp resp) {
    if (!resp.success) {
      msgService.info(TextDefs.BhRuntime.Communication.failedToProcessText.get(resp.text));
      LogManager.logger().error("Failed to process a text data.  (%s)".formatted(resp.text));
    }
  }

  @Override
  public void process(BhThreadContext context) {
    logErrMsg(context.getException());
    debugMsgProcessor.process(context);
  }

  private void logErrMsg(Exception exception) {
    String errMsg = "";
    if (exception != null) {
      errMsg = exception.getMessage();
      if (exception.getCause() != null) {
        errMsg += "\n" + exception.getCause().getMessage();
      }
    }
    if (!errMsg.isEmpty()) {
      LogManager.logger().error(errMsg);
    }
  }
}
