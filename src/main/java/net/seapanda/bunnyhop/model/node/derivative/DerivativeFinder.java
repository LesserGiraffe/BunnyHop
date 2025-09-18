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

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.model.node.section.ConnectorSection;
import net.seapanda.bunnyhop.model.node.section.Subsection;
import net.seapanda.bunnyhop.model.node.traverse.BhNodeWalker;

/**
 * 派生ノード接続位置を指定し, そこに接続されている {@link Derivative} を見つけるクラス.
 *
 * @author K.Koike
 */
public class DerivativeFinder implements BhNodeWalker {

  /** 見つかったノード. */
  private Derivative foundNode;
  /** この ID を持つコネクタに接続されている {@link BhNode} を探す. */
  private DerivativeJointId joint;
  private boolean found = false;

  /**
   * 派生ノード接続位置を指定し, そこに接続されている {@link BhNode} を見つける.
   *
   * @param node これ以下のノードから, {@code joint} と同じ派生ノード接続位置が定義されたコネクタに接続されているノードを見つける.
   * @param joint 派生ノード接続位置.
   * @return 見つかったノード. 見つからなかった場合は null.
   */
  public static Derivative find(BhNode node, DerivativeJointId joint) {
    var finder = new DerivativeFinder(joint);
    node.accept(finder);
    return finder.foundNode;
  }

  /**
   * コンストラクタ.
   *
   * @param joint この派生ノード接続位置を持つコネクタに接続されている {@link Derivative} を見つける
   */
  private DerivativeFinder(DerivativeJointId joint) {
    this.joint = joint;
  }

  @Override
  public void visit(ConnectiveNode node) {
    node.sendToSections(this);
  }

  @Override
  public void visit(Subsection section) {
    if (found) {
      return;
    }
    section.sendToSubsections(this);
  }

  @Override
  public void visit(ConnectorSection connectorGroup) {
    if (found) {
      return;
    }
    connectorGroup.sendToConnectors(this);
  }

  @Override
  public void visit(Connector connector) {
    if (found) {
      return;
    }
    if (connector.getDerivativeJoint().equals(joint)) {
      if (connector.getConnectedNode() instanceof Derivative derv) {
        foundNode = derv;
        found = true;
      }
    }
  }
}
