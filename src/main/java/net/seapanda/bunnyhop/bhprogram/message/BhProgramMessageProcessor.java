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
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoCmd.OutputTextCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoResp.InputTextResp;
import net.seapanda.bunnyhop.bhprogram.common.message.BhTextIoResp.OutputTextResp;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;

/**
 * BhProgram の実行環境から受信した {@link BhProgramMessage} および {@link BhProgramResponse} を処理するクラス.
 *
 * @author K.koike
 */
public class BhProgramMessageProcessor {

  public BhProgramMessageProcessor() {}

  /**
   * {@link OutputTextCmd} を処理する.
   *
   * @param cmd 処理するコマンド
   * @return {@code cmd} に対応する応答データ
   */
  public OutputTextResp process(OutputTextCmd cmd) {
    MsgPrinter.INSTANCE.msgForUser(cmd.text);
    return new OutputTextResp(cmd.getId(), true, cmd.text);
  }

  /**
   * {@link InputTextResp} を処理する.
   *
   * @param resp 処理するレスポンス.
   */
  public void process(InputTextResp resp) {
    if (!resp.success) {
      String msg = "テキストの送信に失敗しました.  (" + resp.text + ")";
      MsgPrinter.INSTANCE.msgForUser(msg + "\n");
      MsgPrinter.INSTANCE.errMsgForDebug(msg);
    }
  }

  /**
   * {@link BhProgramException} を処理する.
   *
   * @param exception 処理する例外
   */
  public void process(BhProgramException exception) {
    MsgPrinter.INSTANCE.msgForUser(exception.getMessage() + "\n");
    // MsgPrinter.INSTANCE.msgForUser(exception.getScriptEngineMsg() + "\n");
    var iter = exception.getCallStack().descendingIterator();
    while (iter.hasNext()) {
      MsgPrinter.INSTANCE.msgForUser("  " + iter.next().toString() + "\n");
    }
  }
}
