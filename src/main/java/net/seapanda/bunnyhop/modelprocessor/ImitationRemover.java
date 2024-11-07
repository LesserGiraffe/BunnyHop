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
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードをオリジナルノードのイミテーションノードリストから取り除くクラス.
 *
 * @author K.Koike
 */
public class ImitationRemover implements BhModelProcessor {

  /** undo 用コマンドオブジェクト. */
  private UserOperationCommand userOpeCmd;

  /**
   * 引数で指定したノード以下にあるイミテーションノードをオリジナルノードのイミテーションノードリストから取り除く.
   *
   * @param node このノード以下のイミテーションノードを, オリジナルノードのイミテーションノードリストから取り除く.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * */
  public static void remove(BhNode node, UserOperationCommand userOpeCmd) {
    node.accept(new ImitationRemover(userOpeCmd));
  }

  /**
   * コンストラクタ.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private ImitationRemover(UserOperationCommand userOpeCmd) {
    this.userOpeCmd = userOpeCmd;
  }

  /**
   * {@code node} の削除処理を行う.
   *
   * @param node 削除するノード
   */
  @Override
  public void visit(ConnectiveNode node) {
    if (node.isImitationNode()) {
      node.getOriginal().removeImitation(node, userOpeCmd);
    }
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    if (node.isImitationNode()) {
      node.getOriginal().removeImitation(node, userOpeCmd);
    }
  }
}
