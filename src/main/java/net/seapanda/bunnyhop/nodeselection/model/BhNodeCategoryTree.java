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

package net.seapanda.bunnyhop.nodeselection.model;

import net.seapanda.bunnyhop.utility.TreeNode;

/**
 * ノードのカテゴリ一覧を提供する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeCategoryTree {
  
  /**
   * ノードのカテゴリ一覧を格納した木のルートノードを返す.
   *
   * @return ノードのカテゴリ一覧を格納した木のルートノード
   */
  TreeNode<String> getRoot();
}
