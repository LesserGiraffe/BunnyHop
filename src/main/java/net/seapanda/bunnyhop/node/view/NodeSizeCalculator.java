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

import java.util.function.Supplier;
import net.seapanda.bunnyhop.node.view.BhNodeView.ParentGroupChangedEvent;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.style.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.NotchPos;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ノードビューのサイズを計算する機能を提供するクラス.
 *
 * @author K.Koike
 */
class NodeSizeCalculator {

  /** ノード全体 (ボディ + コネクタ) のサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> wholeSizeCache = new SimpleCache<>(new Vec2D());
  /** ノードのボディ部分のサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> bodySizeCache = new SimpleCache<>(new Vec2D());
  /** コネクタ部分を含むノードツリーのサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> wholeTreeSizeCache = new SimpleCache<>(new Vec2D());
  /** コネクタ部分を含まないノードツリーのサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> treeSizeCache = new SimpleCache<>(new Vec2D());

  /** 管理対象のノードビュー. */
  private final BhNodeViewBase view;
  private final Supplier<Vec2D> fnGetInnerComponentSize;
  private final Supplier<Vec2D> fnGetOuterGroupSize;

  /**
   * コンストラクタ.
   *
   * @param view 管理対象となるノードビュー
   * @param fnGetInnerComponentSize ノード内部に描画するコンポーネント (テキストフィールド等) の
   *                                サイズを取得するメソッドの関数オブジェクト
   */
  NodeSizeCalculator(BhNodeViewBase view, Supplier<Vec2D> fnGetInnerComponentSize) {
    this(view, fnGetInnerComponentSize, () -> new Vec2D(0, 0));
  }

  /**
   * コンストラクタ.
   *
   * @param view 管理対象となるノードビュー
   * @param fnGetInnerComponentSize ノード内部に描画するコンポーネント (テキストフィールド等) の
   *                                サイズを取得するメソッドの関数オブジェクト
   * @param fnGetOuterGroupSize ノード外部に描画するグループのサイズを取得するメソッドの関数オブジェクト
   */
  NodeSizeCalculator(
      BhNodeViewBase view,
      Supplier<Vec2D> fnGetInnerComponentSize,
      Supplier<Vec2D> fnGetOuterGroupSize) {
    this.view = view;
    this.fnGetInnerComponentSize = fnGetInnerComponentSize;
    this.fnGetOuterGroupSize = fnGetOuterGroupSize;
    view.getCallbackRegistry().getOnParentGroupChanged().add(this::onParentGroupChanged);
  }

  /**
   * このオブジェクトが管理するノードビューの大きさを求める.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合 true
   * @return このオブジェクトが管理するノードビューの大きさ
   */
  Vec2D calcNodeSize(boolean includeCnctr) {
    if (includeCnctr) {
      return calcWholeSize();
    }
    return calcBodySize();
  }

  /**
   * このオブジェクトが管理するノードビューに末尾までの全外部ノードビューを加えた部分の大きさを返す.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
   * @return このオブジェクトが管理するノードビューに末尾までの全外部ノードビューを加えた部分の大きさ
   */
  Vec2D calcNodeTreeSize(boolean includeCnctr) {
    if (includeCnctr) {
      return calcNodeTreeSize(calcWholeSize(), wholeTreeSizeCache);
    }
    return calcNodeTreeSize(calcBodySize(), treeSizeCache);
  }

  private Vec2D calcNodeTreeSize(Vec2D nodeSize, SimpleCache<Vec2D> cache) {
    if (!cache.isDirty()) {
      return new Vec2D(cache.getVal());
    }
    Vec2D outerSize = fnGetOuterGroupSize.get();
    var nodeTreeSize = switch (view.style.connectorPos) {
      // 外部ノードが右に接続される
      case LEFT -> new Vec2D(nodeSize.x + outerSize.x, Math.max(nodeSize.y, outerSize.y));
      // 外部ノードが下に接続される
      case TOP -> new Vec2D(Math.max(nodeSize.x, outerSize.x), nodeSize.y + outerSize.y);
    };
    cache.update(nodeTreeSize);
    return nodeTreeSize;
  }

  /** このオブジェクトに管理するノードビューのサイズが変わったことを通知する. */
  void notifyNodeSizeChanged() {
    setCachesDirty();
  }

  /** ノードのサイズを保持するキャッシュを Dirty にする. */
  private void onParentGroupChanged(ParentGroupChangedEvent event) {
    if (event.oldParent() != null
        && event.newParent() != null
        && event.oldParent().inner == event.newParent().inner) {
      return;
    }
    setCachesDirty();
  }

  private void setCachesDirty() {
    wholeSizeCache.setDirty(true);
    bodySizeCache.setDirty(true);
    treeSizeCache.setDirty(true);
    wholeTreeSizeCache.setDirty(true);
  }

  /** このオブジェクトが管理するノードビューの全体 (ボディ + コネクタ) の大きさを求める. */
  private Vec2D calcWholeSize() {
    if (!wholeSizeCache.isDirty()) {
      return new Vec2D(wholeSizeCache.getVal());
    }
    Vec2D nodeSize = calcBodySize();
    Vec2D cnctrSize = view.getRegionManager().getConnectorSize();
    nodeSize = switch (view.style.connectorPos) {
      case LEFT -> calcSizeIncludingLeftConnector(nodeSize, cnctrSize);
      case TOP -> calcSizeIncludingTopConnector(nodeSize, cnctrSize);
    };
    wholeSizeCache.update(new Vec2D(nodeSize));
    return nodeSize;
  }


  /** このオブジェクトが管理するノードビューのボディの大きさを求める. */
  private Vec2D calcBodySize() {
    if (!bodySizeCache.isDirty()) {
      return new Vec2D(bodySizeCache.getVal());
    }
    Vec2D commonPartSize = view.getRegionManager().getCommonPartSize();
    Vec2D componentSize = fnGetInnerComponentSize.get();
    Vec2D innerSize = switch (view.style.baseArrangement) {
      case ROW -> new Vec2D(
          commonPartSize.x + componentSize.x,
          Math.max(commonPartSize.y, componentSize.y));
      case COLUMN -> new Vec2D(
          Math.max(commonPartSize.x, componentSize.x),
          commonPartSize.y + componentSize.y);
    };
    if (view.getLookManager().getBodyShape() != BodyShapeType.NONE) {
      innerSize = addPaddingAndNotch(innerSize);
    }
    bodySizeCache.update(innerSize);
    return innerSize;
  }

  /**
   * {@param size} にパディングと切り欠き部分の大きさを加えて返す.
   *
   * @param size このサイズにパディングと切り欠き部分の大きさを加える
   */
  private Vec2D addPaddingAndNotch(Vec2D size) {
    double width = view.style.paddingLeft + size.x + view.style.paddingRight;
    double height = view.style.paddingTop + size.y + view.style.paddingBottom;
    Vec2D notchSize = view.getRegionManager().getNotchSize();
    if (view.style.notchPos == NotchPos.RIGHT) {
      width += notchSize.x;
      height = Math.max(height, notchSize.y);
    } else if (view.style.notchPos == NotchPos.BOTTOM) {
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
    double cnctrTopPos = view.style.connectorShift;
    if (view.style.connectorAlignment == ConnectorAlignment.CENTER) {
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
   * @return 上にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingTopConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeHeight = bodySize.y + cnctrSize.y;
    // ボディの左上を原点としたときのコネクタの左端の座標
    double cnctrLeftPos = view.style.connectorShift;
    if (view.style.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrLeftPos += (bodySize.x - cnctrSize.x) / 2;
    }
    // ボディの左上を原点としたときのコネクタの右端の座標
    double cnctrRightPos = cnctrLeftPos + cnctrSize.x;
    double wholeWidth = Math.max(cnctrRightPos, bodySize.x) - Math.min(cnctrLeftPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }
}
