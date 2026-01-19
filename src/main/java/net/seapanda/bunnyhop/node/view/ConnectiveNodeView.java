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
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ChildArrangement;
import net.seapanda.bunnyhop.node.view.style.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.node.view.style.NotchPos;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.SimpleCache;
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

  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含まない, 外部ノード, 含まない) */
  private final SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含む, 外部ノード, 含まない) */
  private final SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含まない, 外部ノード, 含む) */
  private final SimpleCache<Vec2D> nodeTreeSizeCache = new SimpleCache<>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含む, 外部ノード, 含む) */
  private final SimpleCache<Vec2D> nodeTreeWithCnctrSizeCache = new SimpleCache<>(new Vec2D());

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param factory サブグループ内の疑似ビューを作成するのに使用するオブジェクト
   * @throws ViewConstructionException ノードビューの初期化に失敗した
   */
  public ConnectiveNodeView(
      ConnectiveNode model,
      BhNodeViewStyle viewStyle,
      SequencedSet<Node> components,
      BhNodeViewFactory factory)
      throws ViewConstructionException {
    super(viewStyle, model, components);
    this.model = model;
    innerGroup.buildSubGroup(viewStyle.connective.inner, factory);
    outerGroup.buildSubGroup(viewStyle.connective.outer, factory);
    getLookManager().addCssClass(BhConstants.Css.CLASS_CONNECTIVE_NODE);

    // 親グループに依存してノードの大きさが変わるので, 親グループが変わったときにキャッシュを無効化する
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

  /**
   * このビューのモデルであるBhNodeを取得する.
   *
   * @return このビューのモデルであるBhNode
   */
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
    // innerGroup に追加できなかったらouterGroupに入れる
    if (!innerGroup.addNodeView(view)) {
      outerGroup.addNodeView(view);
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

  /** ノードツリーサイズのキャッシュ値を更新する. */
  private void updateNodeTreeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeTreeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeTreeSizeCache.update(new Vec2D(nodeSize));
    }
  }

  @Override
  protected void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
    nodeTreeSizeCache.setDirty(true);
    nodeTreeWithCnctrSizeCache.setDirty(true);
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
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      outerGroup.updateTreePosOnWorkspace(
          posX + bodySize.x + viewStyle.connective.outerOffset, posY);
    //外部ノードが下に繋がる
    } else {
      outerGroup.updateTreePosOnWorkspace(
          posX, posY + bodySize.y + viewStyle.connective.outerOffset);
    }
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    if (includeCnctr && !nodeWithCnctrSizeCache.isDirty()) {
      return new Vec2D(nodeWithCnctrSizeCache.getVal());
    }
    if (!includeCnctr && !nodeSizeCache.isDirty()) {
      return new Vec2D(nodeSizeCache.getVal());
    }

    Vec2D nodeSize = calcBodySize();
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
    Vec2D groupSize = innerGroup.getSize();
    Vec2D innerSize = switch (viewStyle.baseArrangement) {
      case ROW ->
          new Vec2D(
              commonPartSize.x + groupSize.x,
              Math.max(commonPartSize.y, groupSize.y));
      case COLUMN ->
          new Vec2D(
              Math.max(commonPartSize.x, groupSize.x),
              commonPartSize.y + groupSize.y);
    };
    return addPaddingAndNotch(innerSize);
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
    if (includeCnctr && !nodeTreeWithCnctrSizeCache.isDirty()) {
      return new Vec2D(nodeTreeWithCnctrSizeCache.getVal());
    }
    if (!includeCnctr && !nodeTreeSizeCache.isDirty()) {
      return new Vec2D(nodeTreeSizeCache.getVal());
    }
    Vec2D bodySize = getNodeSize(includeCnctr);
    Vec2D outerSize = calcOuterSize();
    var nodeTreeSize = new Vec2D();

    //外部ノードが右に接続される
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      nodeTreeSize.x = bodySize.x + outerSize.x;
      nodeTreeSize.y = Math.max(bodySize.y, outerSize.y);
    //外部ノードが下に接続される
    } else {
      nodeTreeSize.x = Math.max(bodySize.x, outerSize.x);
      nodeTreeSize.y = bodySize.y + outerSize.y;
    }
    updateNodeTreeSizeCache(includeCnctr, nodeTreeSize);
    return nodeTreeSize;
  }

  /** 外部ノードグループのサイズを計算する. */
  private Vec2D calcOuterSize() {
    Vec2D outerSize = outerGroup.getSize();
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      outerSize.x = Math.max(outerSize.x + viewStyle.connective.outerOffset, 0);
    } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
      outerSize.y = Math.max(outerSize.y + viewStyle.connective.outerOffset, 0);
    }
    return outerSize;
  }

  /** ノードの親からの相対位置を更新する. */
  @Override
  protected void updateChildRelativePos() {
    Vec2D innerRelPos = new Vec2D(viewStyle.paddingLeft, viewStyle.paddingTop);
    Vec2D commonPartSize = getRegionManager().getCommonPartSize();
    if (viewStyle.baseArrangement == ChildArrangement.ROW) {
      innerRelPos.x += commonPartSize.x;
    } else if (viewStyle.baseArrangement == ChildArrangement.COLUMN) {
      innerRelPos.y += commonPartSize.y;
    }
    innerGroup.setRelativePosFromParent(innerRelPos.x, innerRelPos.y);
    innerGroup.updateChildRelativePos();

    Vec2D bodySize = getRegionManager().getNodeSize(false);
    // 外部ノードが右に繋がる
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      outerGroup.setRelativePosFromParent(bodySize.x + viewStyle.connective.outerOffset, 0.0);
    // 外部ノードが下に繋がる
    } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
      outerGroup.setRelativePosFromParent(0.0, bodySize.y + viewStyle.connective.outerOffset);
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

  @Override
  public void show(int depth) {
    try {
      System.out.printf("%s<ConnectiveNodeView>  %s%n", indent(depth), hashCode());
      innerGroup.show(depth + 1);
      outerGroup.show(depth + 1);
    } catch (Exception e) {
      System.out.println("connectiveNodeView show exception " + e);
    }
  }
}
