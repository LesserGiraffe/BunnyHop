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

package net.seapanda.bunnyhop.debugger.model;

import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.common.text.TextDefs;
import org.apache.commons.lang3.StringUtils;

/**
 * デバッガの実装で必要となる共通の処理をまとめたクラス.
 *
 * @author K.Koike
 */
public class DebugUtil {
 
  /**
   * {@code exception} からエラーメッセージを取得する.
   *
   * @param exception この例外オブジェクトからエラーメッセージを取得する.  null の場合, 空文字列を返す.
   */
  public static String getErrMsg(BhProgramException exception) {
    if (exception == null) {
      return "";
    }
    String msg = exception.getMessage();
    Throwable cause = exception.getCause();
    if (cause == null) {
      return truncateErrMsg(msg);
    }
    String errMsg = switch (cause) {
      case StackOverflowError e -> TextDefs.Debugger.stackOverflow.get();
      case OutOfMemoryError e -> TextDefs.Debugger.outOfMemory.get();
      case BhProgramException e -> {
        msg += StringUtils.isEmpty(msg) ? "" : "\n";
        yield msg + getErrMsg(e);
      }
      default -> {
        msg += StringUtils.isEmpty(msg) ? "" : "\n";
        msg += StringUtils.isEmpty(cause.getMessage()) ? "" : "%s  ".formatted(cause.getMessage());
        yield msg + "(%s)".formatted(cause.getClass().getName());
      }
    };
    return truncateErrMsg(errMsg);
  }

  private static String truncateErrMsg(String errMsg) {
    return (errMsg.length() > BhSettings.Message.maxErrMsgChars)
        ? errMsg.substring(0, BhSettings.Message.maxErrMsgChars) + "..."
        : errMsg;
  }  
}