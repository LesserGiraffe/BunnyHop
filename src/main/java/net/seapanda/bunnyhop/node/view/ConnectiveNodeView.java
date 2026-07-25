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

import java.util.Optional;
import java.util.SequencedSet;
import javafx.scene.Node;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ChildArrangement;
import net.seapanda.bunnyhop.node.view.style.ConnectorOrientation;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem;

/**
 * {@link ConnectiveNode} に対応するビュークラス.
 *
 * @author K.Koike
 */
public final class ConnectiveNodeView extends BhNodeViewBase {

  /** ノード内部に描画されるノードの Group. */
  private final BhNodeViewGroup innerGroup = new BhNodeViewGroup(this, true);
  /** ノード外部に描画されるノードのGroup. */
  private final BhNodeViewGroup outerGroup = new BhNodeViewGroup(this, false);
  private final ConnectiveNode model;
  private final Geometry geometry;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @param factory サブグループ内の疑似ビューを作成するのに使用するオブジェクト
   * @throws ViewConstructionException ノードビューの初期化に失敗した
   */
  public ConnectiveNodeView(
      ConnectiveNode model,
      BhNodeViewStyle style,
      SequencedSet<Node> components,
      boolean isTemplate,
      BhNodeViewFactory factory)
      throws ViewConstructionException {
    super(style, model, components, isTemplate);
    this.model = model;
    innerGroup.buildSubGroup(style.connective.inner, factory, isTemplate);
    outerGroup.buildSubGroup(style.connective.outer, factory, isTemplate);
    var qtsReg = getQuadTreeSpaceRegistration();
    geometry = new Geometry(qtsReg.getBodyQtItem(), qtsReg.getConnectorQtItem());
    getVisual().addCssClass(BhConstants.Css.Class.CONNECTIVE_NODE);
  }

  /**
   * 子ノードを追加する.
   *
   * @param view 追加する子ノード
   * @return 追加に成功した場合 true. 失敗した場合 false.
   */
  public boolean addChild(BhNodeView view) {
    if (!(view instanceof BhNodeViewBase viewBase)) {
      return false;
    }
    // innerGroup に追加できなかった場合は outerGroup に入れる
    if (!innerGroup.addNodeView(viewBase)) {
      return outerGroup.addNodeView(viewBase);
    }
    return true;
  }

  /** {@code visitor} を内部ノードを管理するグループに渡す. */
  public void sendToInnerGroup(NodeViewWalker visitor) {
    innerGroup.accept(visitor);
  }

  /** {@code visitor} を外部ノードを管理するグループに渡す. */
  public void sendToOuterGroup(NodeViewWalker visitor) {
    outerGroup.accept(visitor);
  }

  @Override
  public Optional<ConnectiveNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }


  /** ノードビューの幾何形状に関する操作を提供するクラス. */
  public class Geometry extends GeometryBase {

    private final NodeSizeCalculator sizeCalculator;
    /** 子孫ノードビューの親要素からの相対位置が最新の値かどうかを示すフラグ. */
    private boolean isRelativePositionUpToDate = false;

    private Geometry(QuadTreeItem bodyItem, QuadTreeItem cnctrItem) {
      super(ConnectiveNodeView.this, bodyItem, cnctrItem);
      ConnectiveNodeView view = ConnectiveNodeView.this;
      sizeCalculator = new NodeSizeCalculator(view, view.innerGroup::getSize, this::calcOuterSize);
      view.getSizeChangeNotifier().setOnDescendantSizeChanged(this::onDescendantSizeChanged);
    }

    void onDescendantSizeChanged() {
      sizeCalculator.notifyNodeSizeChanged();
      isRelativePositionUpToDate = false;
    }


    @Override
    void updateTreePosition(double posX, double posY) {
      //内部ノード絶対位置更新
      Vec2D relativePos = innerGroup.getRelativePosition();
      innerGroup.updateTreePosition(posX + relativePos.x, posY + relativePos.y);

      //外部ノード絶対位置更新
      BhNodeViewStyle style = ConnectiveNodeView.this.getStyle();
      Vec2D bodySize = getNodeSize(false);
      //外部ノードが右に繋がる
      if (style.connectorOrientation == ConnectorOrientation.LEFT) {
        outerGroup.updateTreePosition(posX + bodySize.x + style.connective.outerOffset, posY);
      //外部ノードが下に繋がる
      } else {
        outerGroup.updateTreePosition(posX, posY + bodySize.y + style.connective.outerOffset);
      }
      setComponentPositions(posX, posY);
      setPositionsOnQuadTreeSpace(posX, posY);
    }

    @Override
    public Vec2D getNodeSize(boolean includeCnctr) {
      return sizeCalculator.calcNodeSize(includeCnctr);
    }

    @Override
    public Vec2D getNodeTreeSize(boolean includeCnctr) {
      return sizeCalculator.calcNodeTreeSize(includeCnctr);
    }

    @Override
    void updateDescendantRelativePositions() {
      if (isRelativePositionUpToDate) {
        return;
      }
      BhNodeViewStyle style = ConnectiveNodeView.this.getStyle();
      Vec2D innerRelPos = new Vec2D(style.paddingLeft, style.paddingTop);
      Vec2D commonPartSize = geometry.getCommonPartSize();
      if (style.baseArrangement == ChildArrangement.ROW) {
        innerRelPos.x += commonPartSize.x;
      } else if (style.baseArrangement == ChildArrangement.COLUMN) {
        innerRelPos.y += commonPartSize.y;
      }
      innerGroup.setRelativePosition(innerRelPos.x, innerRelPos.y);
      innerGroup.updateDescendantRelativePositions();

      Vec2D bodySize = geometry.getNodeSize(false);
      // 外部ノードが右に繋がる
      if (style.connectorOrientation == ConnectorOrientation.LEFT) {
        outerGroup.setRelativePosition(bodySize.x + style.connective.outerOffset, 0.0);
      // 外部ノードが下に繋がる
      } else if (style.connectorOrientation == ConnectorOrientation.TOP) {
        outerGroup.setRelativePosition(0.0, bodySize.y + style.connective.outerOffset);
      }
      outerGroup.updateDescendantRelativePositions();
      isRelativePositionUpToDate = true;
    }

    /** 外部ノードグループのサイズを計算する. */
    private Vec2D calcOuterSize() {
      BhNodeViewStyle style = ConnectiveNodeView.this.getStyle();
      Vec2D outerSize = outerGroup.getSize();
      if (style.connectorOrientation == ConnectorOrientation.LEFT) {
        outerSize.x = Math.max(outerSize.x + style.connective.outerOffset, 0);
      } else if (style.connectorOrientation == ConnectorOrientation.TOP) {
        outerSize.y = Math.max(outerSize.y + style.connective.outerOffset, 0);
      }
      return outerSize;
    }
  }
}
