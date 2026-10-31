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

package net.seapanda.bunnyhop.ui.service.window;

/**
 * アプリケーションのウィンドウの状態 (位置, サイズ, 最小化・最大化など) の
 * 永続化と復元を行う操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface WindowStateManager {

  /** 永続化されたウィンドウの状態を読み込み, 各ウィンドウに適用する. */
  void restoreWindowStates();

  /** 現在のウィンドウの状態を永続化する. */
  void saveWindowStates();
}
