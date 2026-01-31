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

package net.seapanda.bunnyhop.node.model.derivative;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * 走査したノードが派生ノードであった場合, そのオリジナルノードの派生ノード一覧から取り除くクラス.
 *
 * @author K.Koike
 */
public class DerivativeUnlinker implements BhNodeWalker {

  /** undo 用コマンドオブジェクト. */
  private final UserOperation userOpe;
  private final boolean walk;

  /**
   * {@code node} 以下にある派生ノードを, そのオリジナルノードの派生ノード一覧から取り除く.
   *
   * @param node このノード以下の派生ノードを, そのオリジナルノードの派生ノード一覧から取り除く.
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public static void unlink(BhNode node, UserOperation userOpe) {
    unlink(node, true, userOpe);
  }

  /**
   * {@code node} をそのオリジナルノードの派生ノード一覧から取り除く.
   *
   * @param node 派生ノード一覧から取り除く対象のノード
   * @param walk true の場合 {@code node} 以下の派生ノードも取り除く.
   *             false の場合 {@code node} が派生ノードの場合のみ取り除く
   * @param userOpe undo 用コマンドオブジェクト
   */
  public static void unlink(BhNode node, boolean walk, UserOperation userOpe) {
    node.accept(new DerivativeUnlinker(userOpe, walk));
  }

  /**
   * コンストラクタ.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  private DerivativeUnlinker(UserOperation userOpe, boolean walk) {
    this.userOpe = userOpe;
    this.walk = walk;
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
    if (walk) {
      node.sendToSections(this);
    }
  }

  @Override
  public void visit(TextNode node) {
    if (node.isDerivative()) {
      node.getOriginal().removeDerivative(node, userOpe);
    }
  }
}
