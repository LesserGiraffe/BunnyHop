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

package net.seapanda.bunnyhop.node.view;

import java.util.SequencedSet;
import javafx.scene.Node;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem;

/**
 * 子要素を持たない NodeView の基底クラス.
 *
 * @author K.Koike
 */
abstract class LeafNodeView extends BhNodeViewBase {

  private final Geometry geometry;

  /** コンテンツを表示する領域の大きさを取得する. */
  abstract Vec2D getContentRegionSize();

  LeafNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(style, model, components, isTemplate);
    var qtsReg = getQuadTreeSpaceRegistration();
    geometry = new Geometry(qtsReg.getBodyQtItem(), qtsReg.getConnectorQtItem());
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  /** ノードビューの幾何形状に関する操作を提供するクラス. */
  public class Geometry extends GeometryBase {

    private final NodeSizeCalculator sizeCalculator;

    private Geometry(QuadTreeItem bodyItem, QuadTreeItem cnctrItem) {
      super(LeafNodeView.this, bodyItem, cnctrItem);
      sizeCalculator =
          new NodeSizeCalculator(LeafNodeView.this, LeafNodeView.this::getContentRegionSize);
      LeafNodeView.this.getSizeChangeNotifier().setOnDescendantSizeChanged(
          sizeCalculator::notifyNodeSizeChanged);
    }

    @Override
    void updateTreePosition(double posX, double posY) {
      setComponentPositions(posX, posY);
      setPositionsOnQuadTreeSpace(posX, posY);
    }

    @Override
    public Vec2D getNodeSize(boolean includeCnctr) {
      return sizeCalculator.calcNodeSize(includeCnctr);
    }

    @Override
    public Vec2D getNodeTreeSize(boolean includeCnctr) {
      return getNodeSize(includeCnctr);
    }

    @Override
    void updateDescendantRelativePositions() {}
  }
}
