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
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;

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
  private ConnectiveNode model;

  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含まない, 外部ノード, 含まない) */
  private SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含む, 外部ノード, 含まない) */
  private SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含まない, 外部ノード, 含む) */
  private SimpleCache<Vec2D> nodeTreeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** ノードのサイズのキャッシュデータ. (コネクタ部分: 含む, 外部ノード, 含む) */
  private SimpleCache<Vec2D> nodeTreeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());

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

  /** このグループに子要素のサイズが変わったことを伝える. */
  void notifyChildSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
    nodeTreeSizeCache.setDirty(true);
    nodeTreeWithCnctrSizeCache.setDirty(true);
    BhNodeViewGroup group = getTreeManager().getParentGroup();
    if (group != null) {
      group.notifyChildSizeChanged();
    }
    if (getTreeManager().isRootView()) {
      getLookManager().requestArrangement();
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
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);

    //内部ノード絶対位置更新
    Vec2D relativePos = innerGroup.getRelativePosFromParent();
    innerGroup.updateTreePosOnWorkspace(posX + relativePos.x, posY + relativePos.y);

    //外部ノード絶対位置更新
    Vec2D bodySize = getRegionManager().getNodeSize(false);
    //外部ノードが右に繋がる
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      outerGroup.updateTreePosOnWorkspace(posX + bodySize.x, posY);
    //外部ノードが下に繋がる
    } else {
      outerGroup.updateTreePosOnWorkspace(posX, posY + bodySize.y);
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
    Vec2D innerSize = innerGroup.getSize();
    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    double bodyWidth = viewStyle.paddingLeft + innerSize.x + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + innerSize.y + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    var nodeSize = new Vec2D(bodyWidth, bodyHeight);
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
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
    Vec2D outerSize = outerGroup.getSize();
    double totalWidth = bodySize.x;
    double totalHeight = bodySize.y;

    //外部ノードが右に接続される
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      totalWidth += outerSize.x;
      totalHeight = Math.max(totalHeight, outerSize.y);
    //外部ノードが下に接続される
    } else {    
      totalWidth = Math.max(totalWidth, outerSize.x);
      totalHeight += outerSize.y;
    }

    var nodeTreeSize = new Vec2D(totalWidth, totalHeight);
    updateNodeTreeSizeCache(includeCnctr, nodeTreeSize);
    return nodeTreeSize;
  }

  /** ノードの親からの相対位置を更新する. */
  @Override
  protected void updateChildRelativePos() {
    innerGroup.setRelativePosFromParent(viewStyle.paddingLeft, viewStyle.paddingTop);
    innerGroup.updateChildRelativePos();
    Vec2D bodySize = getRegionManager().getNodeSize(false);
    // 外部ノードが右に繋がる
    if (viewStyle.connectorPos == ConnectorPos.LEFT) {
      outerGroup.setRelativePosFromParent(bodySize.x, 0.0);
    // 外部ノードが下に繋がる
    } else {                       
      outerGroup.setRelativePosFromParent(0.0, bodySize.y);
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
      System.out.println(
          "%s<ConnectiveNodeView>  %s".formatted(indent(depth), hashCode()));
      innerGroup.show(depth + 1);
      outerGroup.show(depth + 1);
    } catch (Exception e) {
      System.out.println("connectiveNodeView show exception " + e);
    }
  }
}
