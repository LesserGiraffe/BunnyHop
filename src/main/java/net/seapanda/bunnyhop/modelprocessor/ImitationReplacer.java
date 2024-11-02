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

import java.util.Collection;
import java.util.Optional;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.ImitCnctPosId;
import net.seapanda.bunnyhop.model.node.attribute.ImitationId;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.DeleteOperation;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードの入れ替えを行うクラス.
 *
 * @author K.Koike
 */
public class ImitationReplacer implements BhModelProcessor {

  /** undo 用コマンドオブジェクト. */
  UserOperationCommand userOpeCmd;
  /** イミテーションノードが置き換わるオリジナルノード. */
  BhNode oldOriginal;

  /**
   * oldOriginal のイミテーションノードを newOriginal のイミテーションノードで置き換える.
   *
   * @param newOriginal このノードのイミテーションノードで oldOriginal の
   * @param oldOriginal このノードのイミテーションノードを入れ替える.
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public static void replace(
      BhNode newOriginal, BhNode oldOriginal, UserOperationCommand userOpeCmd) {
    newOriginal.accept(new ImitationReplacer(oldOriginal, userOpeCmd));
  }

  /**
   * コンストラクタ.
   *
   * @param oldOriginal このノードのイミテーションノードを入れ替える.
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public ImitationReplacer(BhNode oldOriginal, UserOperationCommand userOpeCmd) {
    this.userOpeCmd = userOpeCmd;
    this.oldOriginal = oldOriginal;
  }

  /** {@code newOriginal} のイミテーションを作成して, 親ノードのイミテーションの子ノードと入れ替えを行う. */
  @Override
  public void visit(ConnectiveNode newOriginal) {
    ImitationId imitId = newOriginal.getParentConnector().findImitationId();
    ImitCnctPosId imitCnctPosId = newOriginal.getParentConnector().getImitCnctPos();
    //子オリジナルノードに対応するイミテーションがある場合
    if (newOriginal.imitationNodeExists(imitId)) {
      //オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
      replaceConnectiveChildren(
          newOriginal.findParentNode().getImitationList(), newOriginal, imitCnctPosId);
    } else {
      //オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
      removeConnectiveChildren(newOriginal.findParentNode().getImitationList(), imitCnctPosId);
    }
  }

  @Override
  public void visit(TextNode newOriginal) {
    ImitationId imitId = newOriginal.getParentConnector().findImitationId();
    ImitCnctPosId imitCnctPosId = newOriginal.getParentConnector().getImitCnctPos();
    //子オリジナルノードに対応するイミテーションがある場合
    if (newOriginal.imitationNodeExists(imitId)) {
      //オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
      replaceConnectiveChildren(
          newOriginal.findParentNode().getImitationList(), newOriginal, imitCnctPosId);
    } else {
      //オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
      removeConnectiveChildren(newOriginal.findParentNode().getImitationList(), imitCnctPosId);
    }
  }

  /**
   * {@cide imitParent} が持つコネクタのイミテーションタグが imitTag と一致した場合そのノードを返す.
   *
   * @param imitParent imitTag の位置に入れ替えもしくは削除の対象になるイミテーションノードを持っているか探すノード
   * @param imitCnctPosId この ID を指定されたコネクタが {@code imitParent} にあった場合, そのコネクタに接続されたノードを返す
   * @return 入れ替えもしくは削除対象になるノード. 見つからなかった場合 Optional.empty を返す.
   */
  private Optional<BhNode> getNodeToReplaceOrRemove(
      ConnectiveNode imitParent, ImitCnctPosId imitCnctPosId) {
    // イミテーションにつながっているノードを探す.
    BhNode connectedNode = ImitTaggedChildFinder.find(imitParent, imitCnctPosId);
    if (connectedNode == null) {
      return Optional.empty();
    }
    // 遅延削除待ち状態のノード
    if (!connectedNode.isInWorkspace()) {
      return Optional.empty();
    }
    return Optional.of(connectedNode);
  }

  /**
   * {@code parentNodeList} の子を {@code original} イミテーションノードと入れ替える.
   *
   * @param parentNodeList 子ノードを入れ替える ConnecitveNode のリスト
   * @param original このノードのイミテーションで子ノードを置き換える
   * @param imiCnctPos このイミテーション位置が指定されたコネクタにつながるノードを入れ替える
   */
  private void replaceConnectiveChildren(
      Collection<ConnectiveNode> parentNodeList,
      Imitatable original,
      ImitCnctPosId imiCnctPos) {

    for (ConnectiveNode parent : parentNodeList) {
      getNodeToReplaceOrRemove(parent, imiCnctPos)
          .ifPresent(toBeReplaced -> replaceConnectiveChildren(toBeReplaced, original));
    }
  }

  /**
   * {@code toBeReplaced} を {@code original} のイミテーションノードと入れ替える.
   *
   * @param toBeReplaced 入れ替えられるノード
   * @param original このノードのイミテーションで子ノードを置き換える
   */
  private void replaceConnectiveChildren(BhNode toBeReplaced, Imitatable original) {
    final Connector parentCnctr = toBeReplaced.getParentConnector();
    Imitatable newImit = original.findExistingOrCreateNewImit(toBeReplaced, userOpeCmd);
    BhNodeHandler.INSTANCE.addRootNode(toBeReplaced.getWorkspace(), newImit, 0, 0, userOpeCmd);
    BhNodeHandler.INSTANCE.replaceChild(toBeReplaced, newImit, userOpeCmd);
    BhNodeHandler.INSTANCE.deleteNodeWithDelay(
        toBeReplaced, userOpeCmd, DeleteOperation.REMOVE_FROM_IMIT_LIST);
    newImit.findParentNode().execOnChildReplaced(
        toBeReplaced, newImit, parentCnctr, userOpeCmd);
  }

  /**
   * {@code parentNodeList} の子のうち, {@code imitCnctPosId} で指定された位置に繋がるものをを削除する.
   *
   * @param parentNodeList 子ノードを削除する {@link ConnectiveNode} のリスト
   * @param imitCnctPosId このイミテーション接続位置が指定されたコネクタにつながるノードを削除する
   */
  private void removeConnectiveChildren(
      Collection<ConnectiveNode> parentNodeList, ImitCnctPosId imitCnctPosId) {
    for (ConnectiveNode parent : parentNodeList) {
      getNodeToReplaceOrRemove(parent, imitCnctPosId).ifPresent(node -> {
        if (node.getOriginal() == oldOriginal) {
          remove(node);
        }
      });
    }
  }

  private void remove(BhNode node) {
    Connector parentCnctr = node.getParentConnector();
    BhNode newNode = BhNodeHandler.INSTANCE.removeChild(node, userOpeCmd);
    BhNodeHandler.INSTANCE.deleteNodeWithDelay(
        node, userOpeCmd, DeleteOperation.REMOVE_FROM_IMIT_LIST);
    newNode.findParentNode().execOnChildReplaced(node, newNode, parentCnctr, userOpeCmd);
  }
}
