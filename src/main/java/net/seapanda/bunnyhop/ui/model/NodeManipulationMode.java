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

package net.seapanda.bunnyhop.ui.model;

import javafx.scene.Cursor;

/**
 * ノードの操作モードを定義した列挙型.
 */
public enum NodeManipulationMode {

  /** デフォルトのモード. */
  MODE_0(Cursor.DEFAULT),
  /** ノードを複数選択するモード. */
  MODE_1(Cursor.HAND),
  /** ノードに付随する情報を参照するモード. */
  MODE_2(Cursor.CROSSHAIR);

  private final Cursor cursor;

  private NodeManipulationMode(Cursor cursor) {
    this.cursor = cursor;
  }

  /** 操作モードに応じたカーソルを取得する. */
  public Cursor getCursor() {
    return cursor;
  }

  // 外部スクリプトから参照するためのメソッド
  public boolean isMode0() {
    return this == MODE_0;
  }

  public boolean isMode1() {
    return this == MODE_1;
  }

  public boolean isMode2() {
    return this == MODE_2;
  }
}
