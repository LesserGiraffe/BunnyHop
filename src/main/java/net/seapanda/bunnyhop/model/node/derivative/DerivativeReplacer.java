/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenss/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.model.node.derivative;

import java.util.Set;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 派生ノードの入れ替えメソッドを定義したインタフェース.
 *
 * @author K.Koike
 */
public interface DerivativeReplacer {
  
  /**
   * <pre>
   * {@code orgOfOldDervs} の派生ノード一式を E とする
   * {@code orgOfNewDervs} の先祖コネクタが持つ派生先 ID を p とする
   * {@code orgOfNewDervs} の p に対応する派生ノードを q とする
   * {@code orgOfNewDervs} の親コネクタが持つ, 派生ノード接続先 ID を r とする.
   * {@code orgOfNewDervs} の親ノードが持つ m 個の派生ノードを D(0) ~ D(m-1) とする
   * D(i) の子コネクタの中で r が指定してあるものがあれば, それを D_r(i) とする.  (i = 0, 1, 2, ..., m - 1)
   * D_r(i) の子ノードを C_r(i) とする.
   * 
   * C_r(i) を q と入れ替える. (q は入れ替える個数分用意する)
   * q が存在しない場合, C_r(i) ∈ E であったなら, その C_r(i) を取り除く.
   * </pre>
   *
   * @param orgOfNewDervs このノードの派生ノードで {@code orgOfOldDervs} の派生ノードを置き換える.
   * @param orgOfOldDervs このノードの派生ノードを取り除くまたは入れ替える.
   * @param userOpe undo 用コマンドオブジェクト
   */
  Set<Swapped> replace(BhNode orgOfNewDervs, BhNode orgOfOldDervs, UserOperation userOpe);
}
