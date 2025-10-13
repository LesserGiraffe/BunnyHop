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

package net.seapanda.bunnyhop.ui.control;

import javafx.scene.control.IndexRange;
import net.seapanda.bunnyhop.ui.control.SearchBox.Query;

/**
 * 文字列から特定の文字列を探す機能を提供するクラス.
 *
 * @author K.Koike
 */
class TextFinder {

  /**
   * {@code text} から {@code query} の条件を満たすテキストを見つける.
   *
   * @param text この文字列から {@code query} の条件を満たすテキストを見つける
   * @param query 検索条件
   * @return 検索された文字列の {@code text} における範囲
   */
  public static IndexRange find(String text, int currentPos, Query query) {
    return new IndexRange(0, 0);
  }
}
