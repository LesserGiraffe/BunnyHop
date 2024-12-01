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

package net.seapanda.bunnyhop.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * 木構造のノード.
 *
 * @author K.Koike
 */
public class TreeNode<T> {

  public final T content;
  private TreeNode<T> parent = null;
  private final List<TreeNode<T>> children = new ArrayList<>();

  public TreeNode(T content) {
    this.content = content;
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  public boolean isRoot() {
    return parent == null;
  }

  /** 子要素を追加する. */
  public void addChild(TreeNode<T> child) {
    child.parent = this;
    if (!children.contains(child) && child != null) {
      children.add(child);
    }
  }

  /** 子要素を取り除く. */
  public void removeChild(TreeNode<T> child) {
    if (children.remove(child)) {
      child.parent = null;
    }
  }

  /** {@code index} 番目の子要素を取得する. */
  public TreeNode<T> getChildAt(int index) {
    return children.get(index);
  }

  /** 全ての子要素を取得する. 子がない場合は, 空のイテレータを返す. */
  public Iterable<TreeNode<T>> getChildren() {
    return children;
  }

  /** 親要素を取得する. 親がいない場合は null を返す. */
  public TreeNode<T> getParent() {
    return parent;
  }
}
