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

import java.util.Collection;
import java.util.Optional;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.imitation.ImitationConnectionPos;
import net.seapanda.bunnyhop.model.node.imitation.ImitationID;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.DeleteOperation;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードの入れ替えを行うクラス
 * @author K.Koike
 */
public class ImitationReplacer implements BhModelProcessor {

  UserOperationCommand userOpeCmd;  //!< undo用コマンドオブジェクト
  BhNode oldOriginal;  //!< イミテーションノードが置き換わるオリジナルノード

  /**
   * oldOriginal のイミテーションノードを newOriginal のイミテーションノードで置き換える.
   * @param newOriginal このノードのイミテーションノードで oldOriginal の
   * @param oldOriginal このノードのイミテーションノードを入れ替える.
   * @param userOpeCmd undo用コマンドオブジェクト
   * */
  public static void replace(BhNode newOriginal, BhNode oldOriginal, UserOperationCommand userOpeCmd) {
    newOriginal.accept(new ImitationReplacer(oldOriginal, userOpeCmd));
  }

  /**
   * コンストラクタ
   * @param oldOriginal このノードのイミテーションノードを入れ替える.
   * @param userOpeCmd undo用コマンドオブジェクト
   */
  public ImitationReplacer(BhNode oldOriginal, UserOperationCommand userOpeCmd) {
    this.userOpeCmd = userOpeCmd;
    this.oldOriginal = oldOriginal;
  }

  /**
   * @param newOriginal このノードのイミテーションを作成して、親ノードのイミテーションの子ノードと入れ替えを行う
   */
  @Override
  public void visit(ConnectiveNode newOriginal) {

    ImitationID imitID = newOriginal.getParentConnector().findImitationID();
    ImitationConnectionPos imitCnctPos = newOriginal.getParentConnector().getImitCnctPoint();
    //子オリジナルノードに対応するイミテーションがある場合
    if (newOriginal.imitationNodeExists(imitID)) {
      //オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
      replaceConnectiveChild(newOriginal.findParentNode().getImitationList(), newOriginal, imitCnctPos);
    }
    else {
      //オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
      removeConnectiveChild(newOriginal.findParentNode().getImitationList(), imitCnctPos);
    }
  }

  @Override
  public void visit(TextNode newOriginal) {

    ImitationID imitID = newOriginal.getParentConnector().findImitationID();
    ImitationConnectionPos imitCnctPos = newOriginal.getParentConnector().getImitCnctPoint();
    //子オリジナルノードに対応するイミテーションがある場合
    if (newOriginal.imitationNodeExists(imitID)) {
      //オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
      replaceConnectiveChild(newOriginal.findParentNode().getImitationList(), newOriginal, imitCnctPos);
    }
    else {
      //オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
      removeConnectiveChild(newOriginal.findParentNode().getImitationList(), imitCnctPos);
    }
  }

  /**
   * imitParentが持つコネクタのイミテーションタグが imitTag と一致した場合そのノードを返す
   * @param imitParent imitTag の位置に入れ替えもしくは remove 対象になるイミテーションノードを持っているか探すノード
   * @param imitCnctPos このイミテーションタグを指定されたコネクタが imitParent にあった場合, そのコネクタに接続されたノードを返す
   * @return 入れ替えもしくは削除対象になるノード. 見つからなかった場合 Optional.emptyを返す.
   */
  private Optional<BhNode> getNodeToReplaceOrRemove(
    ConnectiveNode imitParent, ImitationConnectionPos imitCnctPos) {

    BhNode connectedNode = ImitTaggedChildFinder.find(imitParent, imitCnctPos);  //すでにイミテーションにつながっているノード
    if (connectedNode == null)
      return Optional.empty();

    if (!connectedNode.isInWorkspace())  //遅延削除待ち状態のノード
      return Optional.empty();

    return Optional.of(connectedNode);
  }

  /**
   * ConnectiveNode の子を入れ替える
   * @param parentNodeList 子ノードを入れ替える ConnecitveNode のリスト
   * @param original このノードのイミテーションで子ノードを置き換える
   * @param imiCnctPos このイミテーション位置が指定されたコネクタにつながるノードを入れ替える
   */
  private void replaceConnectiveChild(
    Collection<ConnectiveNode> parentNodeList,
    Imitatable original,
    ImitationConnectionPos imiCnctPos) {

    for (ConnectiveNode parent : parentNodeList) {
      Optional<BhNode> nodeToReplace = getNodeToReplaceOrRemove(parent, imiCnctPos);
      nodeToReplace.ifPresent(imitToReplace -> {
        Connector parentCnctr = imitToReplace.getParentConnector();
        Imitatable newImit = original.findExistingOrCreateNewImit(imitToReplace, userOpeCmd);
        BhNodeHandler.INSTANCE.replaceChildNewlyCreated(imitToReplace, newImit, userOpeCmd);
        BhNodeHandler.INSTANCE.deleteNodeWithDelay(
          imitToReplace, userOpeCmd, DeleteOperation.REMOVE_FROM_IMIT_LIST);
        newImit.findParentNode().execScriptOnChildReplaced(imitToReplace, newImit, parentCnctr, userOpeCmd);
      });
    }
  }

  /**
   * ConnectiveNode の子を削除する
   * @param parentNodeList 子ノードを削除するConnecitveノードのリスト
   * @param imitCnctPos このイミテーション接続位置が指定されたコネクタにつながるノードを削除する
   */
  private void removeConnectiveChild(
    Collection<ConnectiveNode> parentNodeList, ImitationConnectionPos imitCnctPos) {

    for (ConnectiveNode parent : parentNodeList) {
      Optional<BhNode> nodeToRemove = getNodeToReplaceOrRemove(parent, imitCnctPos);
      nodeToRemove.ifPresent(node -> {
        //取り除くノードのオリジナルノードが入れ替え対象の古いノードであった場合
        if (node.getOriginal() == oldOriginal) {
          Connector parentCnctr = node.getParentConnector();
          BhNode newNode = BhNodeHandler.INSTANCE.removeChild(node, userOpeCmd);
          BhNodeHandler.INSTANCE.deleteNodeWithDelay(
            node, userOpeCmd, DeleteOperation.REMOVE_FROM_IMIT_LIST);
          newNode.findParentNode().execScriptOnChildReplaced(node, newNode, parentCnctr, userOpeCmd);
        }
      });
    }
  }
}
