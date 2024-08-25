/**
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

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.imitation.ImitationID;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードツリーを作成するクラス
 * @author K.Koike
 */
public class ImitationBuilder implements BhModelProcessor {

  private final Deque<Imitatable> parentImitStack = new LinkedList<>();  //!< 現在処理中のBhNode の親がトップにくるスタック
  UserOperationCommand userOpeCmd;  //!< undo用コマンドオブジェクト
  //private boolean isManualCreation;  //!< トップノードのイミテーションを手動作成する場合true
  private final boolean buildMVC;
  private ImitationID imitationID = ImitationID.NONE;

  /**
   * {@code node} の先祖のコネクタに定義されたイミテーション ID を元にイミテーションノードを作成する.
   * @param node イミテーションを作成するオリジナルノード
   * @param buildMVC イミテーションノードの MVC 関係を構築する場合 true.
   * @param userOpeCmd undo用コマンドオブジェクト
   * @return 作成したイミテーションノードツリーのトップノード
   * */
  public static Imitatable buildFromImitIdOfAncestor(
    Imitatable node, boolean buildMVC, UserOperationCommand userOpeCmd) {

    var builder = new ImitationBuilder(ImitationID.NONE, buildMVC, userOpeCmd);
    node.accept(builder);
    return builder.parentImitStack.peekLast();
  }

  /**
   * {@code node} の {@code imitID} に対応するイミテーションノードを作成する.
   * @param node イミテーションを作成するオリジナルノード
   * @param imitID 作成するイミテーションのID.  {@code node} に対して, このイミテーション ID が定義されていること.
   * @param buildMVC イミテーションノードの MVC 関係を構築する場合 true.
   * @param userOpeCmd undo用コマンドオブジェクト
   * @return 作成したイミテーションノードツリーのトップノード
   */
  public static Imitatable build(
    Imitatable node, ImitationID imitID, boolean buildMVC, UserOperationCommand userOpeCmd) {

    var builder = new ImitationBuilder(imitID, buildMVC, userOpeCmd);
    node.accept(builder);
    return builder.parentImitStack.peekLast();
  }

  private ImitationBuilder(ImitationID imitID, boolean buildMVC, UserOperationCommand userOpeCmd) {

    this.imitationID = imitID;
    this.buildMVC = buildMVC;
    this.userOpeCmd = userOpeCmd;
  }

  /**
   * {@code node} のイミテーションノードを作成する.
   */
  @Override
  public void visit(ConnectiveNode node) {

    ImitationID imitID = ImitationID.NONE;
    if (!imitationID.equals(ImitationID.NONE)) {
      imitID = imitationID;
      imitationID = ImitationID.NONE;
    }
    else if (node.getParentConnector() != null) {
      imitID = node.getParentConnector().findImitationID();
    }

    if (!node.imitationNodeExists(imitID))
      return;

    if (parentImitStack.isEmpty()) {
      ConnectiveNode newImit = node.createImitNode(imitID, userOpeCmd);
      parentImitStack.addLast(newImit);
      node.sendToSections(this);
      if (buildMVC) {
        NodeMVCBuilder.build(newImit);
        TextImitationPrompter.prompt(newImit);
      }
    }
    else {
      Imitatable parentImit = parentImitStack.peekLast();
      //接続先を探す
      BhNode oldImit = ImitTaggedChildFinder.find(parentImit, node.getParentConnector().getImitCnctPoint());
      if (oldImit != null) {
        ConnectiveNode newImit = node.createImitNode(imitID, userOpeCmd);
        oldImit.replace(newImit, userOpeCmd);
        parentImitStack.addLast(newImit);
        node.sendToSections(this);
        parentImitStack.removeLast();
      }
    }
  }

  @Override
  public void visit(TextNode node) {

    ImitationID imitID = ImitationID.NONE;
    if (!imitationID.equals(ImitationID.NONE)) {
      imitID = imitationID;
      imitationID = ImitationID.NONE;
    }
    else if (node.getParentConnector() != null) {
      imitID = node.getParentConnector().findImitationID();
    }

    if (!node.imitationNodeExists(imitID))
      return;

    if (parentImitStack.isEmpty()) {
      TextNode newImit = node.createImitNode(imitID, userOpeCmd);
      parentImitStack.addLast(newImit);
      if (buildMVC) {
        NodeMVCBuilder.build(newImit);
        TextImitationPrompter.prompt(newImit);
      }
    }
    else {
      Imitatable parentImit = parentImitStack.peekLast();
      //接続先を探す
      BhNode oldImit = ImitTaggedChildFinder.find(parentImit, node.getParentConnector().getImitCnctPoint());
      if (oldImit != null) {
        TextNode newImit = node.createImitNode(imitID, userOpeCmd);
        oldImit.replace(newImit, userOpeCmd);
      }
    }
  }
}
