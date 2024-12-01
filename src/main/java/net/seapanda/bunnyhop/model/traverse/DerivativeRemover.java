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

package net.seapanda.bunnyhop.model.traverse;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 走査したノードが派生ノードであった場合, そのオリジナルノードの派生ノード一覧から取り除くクラス.
 *
 * @author K.Koike
 */
public class DerivativeRemover implements BhNodeWalker {

  /** undo 用コマンドオブジェクト. */
  private UserOperation userOpe;

  /**
   * {@code node} で指定ノード以下にある派生ノードを, そのオリジナルノードの派生ノード一覧から取り除く.
   *
   * @param node このノード以下の派生ノードを, そのオリジナルノードの派生ノード一覧から取り除く.
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public static void remove(BhNode node, UserOperation userOpe) {
    node.accept(new DerivativeRemover(userOpe));
  }

  /**
   * コンストラクタ.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  private DerivativeRemover(UserOperation userOpe) {
    this.userOpe = userOpe;
  }

  /**
   * {@code node} の削除処理を行う.
   *
   * @param node 削除するノード
   */
  @Override
  public void visit(ConnectiveNode node) {
    if (node.isDerivative()) {
      node.getOriginal().removeDerivative(node, userOpe);
    }
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    if (node.isDerivative()) {
      node.getOriginal().removeDerivative(node, userOpe);
    }
  }
}
