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

package net.seapanda.bunnyhop.nodeselection.view;

import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * {@link BhNodeSelectionView} を作成する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionViewFactory {

  /**
   * ノード選択ビューを作成する.
   *
   * @param name ノード選択ビューの名前
   * @param cssClass ノード選択ビューに適用する css クラス
   */
  BhNodeSelectionView create(String name, String cssClass) throws ViewConstructionException;
}
