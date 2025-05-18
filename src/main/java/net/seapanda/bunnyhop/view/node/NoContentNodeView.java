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

package net.seapanda.bunnyhop.view.node;

import java.util.Optional;
import java.util.SequencedSet;
import javafx.scene.Node;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;

/** 内部に何も表示しないノードビュー. */
public class NoContentNodeView extends BhNodeViewBase {

  private final TextNode model;
  /** コネクタ部分を含まないノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** コネクタ部分を含むノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());

  /**
   * コンストラクタ.
   *
   * @param model ビューに対応するモデル
   * @param viewStyle ビューのスタイル
   */
  public NoContentNodeView(
      TextNode model, BhNodeViewStyle viewStyle, SequencedSet<Node> components) 
      throws ViewConstructionException {
    super(viewStyle, model, components);
    this.model = model;
    getLookManager().addCssClass(BhConstants.Css.CLASS_NO_CONTENT_NODE);
    nodeBase.setMouseTransparent(true);
    getLookManager().setBodyShapeGetter(() -> {
      boolean inner = (parent == null) ? true : parent.inner;
      return inner ? viewStyle.bodyShape : BodyShape.BODY_SHAPE_NONE;
    });
  }

  /** ノードサイズのキャッシュ値を更新する. */
  private void updateNodeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeSizeCache.update(new Vec2D(nodeSize));
    }
  }

  /**
   * このビューのモデルであるBhNodeを取得する.
   *
   * @return このビューのモデルであるBhNode
   */
  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    if (includeCnctr && !nodeWithCnctrSizeCache.isDirty()) {
      return new Vec2D(nodeWithCnctrSizeCache.getVal());
    }
    if (!includeCnctr && !nodeSizeCache.isDirty()) {
      return new Vec2D(nodeSizeCache.getVal());
    }

    double paddingLeft = 0.0;
    double paddingRight = 0.0;
    double paddingTop = 0.0;
    double paddingBottom = 0.0;

    boolean inner = (parent == null) ? true : parent.inner;
    if (inner) {
      paddingLeft = viewStyle.paddingLeft;
      paddingRight = viewStyle.paddingRight;
      paddingTop = viewStyle.paddingTop;
      paddingBottom = viewStyle.paddingBottom;
    }

    Vec2D cnctrSize = getRegionManager().getConnectorSize();
    double bodyWidth = paddingLeft + paddingRight;
    double bodyHeight = paddingTop + paddingBottom;
    
    if (includeCnctr) {
      var wholeSize = new Vec2D();
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        wholeSize = calcSizeIncludingLeftConnector(bodyWidth, bodyHeight, cnctrSize);
      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        wholeSize = calcSizeIncludingTopConnector(bodyWidth, bodyHeight, cnctrSize);
      }
      bodyWidth = wholeSize.x;
      bodyHeight = wholeSize.y;
    }
    var nodeSize = new Vec2D(bodyWidth, bodyHeight);
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
  }

  /**
   * 左にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodyWidth ボディ部分の幅
   * @param bodyHeight ボディ部分の高さ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingLeftConnector(
      double bodyWidth, double bodyHeight, Vec2D cnctrSize) {
    double wholeWidth = bodyWidth + cnctrSize.x;
    // ボディの左上を原点としたときのコネクタの上端の座標
    double cnctrTopPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrTopPos += (bodyHeight - cnctrSize.y) / 2;
    }
    // ボディの左上を原点としたときのコネクタの下端の座標
    double cnctrBottomPos = cnctrTopPos + cnctrSize.y;
    double wholeHeight = Math.max(cnctrBottomPos, bodyHeight) - Math.min(cnctrTopPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }

  /**
   * 上にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodyWidth ボディ部分の幅
   * @param bodyHeight ボディ部分の高さ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingTopConnector(
      double bodyWidth, double bodyHeight, Vec2D cnctrSize) {
    double wholeHeight = bodyHeight + cnctrSize.y;
    // ボディの左上を原点としたときのコネクタの左端の座標
    double cnctrLeftPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrLeftPos += (bodyWidth - cnctrSize.x) / 2;
    }
    // ボディの左上を原点としたときのコネクタの右端の座標
    double cnctrRightPos = cnctrLeftPos + cnctrSize.x;
    double wholeWidth = Math.max(cnctrRightPos, bodyWidth) - Math.min(cnctrLeftPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }

  @Override
  protected Vec2D getNodeTreeSize(boolean includeCnctr) {
    return getNodeSize(includeCnctr);
  }

  @Override
  protected void updateChildRelativePos() {}

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    System.out.println(
        "%s<NoContentNodeView>  %s".formatted(indent(depth), hashCode()));
  }
}
