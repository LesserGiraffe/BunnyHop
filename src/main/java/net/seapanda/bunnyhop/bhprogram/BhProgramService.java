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

package net.seapanda.bunnyhop.bhprogram;

import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;

/**
 * BhProgram の実行、終了、通信を行うオブジェクトを提供するクラス.
 *
 * @author K.Koike
 */
public class BhProgramService {

  private static LocalBhProgramManager localManager;
  private static RemoteBhProgramManager remoteManager;

  /**
   * statis メンバの初期化を行う.
   *
   * @param simCmdProcessor BhSimulator 用のコマンドを処理するオブジェクト.
   */
  public static boolean init(SimulatorCmdProcessor simCmdProcessor) {
    localManager = new LocalBhProgramManager(simCmdProcessor);
    try {
      remoteManager = new RemoteBhProgramManager(simCmdProcessor);
    } catch (IllegalStateException e) {
      return false;
    }
    return true;
  }

  /** ローカル環境で動作する BhProgram を制御するためのオブジェクトを取得する. */
  public static LocalBhProgramManager local() {
    return localManager;
  }

  /** リモート環境で動作する BhProgram を制御するためのオブジェクトを取得する. */
  public static RemoteBhProgramManager remote() {
    return remoteManager;
  }
}
