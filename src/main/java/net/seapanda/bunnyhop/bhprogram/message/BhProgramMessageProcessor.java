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

/**
 * BhProgram の実行環境から受信した {@link BhProgramMessage} を
 * 処理する機能を規定したインタフェース.
 *
 * @author K.koike
 */
public interface BhProgramMessageProcessor {

  /**
   * {@link OutputTextCmd} を処理する.
   *
   * @param cmd 処理するコマンド
   * @return {@code cmd} に対応する応答データ
   */
  public OutputTextResp process(OutputTextCmd cmd);

  /**
   * {@link InputTextResp} を処理する.
   *
   * @param resp 処理するレスポンス.
   */
  public void process(InputTextResp resp);

  /**
   * {@link BhThreadContext} を処理する.
   *
   * @param context 処理するスレッドコンテキスト
   */
  public void process(BhThreadContext context);
}
