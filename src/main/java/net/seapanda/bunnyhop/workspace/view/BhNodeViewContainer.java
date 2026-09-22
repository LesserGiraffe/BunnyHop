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

import net.seapanda.bunnyhop.node.view.BhNodeView;

/**
 * {@link BhNodeView} を保持するクラスが共通で持つインタフェース.
 */
public interface BhNodeViewContainer {

  /**
   * {@code view} をこのコンテナに追加する.
   *
   * @param view 追加する {@link BhNodeView}
   */
  void addNodeView(BhNodeView view);

  /**
   * {@code view} をこのコンテナから削除する.
   *
   * @param view 削除する {@link BhNodeView}
   */
  void removeNodeView(BhNodeView view);
}
