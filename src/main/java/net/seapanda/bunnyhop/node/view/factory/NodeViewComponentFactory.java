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

import java.util.SequencedSet;
import javafx.scene.Node;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * ノードビューが持つ可能性のある共通の UI コンポーネントを作成する機能を規定したインタフェース
 *
 * @author K.Koike
 */
public interface NodeViewComponentFactory {
  
  /**
   * {@code node} のノードビューが持つ UI コンポーネントを作成する.
   *
   * @param node このノードのノードビューが持つ UI コンポーネントを作成する
   * @return UI コンポーネントのリスト
   */
  SequencedSet<Node> create(BhNode node) throws ViewConstructionException;
}
