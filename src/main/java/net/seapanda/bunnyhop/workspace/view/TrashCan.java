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

package net.seapanda.bunnyhop.workspace.view;

/**
 * ノードを削除するゴミ箱を操作するメソッドを規定したインタフェース.
 *
 * @author K.Koike
 */
public interface TrashCan {
  
  /** ゴミ箱を開ける. */
  void open();

  /** ゴミ箱を閉じる. */
  void close();

  /** ゴミ箱が開いている場合 true を返す. */
  boolean isOpened();

  /** ゴミ箱が閉じている場合 false を返す. */
  boolean isClosed();
 
  /**
   * 引数で指定した位置がゴミ箱領域内ならゴミ箱を開く.
   * それ以外の場合, ゴミ箱を閉じる.
   *
   * @param sceneX Scene 上でのX位置
   * @param sceneY Scene 上でのY位置
   */
  void auto(double sceneX, double sceneY);
}
