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

package net.seapanda.bunnyhop.node.view.factory;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * {@link BhNodeView} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public interface BhNodeViewFactory {

  /**
   * {@code node} に対応する {@link BhNodeView} を作成する.
   *
   * @param node この {@link BhNode} に対応する {@link BhNodeView} オブジェクトを作成する
   * @param isTemplate テンプレートノードビューを作成する場合 true
   * @return 作成した {@link BhNodeView} オブジェクト
   */
  BhNodeView createViewOf(BhNode node, boolean isTemplate) throws ViewConstructionException;

  /**
   * 疑似ビュー (= モデルを持たないビュー) を表す文字列から {@link BhNodeView} を作成する.
   *
   * @param specification 疑似ビューの構成を指定した文字列
   * @param isTemplate テンプレートノードビューを作成する場合 true
   * @return 作成した {@link BhNodeView} オブジェクト
   */
  BhNodeView createViewOf(String specification, boolean isTemplate)
      throws ViewConstructionException;
}
