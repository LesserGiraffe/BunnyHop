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

package net.seapanda.bunnyhop.quadtree;

import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;

/**
 * 4文木空間を持ちいた衝突を管理するクラス.
 *
 * @author K.Koike
 * */
public class QuadTreeManager {

  /** 再帰的に分割する回数. */
  private int numRecursive;
  /** 縦と横の分割数. */
  private int numPartitions;
  /** 分割する空間横幅. */
  private double width;
  /** 分割する空間の縦幅. */
  private double height;
  /** 分割された空間の横幅. */
  private double cellWidth;
  /** 分割された空間の縦幅. */
  private double cellHeight;
  /** 4 分木. */
  private ArrayList<QuadTreeRectangle> quadTree;
  /** 4 分木空間上での位置が決まっていない 4 分木オブジェクトのリストの先頭. */
  private QuadTreeRectangle unknownSpaceListHead = new QuadTreeRectangle();

  /**
   * コンストラクタ.
   *
   * @param numRecursive 再帰的に分割する回数 (3 の場合縦横が 2**3 = 8 に分割され, 64個の小空間に分割される)
   * @param width 分割される空間の横幅
   * @param height 分割される空間の縦幅
   */
  public QuadTreeManager(int numRecursive, double width, double height) {
    this.numRecursive = numRecursive;
    this.width = width;
    this.height = height;
    numPartitions = 1;
    for (int i = 0; i < numRecursive; ++i) {
      numPartitions *= 2;
    }
    cellWidth = width / numPartitions;
    cellHeight = height / numPartitions;

    int tmp = 1;
    for (int i = 0; i < numRecursive + 1; ++i) {
      tmp *= 4;
    }
    int numQuadTreeNode = (tmp - 1) / 3;
    quadTree = new ArrayList<>(numQuadTreeNode);
    for (int i = 0; i < numQuadTreeNode; ++i) {
      quadTree.add(new QuadTreeRectangle());
    }
  }

  /**
   * コンストラクタ.
   *
   * @param org コピー元オブジェクト
   * @param numRecursive 再帰的に分割する回数 (3 の場合縦横が 2**3 = 8 に分割され, 64個の小空間に分割される)
   * @param width 分割される空間の横幅
   * @param height 分割される空間の縦幅
   * */
  public QuadTreeManager(QuadTreeManager org, int numRecursive, double width, double height) {
    this(numRecursive, width, height);
    moveQuadTreeObj(org);
  }

  /**
   * 矩形オブジェクトを登録する.
   *
   * @param quadTreeObj 登録する矩形オブジェクト
   */
  public void addQuadTreeObj(QuadTreeRectangle quadTreeObj) {
    quadTreeObj.remove();
    quadTreeObj.setIdxInQuadTree(-1);  //無効な4 分木ノードインデックスを登録しておく
    unknownSpaceListHead.connectToNext(quadTreeObj);
    quadTreeObj.setCallBackFuncs(this::registerWithQuadTree, this::searchOverlappedRects);
  }

  /**
   * 矩形オブジェクトを削除する.
   *
   * @param quadTreeObj 削除する矩形オブジェクト
   */
  public static void removeQuadTreeObj(QuadTreeRectangle quadTreeObj) {
    quadTreeObj.remove();
    quadTreeObj.setIdxInQuadTree(-1);
    quadTreeObj.setCallBackFuncs(null, null);
  }

  /**
   * 古い QuadTreeManager から この QuadTreeManager に 4 分木登録オブジェクトを移し替える.
   *
   * @param old 古い QuadTreeManager
   */
  private void moveQuadTreeObj(QuadTreeManager old) {
    //4 分木からの移動
    for (QuadTreeRectangle headQuadTreeObj : old.quadTree) {
      QuadTreeRectangle movedQuadTreeObj;
      while ((movedQuadTreeObj = headQuadTreeObj.getNext()) != null) {
        addQuadTreeObj(movedQuadTreeObj);
      }
    }
    //位置不明リストからの移動
    QuadTreeRectangle movedQuadTreeObj;
    while ((movedQuadTreeObj = old.unknownSpaceListHead.getNext()) != null) {
      addQuadTreeObj(movedQuadTreeObj);
    }
  }

  /**
   * 4 分木空間の大きさを取得する.
   *
   * @return 4 分木空間の大きさ
   */
  public Vec2D getQtSpaceSize() {
    return new Vec2D(width, height);
  }

  /**
   * 4 分木に4 分木オブジェクトを登録する.
   *
   * @param quadTreeObj 4 分木に登録されるオブジェクト
   */
  private void registerWithQuadTree(QuadTreeRectangle quadTreeObj) {
    Vec2D upperLeftPos = quadTreeObj.getUpperLeftPos();
    Vec2D lowerRightPos = quadTreeObj.getLowerRightPos();

    int upperLeftMortonNum = getMortonNumber(upperLeftPos);
    int lowerRightMortonNum = getMortonNumber(lowerRightPos);
    int xorMorton = (upperLeftMortonNum ^ lowerRightMortonNum) << 2 | 0x00000003;
    int spaceLevel = 0; // 分割空間レベル (0:ルート, 1:親, 2:子, 3:孫, ...)
    int levelDecision = 0x3 << numRecursive * 2;
    while ((xorMorton & levelDecision) == 0) {
      xorMorton <<= 2;
      ++spaceLevel;
    }
    int spaceMortonNum = lowerRightMortonNum >>> ((numRecursive - spaceLevel) * 2);
    int tmp = 1;
    for (int i = 0; i < spaceLevel; ++i) {
      tmp *= 4;
    }
    int quadTreeIndex = (tmp - 1) / 3 + spaceMortonNum;
    quadTreeObj.remove();
    quadTreeObj.setIdxInQuadTree(quadTreeIndex);
    quadTree.get(quadTreeIndex).connectToNext(quadTreeObj);  // 所属空間変更
  }

  /**
   * 点の位置からモートン番号を求める.
   *
   * @param pos モートン番号を求める位置
   * @rteurn モートン番号
   */
  private int getMortonNumber(Vec2D pos) {
    int adjustedX = (int) Math.min(width - 1,  Math.max(0, pos.x));
    int adjustedY = (int) Math.min(height - 1, Math.max(0, pos.y));
    int addressX = (int) (adjustedX / cellWidth);
    int addressY = (int) (adjustedY / cellHeight);
    return separateBits(addressX) | (separateBits(addressY) << 1);
  }

  private int separateBits(int address) {
    address = (address | (address << 8)) & 0x00ff00ff;
    address = (address | (address << 4)) & 0x0f0f0f0f;
    address = (address | (address << 2)) & 0x33333333;
    return (address | (address << 1)) & 0x55555555;
  }

  /**
   * {@code idx} の 4 分木空間とその下位空間から rectangle に重なる {@link QuadTreeRectangle} オブジェクトを見つける.
   *
   * @param idx この 4 分木空間以下の空間から rectangleに重なっている {@link QuadTreeRectangle} オブジェクトを見つける
   * @param rectangle このオブジェクトに重なっているQuadTreeRectangleオブジェクトを見つける
   * @param option 検索オプション
   * @param overlappedList 重なっているQuadTreeRectangleオブジェクトを格納するリスト
   * */
  private void searchOverlappedRects(
      int idx,
      QuadTreeRectangle rectangle,
      OverlapOption option,
      List<QuadTreeRectangle> overlappedList) {

    searchOverlappedRectsForOneSpace(idx, rectangle, option, overlappedList);
    int childIdx = idx * 4 + 1;
    if (childIdx <= quadTree.size() - 1) {
      searchOverlappedRects(childIdx,     rectangle, option, overlappedList);
      searchOverlappedRects(childIdx + 1, rectangle, option, overlappedList);
      searchOverlappedRects(childIdx + 2, rectangle, option, overlappedList);
      searchOverlappedRects(childIdx + 3, rectangle, option, overlappedList);
    }
  }

  /**
   * {@code rectangle} に重なる {@link QuadTreeRectangle} オブジェクトを4 分木空間の中から見つける.
   *
   * @param rectangle このオブジェクトに重なっている {@link QuadTreeRectangle} オブジェクトを見つける
   * @param option 検索オプション
   * @return 引数で指定したQuadTreeRectangleオブジェクトに重なるQuadTreeRectangleオブジェクトのリスト
   */
  public ArrayList<QuadTreeRectangle> searchOverlappedRects(
      QuadTreeRectangle rectangle, OverlapOption option) {
    int idxInQuadTree = rectangle.getIdxInQuadTree();
    ArrayList<QuadTreeRectangle> overlappedList = new ArrayList<>();
    searchOverlappedRects(idxInQuadTree, rectangle, option, overlappedList);  //子空間から探す

    //親空間から探す
    int nextSearchIdx = idxInQuadTree - 1;
    while (nextSearchIdx >= 0) {
      nextSearchIdx /= 4;
      searchOverlappedRectsForOneSpace(nextSearchIdx, rectangle, option, overlappedList);
      nextSearchIdx -= 1;
    }
    overlappedList.remove(rectangle);

    //引数の rectangle に対して近い順にソートする
    double centerX = (rectangle.getLowerRightPos().x - rectangle.getUpperLeftPos().x) / 2 
        + rectangle.getUpperLeftPos().x;
    double centerY = (rectangle.getLowerRightPos().y - rectangle.getUpperLeftPos().y) / 2
        + rectangle.getUpperLeftPos().y;
    overlappedList.sort((rectA, rectB) -> compare(rectA, rectB, centerX, centerY));
    return overlappedList;
  }

  private int compare(
      QuadTreeRectangle rectA, QuadTreeRectangle rectB, double centerX, double centerY) {
    double posAx = (rectA.getLowerRightPos().x - rectA.getUpperLeftPos().x) / 2
        + rectA.getUpperLeftPos().x;
    double posAy = (rectA.getLowerRightPos().y - rectA.getUpperLeftPos().y) / 2
        + rectA.getUpperLeftPos().y;
    double distanceAx = centerX - posAx;
    double distanceAy = centerY - posAy;
    double distanceA = distanceAx * distanceAx + distanceAy * distanceAy;
    double posBx = (rectB.getLowerRightPos().x - rectB.getUpperLeftPos().x) / 2 
        + rectB.getUpperLeftPos().x;
    double posBy = (rectB.getLowerRightPos().y - rectB.getUpperLeftPos().y) / 2
        + rectB.getUpperLeftPos().y;
    double distanceBx = centerX - posBx;
    double distanceBy = centerY - posBy;
    double distanceB = distanceBx * distanceBx + distanceBy * distanceBy;

    if (distanceA < distanceB) {
      return -1;
    } else if (distanceA > distanceB) {
      return 1;
    }
    return 0;
  }

  /**
   * {@code idx} の 4 分木空間から {@code rectangle} に重なる {@link QuadTreeRectangle} オブジェクトを見つける.
   *
   * @param idx この 4 分木空間から rectangleに重なっている {@link QuadTreeRectangle} オブジェクトを見つける
   * @param rectangle このオブジェクトに重なっている {@link QuadTreeRectangle} オブジェクトを見つける
   * @param option 検索オプション
   * @param overlappedList 重なっているQuadTreeRectangleオブジェクトを格納するリスト
   * */
  private void searchOverlappedRectsForOneSpace(
      int idx,
      QuadTreeRectangle rectangle,
      OverlapOption option,
      List<QuadTreeRectangle> overlappedList) {

    QuadTreeRectangle head = quadTree.get(idx);
    QuadTreeRectangle next = head.getNext();
    while (next != null) {
      if (rectangle.overlapsWith(next, option)) {
        overlappedList.add(next);
      }
      next = next.getNext();
    }
  }

  /**
   * 登録されているBhNodeの数を計算する (デバッグ用).
   *
   * @return 登録されているBhNodeの数
   */
  public int calcRegisteredNodeNum() {
    int numOfNode = 0;
    for (QuadTreeRectangle head : quadTree) {
      QuadTreeRectangle rect = head;
      while ((rect = rect.getNext()) != null) {
        ++numOfNode;
      }
    }

    QuadTreeRectangle rect = unknownSpaceListHead;
    while ((rect = rect.getNext()) != null) {
      ++numOfNode;
    }
    return numOfNode;
  }

  /**
   * 4 分木空間の縦と横の分割数を返す.
   *
   * @return 4 分木空間の縦と横の分割数
   */
  public int getNumPartitions() {
    return numPartitions;
  }
}
