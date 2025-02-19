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

package net.seapanda.bunnyhop.service;

import net.seapanda.bunnyhop.utility.log.Logger;

/**
 * アプリケーション全体で使用する {@link Logger} オブジェクトを保持するクラス.
 *
 * @author K.Koike
 */
public class LogManager {

  private static volatile Logger logger = new Logger() {
    @Override
    public void info(String msg) {}

    @Override
    public void error(String msg) {}
  };

  /** 初期化処理を行う. */
  public static void initialize(Logger logger) {
    LogManager.logger = logger;
  }

  public static Logger logger() {
    return logger;
  }
}
