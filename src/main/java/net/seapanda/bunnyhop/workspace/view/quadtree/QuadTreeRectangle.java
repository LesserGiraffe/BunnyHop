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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.utility.LinkNode;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * 4 分木空間に登録される矩形オブジェクト.
 *
 * @author K.Koike
 */
public class QuadTreeRectangle extends LinkNode<QuadTreeRectangle> {

  /** 現在属している 4 分木ノードのインデックス. */
  private int currentIdxInQuadTree = -1;
  /** 矩形の左上座標. */
  private Vec2D upperLeftPos;
  /** 矩形の右下座標. */
  private Vec2D lowerRightPos;
  /** 位置が更新されたときのイベントハンドラを. */
  Consumer<? super QuadTreeRectangle> fnUpdatePos;
  /** このオブジェクトの矩形領域に重なっている {@link QuadTreeRectangle} を探すときのイベントハンドラ. */
  BiFunction<? super QuadTreeRectangle, ? super OverlapOption, ? extends List<QuadTreeRectangle>>
      fnSearchOverlapped;
  /** この矩形に関連するオブジェクト. */
  private final Object userData;
  /** この矩形オブジェクトを保持する {@link QuadTreeManager} を取得する. */
  private QuadTreeManager qtManager;

  /**
   * コンストラクタ.
   *
   * @param upperLeftX 左上X座標
   * @param upperLeftY 左上Y座標
   * @param lowerRightX 右下X座標
   * @param lowerRightY 右下Y座標
   * @param userData この矩形に関連するオブジェクト
   */
  public QuadTreeRectangle(
      double upperLeftX, double upperLeftY,
      double lowerRightX, double lowerRightY,
      Object userData) {

    upperLeftPos = new Vec2D(upperLeftX, upperLeftY);
    lowerRightPos = new Vec2D(lowerRightX, lowerRightY);
    container = this;
    this.userData = userData;
  }

  public QuadTreeRectangle() {
    container = this;
    userData = null;
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
    updatePos();
  }

  /** 現在の位置で位置更新する. */
  public void updatePos() {
    if (fnUpdatePos != null) {
      fnUpdatePos.accept(this);
    }
  }

  /**
   * 矩形の左上座標を返す.
   *
   * @return 矩形の左上座標
   */
  public Vec2D getUpperLeftPos() {
    return upperLeftPos;
  }

  /**
   * 矩形の右下座標を返す.
   *
   * @return 矩形の右下座標
   */
  public Vec2D getLowerRightPos() {
    return lowerRightPos;
  }

  /**
   * コールバック関数を登録する.
   *
   * @param fnUpdatePos 位置更新時に呼び出すメソッド
   * @param fnSearchOverlapped このオブジェクトの矩形領域に重なっている {@link QuadTreeRectangle} を探すときのイベントハンドラを
   */
  public void setCallbacks(
      Consumer<? super QuadTreeRectangle> fnUpdatePos,
      BiFunction<
          ? super QuadTreeRectangle,
          ? super OverlapOption,
          ? extends List<QuadTreeRectangle>> fnSearchOverlapped) {

    this.fnUpdatePos = fnUpdatePos;
    this.fnSearchOverlapped = fnSearchOverlapped;
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
   * 引数のオブジェクトとこのオブジェクトが重なりを判定する.
   *
   * @param rectangle このオブジェクトとの重なりを判定するオブジェクト
   * @param option 検索オプション
   * @return 引数のオブジェクトとこのオブジェクトが重なっていた場合 true
   */
  public boolean overlapsWith(QuadTreeRectangle rectangle, OverlapOption option) {
    switch (option) {
      case CONTAIN:
        return contains(rectangle);

      case INTERSECT:
        return intersects(rectangle);

      default:
        throw new AssertionError("Invalid search option " + option);
    }
  }

  /**
   * 引数のオブジェクトをこのオブジェクトが完全に覆っているか判定する.
   *
   * @param retangle 重なりを判定するオブジェクト
   * @return 引数のオブジェクトをこのオブジェクトが完全に覆っている場合 true
   */
  private boolean contains(QuadTreeRectangle rectangle) {
    return upperLeftPos.x  <= rectangle.upperLeftPos.x
        && lowerRightPos.x >= rectangle.lowerRightPos.x
        && upperLeftPos.y  <= rectangle.upperLeftPos.y
        && lowerRightPos.y >= rectangle.lowerRightPos.y;
  }

  /**
   * 引数のオブジェクトとこのオブジェクトが重なりを判定する.
   *
   * @param rectangle このオブジェクトとの重なりを判定するオブジェクト
   * @return 引数のオブジェクトとこのオブジェクト一部でも重なっていた場合 true
   */
  private boolean intersects(QuadTreeRectangle rectangle) {
    return upperLeftPos.x  <= rectangle.lowerRightPos.x
        && lowerRightPos.x >= rectangle.upperLeftPos.x
        && upperLeftPos.y  <= rectangle.lowerRightPos.y
        && lowerRightPos.y >= rectangle.upperLeftPos.y;
  }

  /**
   * この矩形に重なっている {@link QuadTreeRectangle} オブジェクトを 4 分木空間から探す.
   *
   * @param option 検索オプション
   */
  public List<QuadTreeRectangle> searchOverlappedRects(OverlapOption option) {
    return fnSearchOverlapped.apply(this, option);
  }

  /**
   * この矩形オブジェクトを保持する {@link QuadTreeManager} を取得する.
   * 存在しない場合は null.
   */
  public QuadTreeManager getCurrenManager() {
    return qtManager;
  }

  /**
   * この矩形オブジェクトを保持する {@link QuadTreeManager} を設定する.
   *
   * @param qtManager この矩形オブジェクトを保持する {@link QuadTreeManager} オブジェクト. (nullable)
   */
  public void setCurrenManager(QuadTreeManager qtManager) {
    this.qtManager = qtManager;
  }

  /**
   * この矩形に対応する描画対象のオブジェクトを返す.
   *
   * @return 描画対象のオブジェクト
   */
  @SuppressWarnings("unchecked")
  public <T> T getUserData() {
    return (T) userData;
  }

  /** 矩形同士の重なりを判定する際のオプション. */
  public enum OverlapOption {
    /** サーチ関数を呼び出した矩形に完全に覆われる矩形を探す場合. */
    CONTAIN,
    /** サーチ関数を呼び出した矩形と一部でも重なる矩形を探す場合. */
    INTERSECT,
  }
}
