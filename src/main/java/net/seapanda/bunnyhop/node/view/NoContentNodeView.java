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
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.node.view.style.NotchPos;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/** 内部に何も表示しないノードビュー. */
public class NoContentNodeView extends BhNodeViewBase {

  private final TextNode model;
  /** コネクタ部分を含まないノードサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<>(new Vec2D());
  /** コネクタ部分を含むノードサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<>(new Vec2D());

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
    setMouseTransparent(true);
    // 親グループに依存してノードの大きさが変わるので, 親グループが変わったときにキャッシュを無効化する
    if (viewStyle.bodyShapeInner != viewStyle.bodyShapeOuter) {
      getCallbackRegistry().getOnParentGroupChanged().add(event -> {
        if (event.oldParent() != null
            && event.newParent() != null
            && event.oldParent().inner == event.newParent().inner) {
          return;
        }
        nodeSizeCache.setDirty(true);
        nodeWithCnctrSizeCache.setDirty(true);
      });
    }
  }

  /** ノードサイズのキャッシュ値を更新する. */
  private void updateNodeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeSizeCache.update(new Vec2D(nodeSize));
    }
  }

  @Override
  protected void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
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

    var nodeSize = calcBodySize();
    updateNodeSizeCache(false, nodeSize);
    if (includeCnctr) {
      Vec2D cnctrSize = getRegionManager().getConnectorSize();
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        nodeSize = calcSizeIncludingLeftConnector(nodeSize, cnctrSize);
      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        nodeSize = calcSizeIncludingTopConnector(nodeSize, cnctrSize);
      }
      updateNodeSizeCache(true, nodeSize);
    }
    return nodeSize;
  }

  /** ノードのボディ部分のサイズを求める. */
  private Vec2D calcBodySize() {
    if (!nodeSizeCache.isDirty()) {
      return new Vec2D(nodeSizeCache.getVal());
    }
    Vec2D commonPartSize = getRegionManager().getCommonPartSize();
    return addPaddingAndNotch(commonPartSize);
  }

  /**
   * {@param size} にパディングと切り欠き部分の大きさを加えて返す.
   *
   * @param size このサイズにパディングと切り欠き部分の大きさを加える
   */
  private Vec2D addPaddingAndNotch(Vec2D size) {
    if (getLookManager().getBodyShape() == BodyShapeType.NONE) {
      return size;
    }
    double width = viewStyle.paddingLeft + size.x + viewStyle.paddingRight;
    double height = viewStyle.paddingTop + size.y + viewStyle.paddingBottom;
    Vec2D notchSize = getRegionManager().getNotchSize();
    if (viewStyle.notchPos == NotchPos.RIGHT) {
      width += notchSize.x;
      height = Math.max(height, notchSize.y);
    } else if (viewStyle.notchPos == NotchPos.BOTTOM) {
      width = Math.max(width, notchSize.x);
      height += notchSize.y;
    }
    return new Vec2D(width, height);
  }

  /**
   * 左にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingLeftConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeWidth = bodySize.x + cnctrSize.x;
    // ボディの左上を原点としたときのコネクタの上端の座標
    double cnctrTopPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrTopPos += (bodySize.y - cnctrSize.y) / 2;
    }
    // ボディの左上を原点としたときのコネクタの下端の座標
    double cnctrBottomPos = cnctrTopPos + cnctrSize.y;
    double wholeHeight = Math.max(cnctrBottomPos, bodySize.y) - Math.min(cnctrTopPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }

  /**
   * 上にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingTopConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeHeight = bodySize.y + cnctrSize.y;
    // ボディの左上を原点としたときのコネクタの左端の座標
    double cnctrLeftPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrLeftPos += (bodySize.x - cnctrSize.x) / 2;
    }
    // ボディの左上を原点としたときのコネクタの右端の座標
    double cnctrRightPos = cnctrLeftPos + cnctrSize.x;
    double wholeWidth = Math.max(cnctrRightPos, bodySize.x) - Math.min(cnctrLeftPos, 0);
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
    System.out.printf("%s<NoContentNodeView>  %s%n", indent(depth), hashCode());
  }
}
