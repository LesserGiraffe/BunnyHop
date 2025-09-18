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

package net.seapanda.bunnyhop.model.node.derivative;

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 派生ノードツリーを作成するクラス.
 *
 * @author K.Koike
 */
public class DerivativeBuilder implements BhNodeWalker {

  /** 現在処理中の {@link BhNode} の親がトップにくるスタック. */
  private final Deque<Derivative> parentStack = new LinkedList<>();
  /** undo 用コマンドオブジェクト. */
  UserOperation userOpe;
  private DerivationId derivationId = DerivationId.NONE;

  /**
   * {@code node} の {@code derivationId} に対応する派生ノードを作成する.
   *
   * @param node このノードの派生ノードを作成する
   * @param derivationId この派生先 ID に対応する派生ノードを作成する.  {@code node} に対して, この派生先 ID が定義されていること.
   * @param userOpe undo 用コマンドオブジェクト
   * @return 作成した派生ノードツリーのトップノード
   */
  public static Derivative build(
      Derivative node, DerivationId derivationId, UserOperation userOpe) {
    var builder = new DerivativeBuilder(derivationId, userOpe);
    node.accept(builder);
    return builder.parentStack.peekLast();
  }

  private DerivativeBuilder(DerivationId derivationId, UserOperation userOpe) {
    this.derivationId = derivationId;
    this.userOpe = userOpe;
  }

  /**
   * {@code node} の派生ノードを作成する.
   */
  @Override
  public void visit(ConnectiveNode node) {
    DerivationId dervId = DerivationId.NONE;
    if (!derivationId.equals(DerivationId.NONE)) {
      dervId = derivationId;
      derivationId = DerivationId.NONE;
    } else {
      dervId = node.findDerivationIdUp();
    }

    if (!node.hasDerivativeOf(dervId)) {
      return;
    }
    if (parentStack.isEmpty()) {
      ConnectiveNode newDerivation = node.createDerivative(dervId, userOpe);
      parentStack.addLast(newDerivation);
      node.sendToSections(this);
    } else {
      Derivative parent = parentStack.peekLast();
      //接続先を探す
      BhNode toBeReplaced =
          DerivativeFinder.find(parent, node.getParentConnector().getDerivativeJoint());
      if (toBeReplaced != null) {
        ConnectiveNode newDerivative = node.createDerivative(dervId, userOpe);
        toBeReplaced.replace(newDerivative, userOpe);
        parentStack.addLast(newDerivative);
        node.sendToSections(this);
        parentStack.removeLast();
      }
    }
  }

  @Override
  public void visit(TextNode node) {
    DerivationId dervId = DerivationId.NONE;
    if (!derivationId.equals(DerivationId.NONE)) {
      dervId = derivationId;
      derivationId = DerivationId.NONE;
    } else {
      dervId = node.findDerivationIdUp();
    }

    if (!node.hasDerivativeOf(dervId)) {
      return;
    }
    if (parentStack.isEmpty()) {
      TextNode newDerivation = node.createDerivative(dervId, userOpe);
      parentStack.addLast(newDerivation);
    } else {
      Derivative parent = parentStack.peekLast();
      //接続先を探す
      BhNode toBeReplaced =
          DerivativeFinder.find(parent, node.getParentConnector().getDerivativeJoint());
      if (toBeReplaced != null) {
        TextNode newDerivative = node.createDerivative(dervId, userOpe);
        toBeReplaced.replace(newDerivative, userOpe);
      }
    }
  }
}
