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

package net.seapanda.bunnyhop.common;

import java.util.ArrayList;

/**
 * 木構造のノード.
 *
 * @author K.Koike
 */
public class TreeNode<T> {

  public T content;
  public ArrayList<TreeNode<T>> children = new ArrayList<>();

  public boolean isLeaf() {
    return children.isEmpty();
  }

  public TreeNode(T content) {
    this.content = content;
  }
}
