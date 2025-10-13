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
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;

/**
 * BhProgram の実行環境から受信したメッセージを処理する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class IoMessageProcessorImpl implements IoMessageProcessor {

  private final MessageService msgService;

  /**
   * コンストラクタ.
   *
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public IoMessageProcessorImpl(MessageService msgService) {
    this.msgService = msgService;
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
}
