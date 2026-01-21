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
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;

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
  private final NodeSizeCalculator sizeCalculator;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param factory サブグループ内の疑似ビューを作成するのに使用するオブジェクト
   * @throws ViewConstructionException ノードビューの初期化に失敗した
   */
  public ConnectiveNodeView(
      ConnectiveNode model,
      BhNodeViewStyle style,
      SequencedSet<Node> components,
      BhNodeViewFactory factory)
      throws ViewConstructionException {
    super(style, model, components);
    this.model = model;
    sizeCalculator = new NodeSizeCalculator(this, innerGroup::getSize, this::calcOuterSize);
    innerGroup.buildSubGroup(style.connective.inner, factory);
    outerGroup.buildSubGroup(style.connective.outer, factory);
    getLookManager().addCssClass(BhConstants.Css.CLASS_CONNECTIVE_NODE);
  }

  @Override
  public Optional<ConnectiveNode> getModel() {
    return Optional.ofNullable(model);
  }

  /**
   * ノード内部に描画されるノードを追加する.
   *
   * @param view ノード内部に描画されるノード
   */
  public void addToGroup(BhNodeViewBase view) {
    // innerGroup に追加できなかった場合は outerGroup に入れる
    if (!innerGroup.addNodeView(view)) {
      outerGroup.addNodeView(view);
    }
  }

  @Override
  protected void notifyChildSizeChanged() {
    sizeCalculator.notifyNodeSizeChanged();
    super.notifyChildSizeChanged();
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);

    //内部ノード絶対位置更新
    Vec2D relativePos = innerGroup.getRelativePosFromParent();
    innerGroup.updateTreePosOnWorkspace(posX + relativePos.x, posY + relativePos.y);

    //外部ノード絶対位置更新
    Vec2D bodySize = getRegionManager().getNodeSize(false);
    //外部ノードが右に繋がる
    if (style.connectorPos == ConnectorPos.LEFT) {
      outerGroup.updateTreePosOnWorkspace(
          posX + bodySize.x + style.connective.outerOffset, posY);
    //外部ノードが下に繋がる
    } else {
      outerGroup.updateTreePosOnWorkspace(
          posX, posY + bodySize.y + style.connective.outerOffset);
    }
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    return sizeCalculator.calcNodeSize(includeCnctr);
  }

  @Override
  protected Vec2D getNodeTreeSize(boolean includeCnctr) {
    return sizeCalculator.calcNodeTreeSize(includeCnctr);
  }

  /** 外部ノードグループのサイズを計算する. */
  private Vec2D calcOuterSize() {
    Vec2D outerSize = outerGroup.getSize();
    if (style.connectorPos == ConnectorPos.LEFT) {
      outerSize.x = Math.max(outerSize.x + style.connective.outerOffset, 0);
    } else if (style.connectorPos == ConnectorPos.TOP) {
      outerSize.y = Math.max(outerSize.y + style.connective.outerOffset, 0);
    }
    return outerSize;
  }

  /** ノードの親からの相対位置を更新する. */
  @Override
  protected void updateChildRelativePos() {
    Vec2D innerRelPos = new Vec2D(style.paddingLeft, style.paddingTop);
    Vec2D commonPartSize = getRegionManager().getCommonPartSize();
    if (style.baseArrangement == ChildArrangement.ROW) {
      innerRelPos.x += commonPartSize.x;
    } else if (style.baseArrangement == ChildArrangement.COLUMN) {
      innerRelPos.y += commonPartSize.y;
    }
    innerGroup.setRelativePosFromParent(innerRelPos.x, innerRelPos.y);
    innerGroup.updateChildRelativePos();

    Vec2D bodySize = getRegionManager().getNodeSize(false);
    // 外部ノードが右に繋がる
    if (style.connectorPos == ConnectorPos.LEFT) {
      outerGroup.setRelativePosFromParent(bodySize.x + style.connective.outerOffset, 0.0);
    // 外部ノードが下に繋がる
    } else if (style.connectorPos == ConnectorPos.TOP) {
      outerGroup.setRelativePosFromParent(0.0, bodySize.y + style.connective.outerOffset);
    }
    outerGroup.updateChildRelativePos();
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
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }
}
