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

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.ImitationId;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードツリーを作成するクラス.
 *
 * @author K.Koike
 */
public class ImitationBuilder implements BhModelProcessor {

  /** 現在処理中の {@link BhNode} の親がトップにくるスタック. */
  private final Deque<Imitatable> parentImitStack = new LinkedList<>();
  /** undo 用コマンドオブジェクト. */
  UserOperationCommand userOpeCmd;
  private final boolean buildMvc;
  private ImitationId imitationId = ImitationId.NONE;

  /**
   * {@code node} の先祖のコネクタに定義されたイミテーション ID を元にイミテーションノードを作成する.
   *
   * @param node イミテーションを作成するオリジナルノード
   * @param buildMvc イミテーションノードの MVC 関係を構築する場合 true.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 作成したイミテーションノードツリーのトップノード
   * */
  public static Imitatable buildFromImitIdOfAncestor(
      Imitatable node, boolean buildMvc, UserOperationCommand userOpeCmd) {
    var builder = new ImitationBuilder(ImitationId.NONE, buildMvc, userOpeCmd);
    node.accept(builder);
    return builder.parentImitStack.peekLast();
  }

  /**
   * {@code node} の {@code imitID} に対応するイミテーションノードを作成する.
   *
   * @param node イミテーションを作成するオリジナルノード
   * @param imitId 作成するイミテーションのID.  {@code node} に対して, このイミテーション ID が定義されていること.
   * @param buildMvc イミテーションノードの MVC 関係を構築する場合 true.
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 作成したイミテーションノードツリーのトップノード
   */
  public static Imitatable build(
      Imitatable node, ImitationId imitId, boolean buildMvc, UserOperationCommand userOpeCmd) {
    var builder = new ImitationBuilder(imitId, buildMvc, userOpeCmd);
    node.accept(builder);
    return builder.parentImitStack.peekLast();
  }

  private ImitationBuilder(ImitationId imitId, boolean buildMvc, UserOperationCommand userOpeCmd) {
    this.imitationId = imitId;
    this.buildMvc = buildMvc;
    this.userOpeCmd = userOpeCmd;
  }

  /**
   * {@code node} のイミテーションノードを作成する.
   */
  @Override
  public void visit(ConnectiveNode node) {
    ImitationId imitId = ImitationId.NONE;
    if (!imitationId.equals(ImitationId.NONE)) {
      imitId = imitationId;
      imitationId = ImitationId.NONE;
    } else if (node.getParentConnector() != null) {
      imitId = node.getParentConnector().findImitationId();
    }

    if (!node.imitationNodeExists(imitId)) {
      return;
    }
    if (parentImitStack.isEmpty()) {
      ConnectiveNode newImit = node.createImitNode(imitId, userOpeCmd);
      parentImitStack.addLast(newImit);
      node.sendToSections(this);
      if (buildMvc) {
        NodeMvcBuilder.build(newImit);
        TextImitationPrompter.prompt(newImit);
      }
    } else {
      Imitatable parentImit = parentImitStack.peekLast();
      //接続先を探す
      BhNode oldImit =
          ImitTaggedChildFinder.find(parentImit, node.getParentConnector().getImitCnctPos());
      if (oldImit != null) {
        ConnectiveNode newImit = node.createImitNode(imitId, userOpeCmd);
        oldImit.replace(newImit, userOpeCmd);
        parentImitStack.addLast(newImit);
        node.sendToSections(this);
        parentImitStack.removeLast();
      }
    }
  }

  @Override
  public void visit(TextNode node) {
    ImitationId imitId = ImitationId.NONE;
    if (!imitationId.equals(ImitationId.NONE)) {
      imitId = imitationId;
      imitationId = ImitationId.NONE;
    } else if (node.getParentConnector() != null) {
      imitId = node.getParentConnector().findImitationId();
    }

    if (!node.imitationNodeExists(imitId)) {
      return;
    }
    if (parentImitStack.isEmpty()) {
      TextNode newImit = node.createImitNode(imitId, userOpeCmd);
      parentImitStack.addLast(newImit);
      if (buildMvc) {
        NodeMvcBuilder.build(newImit);
        TextImitationPrompter.prompt(newImit);
      }
    } else {
      Imitatable parentImit = parentImitStack.peekLast();
      //接続先を探す
      BhNode oldImit =
          ImitTaggedChildFinder.find(parentImit, node.getParentConnector().getImitCnctPos());
      if (oldImit != null) {
        TextNode newImit = node.createImitNode(imitId, userOpeCmd);
        oldImit.replace(newImit, userOpeCmd);
      }
    }
  }
}
