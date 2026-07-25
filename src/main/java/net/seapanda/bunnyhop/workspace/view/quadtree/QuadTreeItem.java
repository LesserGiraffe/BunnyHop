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

package net.seapanda.bunnyhop.workspace.view.quadtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.seapanda.bunnyhop.utility.collection.ListNode;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * 四分木空間に登録されるアイテム.
 *
 * @author K.Koike
 */
public class QuadTreeItem extends ListNode<QuadTreeItem> {

  /** 現在属している 4 分木ノードのインデックス. */
  private int currentIdxInQuadTree = -1;
  /** このアイテムの矩形領域の左上座標. */
  private final Vec2D upperLeftPos;
  /** このアイテムの矩形領域の右下座標. */
  private final Vec2D lowerRightPos;
  /** この矩形に関連するオブジェクト. */
  private final Object userData;
  /** このアイテムを保持する {@link QuadTreeSpace} を取得する. */
  private QuadTreeSpace qtSpace;

  /**
   * コンストラクタ.
   *
   * @param upperLeftX 左上X座標
   * @param upperLeftY 左上Y座標
   * @param lowerRightX 右下X座標
   * @param lowerRightY 右下Y座標
   * @param userData この矩形に関連するオブジェクト
   */
  public QuadTreeItem(
      double upperLeftX, double upperLeftY,
      double lowerRightX, double lowerRightY,
      Object userData) {
    upperLeftPos = new Vec2D(upperLeftX, upperLeftY);
    lowerRightPos = new Vec2D(lowerRightX, lowerRightY);
    container = this;
    this.userData = userData;
  }

  /**
   * コンストラクタ.
   *
   * @param upperLeftX 左上X座標
   * @param upperLeftY 左上Y座標
   * @param lowerRightX 右下X座標
   * @param lowerRightY 右下Y座標
   */
  public QuadTreeItem(
      double upperLeftX, double upperLeftY,
      double lowerRightX, double lowerRightY) {
    this(upperLeftX, upperLeftY, lowerRightX, lowerRightY, null);
  }

  public QuadTreeItem() {
    this(0, 0, 0, 0, null);
  }

  /**
   * 位置を設定する.
   *
   * @param upperLeftX 左上X座標
   * @param upperLeftY 左上Y座標
   * @param lowerRightX 右下X座標
   * @param lowerRightY 右下Y座標
   */
  public void setPos(
      double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
    upperLeftPos.x = upperLeftX;
    upperLeftPos.y = upperLeftY;
    lowerRightPos.x = lowerRightX;
    lowerRightPos.y = lowerRightY;
    getQtSpace().ifPresent(space -> space.notifyItemPositionChanged(this));
  }

  /**このアイテムの矩形領域の左上の座標を返す. */
  public Vec2D getUpperLeft() {
    return new Vec2D(upperLeftPos);
  }

  /** このアイテムの矩形領域の右下の座標を返す. */
  public Vec2D getLowerRight() {
    return new Vec2D(lowerRightPos);
  }

  /** このアイテムの矩形領域の左上の X 位置を返す. */
  public double getMinX() {
    return upperLeftPos.x;
  }

  /** このアイテムの矩形領域の左上の Y 位置を返す. */
  public double getMinY() {
    return upperLeftPos.y;
  }

  /** このアイテムの矩形領域の右下の X 位置を返す. */
  public double getMaxX() {
    return lowerRightPos.x;
  }

  /** このアイテムの矩形領域の右下の Y 位置を返す. */
  public double getMaxY() {
    return lowerRightPos.y;
  }

  /** このアイテムの矩形領域の中心の座標を返す. */
  public Vec2D getCenter() {
    return new Vec2D(getCenterX(), getCenterY());
  }

  /** このアイテムの矩形領域の中心の X 座標を返す. */
  public double getCenterX() {
    return (getMaxX() - getMinX()) / 2 + getMinX();
  }

  /** このアイテムの矩形領域の中心の Y 座標を返す. */
  public double getCenterY() {
    return (getMaxY() - getMinY()) / 2 + getMinY();
  }

  /** このアイテムの矩形領域の幅を返す. */
  public double getWidth() {
    return lowerRightPos.x - upperLeftPos.x;
  }

  /** このアイテムの矩形領域の高さを返す. */
  public double getHeight() {
    return lowerRightPos.y - upperLeftPos.y;
  }

  /**
   * 現在属している 4 分木ノードのインデックスを返す.
   *
   * @return 現在属している 4 分木ノードのインデックス
   */
  public int getIdxInQuadTree() {
    return currentIdxInQuadTree;
  }

  /**
   * 4 分木ノードのインデックスをセットする.
   *
   * @param idxInQuadTree 4 分木ノードのインデックス
   */
  public void setIdxInQuadTree(int idxInQuadTree) {
    currentIdxInQuadTree = idxInQuadTree;
  }

  /**
   * 引数のオブジェクトとこのオブジェクトの重なりを判定する.
   *
   * @param item このオブジェクトとの重なりを判定するオブジェクト
   * @param option 検索オプション
   * @return 引数のオブジェクトとこのオブジェクトが重なっていた場合 true
   */
  public boolean overlapsWith(QuadTreeItem item, OverlapOption option) {
    return switch (option) {
      case CONTAIN -> contains(item);
      case INTERSECT -> intersects(item);
      default -> throw new AssertionError("Invalid search option " + option);
    };
  }

  /**
   * 引数のオブジェクトをこのオブジェクトが完全に覆っているか判定する.
   *
   * @param item 重なりを判定するオブジェクト
   * @return 引数のオブジェクトをこのオブジェクトが完全に覆っている場合 true
   */
  private boolean contains(QuadTreeItem item) {
    return upperLeftPos.x  <= item.upperLeftPos.x
        && lowerRightPos.x >= item.lowerRightPos.x
        && upperLeftPos.y  <= item.upperLeftPos.y
        && lowerRightPos.y >= item.lowerRightPos.y;
  }

  /**
   * 引数のオブジェクトとこのオブジェクトが重なりを判定する.
   *
   * @param item このオブジェクトとの重なりを判定するオブジェクト
   * @return 引数のオブジェクトとこのオブジェクト一部でも重なっていた場合 true
   */
  private boolean intersects(QuadTreeItem item) {
    return upperLeftPos.x  <= item.lowerRightPos.x
        && lowerRightPos.x >= item.upperLeftPos.x
        && upperLeftPos.y  <= item.lowerRightPos.y
        && lowerRightPos.y >= item.upperLeftPos.y;
  }

  /**
   * このアイテムを保持する {@link QuadTreeSpace} を取得する.
   * 存在しない場合は null.
   */
  public Optional<QuadTreeSpace> getQtSpace() {
    return Optional.ofNullable(qtSpace);
  }

  /**
   * このアイテムを保持する {@link QuadTreeSpace} を設定する.
   *
   * @param space このオブジェクトを保持する {@link QuadTreeSpace} オブジェクト. (nullable)
   */
  public void setQtSpace(QuadTreeSpace space) {
    this.qtSpace = space;
  }

  /**
   * このアイテムに重なる {@link QuadTreeItem} をこのアイテムが所属する四分木空間の中から見つける.
   *
   * @param option 検索オプション
   * @return このアイテムに重なる {@link QuadTreeItem} のリスト
   */
  public List<QuadTreeItem> findOverlappedItems(OverlapOption option) {
    return getQtSpace().map(space -> space.search(this, option)).orElse(new ArrayList<>());
  }

  /**
   * このアイテムに対応するオブジェクトを返す.
   *
   * @return 描画対象のオブジェクト
   */
  @SuppressWarnings("unchecked")
  public <T> T getUserData() {
    return (T) userData;
  }

  /** {@link QuadTreeItem} 同士の重なりを判定する際のオプション. */
  public enum OverlapOption {
    /**
     * {@link QuadTreeItem#findOverlappedItems} を呼び出した {@link QuadTreeItem} に
     * 完全に覆われる {@link QuadTreeItem} を探す場合.
     */
    CONTAIN,
    /**
     * {@link QuadTreeItem#findOverlappedItems} を呼び出した {@link QuadTreeItem} と
     * 一部でも重なる {@link QuadTreeItem} を探す場合.
     */
    INTERSECT,
  }
}
