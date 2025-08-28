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

package net.seapanda.bunnyhop.view.factory;

import java.util.SequencedCollection;
import javafx.scene.Node;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.view.ViewConstructionException;

/**
 * デバッガのビューを作成する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface DebugViewFactory {
  
  /**
   * コールスタックを表示するビューを作成する.
   *
   * @param items コールスタックに表示する内容
   * @return コールスタックを表示するビューのルート要素.
   * @throws ViewConstructionException ビューの初期化に失敗した場合
   */
  Node createCallStackView(SequencedCollection<CallStackItem> items)
      throws ViewConstructionException;


  /**
   * 変数情報を表示するビューを作成する.
   *
   * @param varInfo 表示する変数情報を格納したオブジェクト
   * @return 変数情報を表示するビューのルート要素.
   * @throws ViewConstructionException ビューの初期化に失敗した場合
   */
  Node createVariableInspectionView(VariableInfo varInfo, String viewName)
      throws ViewConstructionException;
}
