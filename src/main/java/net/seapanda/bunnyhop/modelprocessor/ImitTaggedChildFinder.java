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

package net.seapanda.bunnyhop.modelprocessor;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorSection;
import net.seapanda.bunnyhop.model.node.connective.Subsection;
import net.seapanda.bunnyhop.model.node.imitation.ImitCnctPosId;

/**
 * イミテーションタグを指定し, そこに接続されている {@link BhNode} を見つけるクラス.
 *
 * @author K.Koike
 */
public class ImitTaggedChildFinder implements BhModelProcessor {

  /** 見つかったノード. */
  private BhNode foundNode;
  /** この ID を持つコネクタに接続されている {@link BhNode} を探す. */
  private ImitCnctPosId imitCnctPosId;
  private boolean found = false;

  /**
   * イミテーションタグを指定し, そこに接続されているBhNode を見つける.
   *
   * @param node これ以下のノードから, イミテーションタグに一致する場所に接続されているノードを見つける.
   * @param imitCnctPos イミテーションタグ. (イミテーションの接続位置を識別するタグ)
   */
  public static BhNode find(BhNode node, ImitCnctPosId imitCnctPos) {
    var finder = new ImitTaggedChildFinder(imitCnctPos);
    node.accept(finder);
    return finder.foundNode;
  }

  /**
   * コンストラクタ.
   *
   * @param imitCnctPosId このイミテーションタグを持つコネクタにつながったBhNodeを見つける
   */
  private ImitTaggedChildFinder(ImitCnctPosId imitCnctPosId) {
    this.imitCnctPosId = imitCnctPosId;
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
    if (connector.getImitCnctPoint().equals(imitCnctPosId)) {
      foundNode = connector.getConnectedNode();
      found = true;
    }
  }
}
