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
 * アプリケーションを構成するウィンドウに対する操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface WindowManager {

  /**
   * シミュレータを表示するウィンドウにフォーカスを当てる.
   *
   * @param doForcibly true の場合, シミュレータがアイコン化されていてもウィンドウ化して表示する. <br>
   *                   false の場合, シミュレータがアイコン化されていたら何もしない.
   */
  void focusSimulator(boolean doForcibly);

  /**
   * シミュレータを表示するウィンドウにフォーカスを当てる.
   *
   * <p>シミュレータがアイコン化されている場合は何もしない
   */
  void focusSimulator();
}
