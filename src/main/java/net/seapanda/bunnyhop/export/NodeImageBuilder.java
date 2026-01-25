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

package net.seapanda.bunnyhop.export;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.PositionManager;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * {@link BhNode} ツリーの保存用イメージを作成するクラス.
 *
 * @author K.Koike
 */
public class NodeImageBuilder implements BhNodeWalker {

  private final Deque<BhNodeImage> nodeStack = new LinkedList<>();

  /**
   * {@code node} で指定したノードをルートとするツリーと同じ親子構造を持つ {@link BhNodeImage} のツリーを作成する.
   *
   * @return 作成した {@link BhNodeImage} ツリーのルートノード.
   */
  public static BhNodeImage build(BhNode node) {
    Objects.requireNonNull(node);
    var builder = new NodeImageBuilder();
    node.accept(builder);
    return builder.nodeStack.getLast();
  }

  @Override
  public void visit(ConnectiveNode node) {
    List<InstanceId> derivationIds = node.getDerivatives().stream()
        .filter(NodeImageBuilder::isNotTemplate)
        .map(derivative -> derivative.getInstanceId())
        .toList();
    Vec2D pos = node.getView()
        .map(view -> view.getPositionManager().getPosOnWorkspace())
        .orElse(new Vec2D());

    var nodeImage = new BhNodeImage(
        node.getId(),
        node.getInstanceId(),
        derivationIds,
        "",
        node.isDefault(),
        node.isBreakpointSet(),
        node.isCorrupted(),
        node.getVersion(),
        pos);
    nodeStack.addLast(nodeImage);
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    List<InstanceId> derivationIds = node.getDerivatives().stream()
        .filter(NodeImageBuilder::isNotTemplate)
        .map(derivative -> derivative.getInstanceId())
        .toList();
    Vec2D pos = node.getView()
        .map(BhNodeView::getPositionManager)
        .map(PositionManager::getPosOnWorkspace)
        .orElse(new Vec2D());

    var nodeImage = new BhNodeImage(
        node.getId(),
        node.getInstanceId(),
        derivationIds,
        node.getText(),
        node.isDefault(),
        node.isBreakpointSet(),
        node.isCorrupted(),
        node.getVersion(),
        pos);
    nodeStack.addLast(nodeImage);
  }

  @Override
  public void visit(Connector cnctr) {
    cnctr.sendToConnectedNode(this);
    BhNodeImage snapshotImage = createDefaultNodeSnapshotImage(cnctr);
    var image = new ConnectorImage(
        cnctr.getInstanceId(),
        cnctr.getId(),
        nodeStack.removeLast(),
        snapshotImage);
    nodeStack.peekLast().addChild(image);
  }

  /** {@code cnctr} に最後に接続されていたデフォルトノードのスナップショットの保存用イメージを作成する. */
  private static BhNodeImage createDefaultNodeSnapshotImage(Connector cnctr) {
    return cnctr.getLastDefaultNodeSnapshot().map(snapshot -> {
      var builder = new NodeImageBuilder();
      snapshot.accept(builder);
      return builder.nodeStack.getLast();
    }).orElse(null);
  }

  /** {@code node} がテンプレートノードか調べる. */
  private static boolean isNotTemplate(BhNode node) {
    while (node != null) {
      if (node.getView().isPresent()) {
        return !node.getView().get().isTemplate();
      }
      node = node.findParentNode();
    }
    return true;
  }  
}
