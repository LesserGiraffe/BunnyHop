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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import net.seapanda.bunnyhop.export.BhNodeImage;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.utility.Vec2D;

/**
 * {@link BhNode} ツリーの保存用イメージを作成するクラス.
 *
 * @author K.koike
 */
public class NodeImageBuilder implements BhNodeWalker {
 
  /** {@link BhNodeImage} ツリーのルート. */
  private BhNodeImage root;
  /** 子 {@link BhNodeImage} を追加する親要素のスタック. */
  private final Deque<BhNodeImage> parentStack = new LinkedList<>();

  /**
   * {@code node} で指定したノードをルートとするツリーと同じ親子構造を持つ {@link BhNodeImage} のツリーを作成する.
   *
   * @return 作成した {@link BhNodeImage} ツリーのルートノード.
   */
  public static BhNodeImage build(BhNode node) {
    Objects.requireNonNull(node);
    var builer = new NodeImageBuilder();
    node.accept(builer);
    return builer.root;
  }

  private NodeImageBuilder() { }

  @Override
  public void visit(ConnectiveNode node) {
    List<InstanceId> derivationIds = node.getDerivatives().stream()
        .filter(derivative -> !derivative.getViewProxy().isTemplateNode())
        .map(derivative -> derivative.getInstanceId())
        .toList();
    Connector parentCnctr = node.getParentConnector();
    ConnectorId cnctrId = (parentCnctr == null) ? ConnectorId.NONE : parentCnctr.getId();
    Vec2D pos = node.getViewProxy().getPosOnWorkspace();
    pos = (pos == null) ? new Vec2D(0, 0) : pos; // View を持たない BhNode もある
    var nodeImage = new BhNodeImage(
        node.getId(),
        node.getInstanceId(),
        derivationIds,
        "",
        node.isDefault(),
        cnctrId,
        node.getVersion(),
        pos);
    
    if (!parentStack.isEmpty()) {
      parentStack.peekLast().addChild(nodeImage);
    }
    parentStack.addLast(nodeImage);
    root = (parentCnctr == null) ? nodeImage : root;
    node.sendToSections(this);
    parentStack.removeLast();
  }

  @Override
  public void visit(TextNode node) {
    List<InstanceId> derivationIds = node.getDerivatives().stream()
        .filter(derivatie -> !derivatie.getViewProxy().isTemplateNode())
        .map(derivatie -> derivatie.getInstanceId())
        .toList();
    Connector parentCnctr = node.getParentConnector();
    ConnectorId cnctrId = (parentCnctr == null) ? ConnectorId.NONE : parentCnctr.getId();
    Vec2D pos = node.getViewProxy().getPosOnWorkspace();
    pos = (pos == null) ? new Vec2D(0, 0) : pos;
    var nodeImage = new BhNodeImage(
        node.getId(),
        node.getInstanceId(),
        derivationIds,
        node.getText(),
        node.isDefault(),
        cnctrId,
        node.getVersion(),
        pos);

    if (!parentStack.isEmpty()) {
      parentStack.peekLast().addChild(nodeImage);
    }
    root = (parentCnctr == null) ? nodeImage : root;
  }
}
