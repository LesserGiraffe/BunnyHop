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
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem.OverlapOption;

/**
 * 四分木空間を使って衝突を管理するクラス.
 *
 * @author K.Koike
 */
public class QuadTreeSpace {

  /** 再帰的に分割する回数. */
  private final int numRecursive;
  /** 縦と横の分割数. */
  private int numPartitions;
  /** 分割する空間横幅. */
  private final double width;
  /** 分割する空間の縦幅. */
  private final double height;
  /** 分割された空間の横幅. */
  private final double cellWidth;
  /** 分割された空間の縦幅. */
  private final double cellHeight;
  /** 四分木. */
  private final ArrayList<QuadTreeItem> quadTree;

  /**
   * コンストラクタ.
   *
   * @param numRecursive 再帰的に分割する回数 (3 の場合縦横が 2**3 = 8 に分割され, 64個の小空間に分割される)
   * @param width 分割される空間の横幅
   * @param height 分割される空間の縦幅
   */
  public QuadTreeSpace(int numRecursive, double width, double height) {
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
      quadTree.add(new QuadTreeItem());
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
  public QuadTreeSpace(QuadTreeSpace org, int numRecursive, double width, double height) {
    this(numRecursive, width, height);
    moveQuadTreeItems(org);
  }

  /**
   * 四分木空間に {@link QuadTreeItem} を追加する.
   *
   * @param item 追加するアイテム
   */
  public void addItem(QuadTreeItem item) {
    item.remove();
    item.setQtSpace(this);
    registerItem(item);
  }

  /**
   * 四分木空間から {@link QuadTreeItem} を削除する.
   *
   * @param item 削除するアイテム
   */
  public static void removeItem(QuadTreeItem item) {
    item.remove();
    item.setIdxInQuadTree(-1);
    item.setQtSpace(null);
  }

  /** {@code old} の持つ {@link QuadTreeItem} をこの四分木空間に移し替える. */
  private void moveQuadTreeItems(QuadTreeSpace old) {
    for (QuadTreeItem headQuadTreeObj : old.quadTree) {
      QuadTreeItem movedQuadTreeObj;
      while ((movedQuadTreeObj = headQuadTreeObj.getNext()) != null) {
        addItem(movedQuadTreeObj);
      }
    }
  }

  /**
   * 四分木空間の大きさを取得する.
   *
   * @return 四分木空間の大きさ
   */
  public Vec2D getSize() {
    return new Vec2D(width, height);
  }

  /** {@code item} の位置が変わった事をこのオブジェクトに通知する. */
  public void notifyItemPositionChanged(QuadTreeItem item) {
    if (item.getQtSpace().orElse(null) != this) {
      return;
    }
    registerItem(item);
  }

  /**
   * 四分木空間に {@link QuadTreeItem} を登録する.
   *
   * @param item 四分木に登録されるオブジェクト
   */
  private void registerItem(QuadTreeItem item) {
    Vec2D upperLeft = item.getUpperLeft();
    Vec2D lowerRight = item.getLowerRight();
    int upperLeftMortonNum = getMortonNumber(upperLeft);
    int lowerRightMortonNum = getMortonNumber(lowerRight);
    int quadTreeIndex = getQuadTreeIndex(upperLeftMortonNum, lowerRightMortonNum);
    item.remove();
    item.setIdxInQuadTree(quadTreeIndex);
    quadTree.get(quadTreeIndex).connectToNext(item);  // 所属空間変更
  }

  private int getQuadTreeIndex(int upperLeftMortonNum, int lowerRightMortonNum) {
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
    return (tmp - 1) / 3 + spaceMortonNum;
  }

  /**
   * 点の位置からモートン番号を求める.
   *
   * @param pos モートン番号を求める位置
   * @return モートン番号
   */
  private int getMortonNumber(Vec2D pos) {
    int adjustedX = (int) Math.clamp(pos.x, 0, width - 1);
    int adjustedY = (int) Math.clamp(pos.y, 0, height - 1);
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
   * {@code item} に重なる {@link QuadTreeItem} を四分木空間の中から見つける.
   *
   * @param item このオブジェクトに重なっている {@link QuadTreeItem} オブジェクトを見つける
   * @param option 検索オプション
   * @return 引数で指定した {@link QuadTreeItem} に重なる {@link QuadTreeItem} のリスト
   */
  public ArrayList<QuadTreeItem> search(QuadTreeItem item, OverlapOption option) {
    if (item.getQtSpace().orElse(null) != this) {
      return new ArrayList<>();
    }
    //子空間から探す
    int idxInQuadTree = item.getIdxInQuadTree();
    ArrayList<QuadTreeItem> overlappedList = new ArrayList<>();
    searchSubSpaces(idxInQuadTree, item, option, overlappedList);

    //親空間から探す
    int nextSearchIdx = idxInQuadTree - 1;
    while (nextSearchIdx >= 0) {
      nextSearchIdx /= 4;
      searchSpace(nextSearchIdx, item, option, overlappedList);
      nextSearchIdx -= 1;
    }
    overlappedList.remove(item);

    sortItems(item, overlappedList);
    return overlappedList;
  }

  /**
   * {@code idx} の四分木空間とその下位空間から {@code item} に重なる {@link QuadTreeItem} オブジェクトを見つける.
   *
   * @param idx この四分木空間以下の空間から @code item} に重なっている {@link QuadTreeItem} オブジェクトを見つける
   * @param item このオブジェクトに重なっている {@link QuadTreeItem} を見つける
   * @param option 検索オプション
   * @param overlappedList 重なっている {@link QuadTreeItem} オブジェクトを格納するリスト
   * */
  private void searchSubSpaces(
      int idx, QuadTreeItem item, OverlapOption option, List<QuadTreeItem> overlappedList) {
    searchSpace(idx, item, option, overlappedList);
    int childIdx = idx * 4 + 1;
    if (childIdx <= quadTree.size() - 1) {
      searchSubSpaces(childIdx,     item, option, overlappedList);
      searchSubSpaces(childIdx + 1, item, option, overlappedList);
      searchSubSpaces(childIdx + 2, item, option, overlappedList);
      searchSubSpaces(childIdx + 3, item, option, overlappedList);
    }
  }

  /**
   * {@code idx} で指定したサブ空間から {@code item} に重なる {@link QuadTreeItem} オブジェクトを見つける.
   *
   * @param idx この四分木空間から rectangleに重なっている {@link QuadTreeItem} オブジェクトを見つける
   * @param item このオブジェクトに重なっている {@link QuadTreeItem} オブジェクトを見つける
   * @param option 検索オプション
   * @param overlappedList 重なっている {@link QuadTreeItem} オブジェクトを格納するリスト
   * */
  private void searchSpace(
      int idx, QuadTreeItem item, OverlapOption option, List<QuadTreeItem> overlappedList) {
    QuadTreeItem head = quadTree.get(idx);
    QuadTreeItem next = head.getNext();
    while (next != null) {
      if (item.overlapsWith(next, option)) {
        overlappedList.add(next);
      }
      next = next.getNext();
    }
  }

  /** {@code overlappedItems} の要素を {@code item} に近い順に並べ替える. */
  private void sortItems(QuadTreeItem item, ArrayList<QuadTreeItem> overlappedItems) {
    double centerX = item.getCenterX();
    double centerY = item.getCenterY();
    overlappedItems.sort((itemA, itemB) -> compare(itemA, itemB, centerX, centerY));
  }

  private int compare(QuadTreeItem itemA, QuadTreeItem itemB, double centerX, double centerY) {
    double distanceAx = centerX - itemA.getCenterX();
    double distanceAy = centerY - itemA.getCenterY();
    double distanceA = distanceAx * distanceAx + distanceAy * distanceAy;

    double distanceBx = centerX - itemB.getCenterX();
    double distanceBy = centerY - itemB.getCenterY();
    double distanceB = distanceBx * distanceBx + distanceBy * distanceBy;

    if (distanceA < distanceB) {
      return -1;
    } else if (distanceA > distanceB) {
      return 1;
    }
    return 0;
  }

  /**
   * 登録されているBhNodeの数を計算する (デバッグ用).
   *
   * @return 登録されているBhNodeの数
   */
  public int calcRegisteredNodeNum() {
    int numOfNode = 0;
    for (QuadTreeItem head : quadTree) {
      QuadTreeItem rect = head;
      while ((rect = rect.getNext()) != null) {
        ++numOfNode;
      }
    }
    return numOfNode;
  }

  /**
   * 四分木空間の縦と横の分割数を返す.
   *
   * @return 四分木空間の縦と横の分割数
   */
  public int getNumPartitions() {
    return numPartitions;
  }
}
