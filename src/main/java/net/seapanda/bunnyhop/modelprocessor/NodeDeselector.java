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

package net.seapanda.bunnyhop.modelprocessor;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノードツリーの全ノードを非選択にするクラス.
 *
 * @author K.Koike
 */
public class NodeDeselector implements BhModelProcessor {

  private final UserOperation userOpe;

  /**
   * 引数で指定したノード以下のノードを非選択にする.
   *
   * @param node このノード以下のノードを非選択にする.
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public static void deselect(BhNode node, UserOperation userOpe) {
    node.accept(new NodeDeselector(userOpe));
  }

  /**
   * コンストラクタ.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  private NodeDeselector(UserOperation userOpe) {
    this.userOpe = userOpe;
  }

  @Override
  public void visit(ConnectiveNode node) {
    if (node.isSelected()) {
      node.getWorkspace().removeSelectedNode(node, userOpe);
    }
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    if (node.isSelected()) {
      node.getWorkspace().removeSelectedNode(node, userOpe);
    }
  }
}
