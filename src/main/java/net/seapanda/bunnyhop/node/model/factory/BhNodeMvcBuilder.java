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

package net.seapanda.bunnyhop.node.model.factory;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;

/**
 * {@link BhNode} の MVC 構造を構築する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeMvcBuilder {

  /**
   * {@code node} 以下のノードに対し MVC 構造を作成する.
   *
   * @param node このノード以下のノードの MVC 構造を作成する
   * @param type 作成する MVC 構造のタイプ.
   *             {@link MvcType#NONE} を指定した場合, 何もしない.
   * @return 引数で指定したノードに対応する {@link BhNodeView}.
   *         View の作成に失敗した場合 null.
   */
  BhNodeView build(BhNode node, MvcType type);
}
