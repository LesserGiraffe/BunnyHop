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

package net.seapanda.bunnyhop.common;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BunnyHop の設定一式をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhSettings {

  public static String language = "Japanese";

  /** BhSimulator に関するパラメータ. */
  public static class BhSimulator {
    /** BhSimulator 初期化待ちタイムアウト (sec). */
    public static int initTimeout = 5;
    /** BhProgram の開始時に BhSimulator をフォーカスするかどうか. */
    public static AtomicBoolean focusOnStartBhProgram = new AtomicBoolean(true);
    /** BhSimulator に変化があったとき BhSimulator をフォーカスするかどうか. */
    public static AtomicBoolean focusOnChanged = new AtomicBoolean(false);
  }

  /** BunnyHop が出力するテキストメッセージに関するパラメータ. */
  public static class Message {
    public static final int maxErrMsgChars = 4096;
  }

  /** デバッグに関するパラメータ. */
  public static class Debug {
    /** コールスタックに表示するデフォルトの最大要素数. */
    public static int maxCallStackItems = 32;
  }
}
