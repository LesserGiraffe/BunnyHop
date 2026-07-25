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

import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeSpace;

/**
 * 四分木空間へのノードビューの追加と削除を提供するクラス.
 *
 * @author K.Koike
 */
class QuadTreeSpaceRegistrationImpl implements BhNodeView.QuadTreeSpaceRegistration {

  /** ボディ部分のの範囲を保持するオブジェクト. */
  private final QuadTreeItem bodyItem;
  /** コネクタ部分のの範囲を保持するオブジェクト. */
  private final QuadTreeItem cnctrItem;

  /** コンストラクタ. */
  QuadTreeSpaceRegistrationImpl(QuadTreeItem bodyItem, QuadTreeItem cnctrItem) {
    this.bodyItem = bodyItem;
    this.cnctrItem = cnctrItem;
  }

  @Override
  public void addToQtSpace(QuadTreeSpace bodySpace, QuadTreeSpace cnctrSpace) {
    bodySpace.addItem(bodyItem);
    cnctrSpace.addItem(cnctrItem);
  }

  @Override
  public void removeFromQtSpace() {
    QuadTreeSpace.removeItem(bodyItem);
    QuadTreeSpace.removeItem(cnctrItem);
  }

  /* ボディ部分の範囲を保持する {@link QuadTreeItem} を返す.*/
  QuadTreeItem getBodyQtItem() {
    return bodyItem;
  }

  /* コネクタ部分分の範囲を保持する {@link QuadTreeItem} を返す.*/
  QuadTreeItem getConnectorQtItem() {
    return cnctrItem;
  }
}
