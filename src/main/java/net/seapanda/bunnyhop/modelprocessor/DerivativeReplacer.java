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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.attribute.DerivativeJointId;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 派生ノードの入れ替えを行うクラス.
 *
 * @author K.Koike
 */
public class DerivativeReplacer implements BhModelProcessor {

  /** undo 用コマンドオブジェクト. */
  UserOperation userOpe;
  /** このノードの派生ノードが置き換えられる可能性がある. */
  private BhNode orgOfOldDervs;
  /** 入れ替わった古い派生ノードと新しい派生ノードのペアのリスト. */
  private List<Swapped> swappedNodes = new ArrayList<>();

  /**
   * <pre> 
   * {@code orgOfNewDervs} の親ノードの派生ノードを A(0) ~ A(n-1) とする.
   * {@code orgOfOldDervs} の派生ノードを B(0) ~ B(m-1) とする.
   * 以下の条件 1 ~ 4 (5 は不問) を満たすとき, A(x) (x = 0 ~ n-1) の s につながる子ノードを r と入れ替える.
   *
   * 条件
   *   1. {@code orgOfNewDervs} の親コネクタに, 派生先 ID (p) が定義されている.
   *   2. p に対応する派生ノード r を {@code orgOfNewDervs} が持っている.
   *   3. {@code orgOfNewDervs} の先祖コネクタに, 派生ノード接続先 ID (q) が定義されている.
   *   4. A(x) が q が定義されたコネクタ (s) を持つ
   *   5. s に B(y) (y = 0 ~ m-1) が接続されている.
   * </pre>
   * 条件 3 ~ 5 だけを満たすとき B(y) を取り除く.
   *
   * @param orgOfNewDervs このノードの派生ノードで {@code orgOfOldDervs} の派生ノードを置き換える.
   * @param orgOfOldDervs このノードの派生ノードを入れ替える.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public static List<Swapped> replace(
      BhNode orgOfNewDervs, BhNode orgOfOldDervs, UserOperation userOpe) {
    var replaced = new DerivativeReplacer(orgOfOldDervs, userOpe);
    orgOfNewDervs.accept(replaced);
    return replaced.swappedNodes;
  }

  /**
   * コンストラクタ.
   *
   * @param orgOfOldDervs このノードの派生ノードを入れ替える.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public DerivativeReplacer(BhNode orgOfOldDervs, UserOperation userOpe) {
    this.userOpe = userOpe;
    this.orgOfOldDervs = orgOfOldDervs;
  }

  /** {@code orgOfNewDervs} の派生ノードを作成して, {@code orgOfNewDervs} の親ノードの派生ノードの子ノードと入れ替えを行う. */
  @Override
  public void visit(ConnectiveNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.getParentConnector().findDerivationId();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    // orgOfNewDervs に対応する派生ノードがある場合
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      // orgOfNewDervs の親ノードが持つ派生ノードを A(0) ~ A(n-1) としたとき, 
      // 新たに n 個の orgOfNewDervs の派生ノードを作成して, A(0) ~ A(n-1) の子ノードと入れ替える.
      replaceConnectiveChildren(
          orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
    } else {
      // orgOfNewDervs の親ノードが持つ派生ノードを A(0) ~ A(n-1) としたとき, A(0) ~ A(n-1) の子ノードを取り除く.
      removeConnectiveChildren(orgOfNewDervs.findParentNode().getDerivatives(), joint);
    }
  }

  @Override
  public void visit(TextNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.getParentConnector().findDerivationId();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      replaceConnectiveChildren(
          orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
    } else {
      removeConnectiveChildren(orgOfNewDervs.findParentNode().getDerivatives(), joint);
    }
  }

  /**
   * {@code parent} が持つコネクタの派生ノード接続位置が {@code jointId} と一致した場合そのノードを返す.
   *
   * @param parent 入れ替えもしくは削除の対象になる子派生ノードを持っているか探すノード
   * @param joint この ID を指定されたコネクタが {@code parent} の下にあった場合, そのコネクタに接続されたノードを返す
   * @return 入れ替えもしくは削除対象になるノード. 見つからなかった場合 Optional.empty を返す.
   */
  private Optional<BhNode> findNodeToReplace(
      ConnectiveNode parent, DerivativeJointId joint) {
    BhNode connectedNode = DerivativeFinder.find(parent, joint);
    if (connectedNode == null) {
      return Optional.empty();
    }
    if (connectedNode.isDeleted()) {
      return Optional.empty();
    }
    return Optional.of(connectedNode);
  }

  /**
   * {@code parents} の子ノードを {@code original} の派生ノードと入れ替える.
   *
   * @param parents 子ノードを入れ替える親ノードのリスト
   * @param original このノードの派生ノードで {@code parents} の子ノードを置き換える
   * @param joint {@code parents} の各ノードが持つコネクタに {@code jointId} が指定されていた場合,
   *                   そこに繋がる子ノードを入れ替えの対象とする.
   */
  private void replaceConnectiveChildren(
      Collection<ConnectiveNode> parents, Derivative original, DerivativeJointId joint) {
    for (ConnectiveNode parent : parents) {
      findNodeToReplace(parent, joint).ifPresent(
          toBeReplaced -> replaceConnectiveChildren(toBeReplaced, original));
    }
  }

  /**
   * {@code toBeReplaced} を {@code original} の派生ノードと入れ替える.
   *
   * @param toBeReplaced 入れ替えられるノード
   * @param original このノードの派生ノードで {@code toBeReplaced} を置き換える
   */
  private void replaceConnectiveChildren(BhNode toBeReplaced, Derivative original) { 
    Derivative newDerv = original.findOrCreateDerivative(toBeReplaced, userOpe);
    swappedNodes.addAll(toBeReplaced.replace(newDerv, userOpe));
  }

  /**
   * {@code parents} の子のうち, {@code jointId} で指定された位置に繋がるものが
   * {@link #orgOfOldDervs} の派生ノードであった場合, それを削除する.
   *
   * @param parents 子ノードを削除する {@link ConnectiveNode} のリスト
   * @param joint この派生ノード接続位置が指定されたコネクタに, 
   *              {@link #orgOfOldDervs} の派生ノードがつながっていた場合削除する.
   */
  private void removeConnectiveChildren(
      Collection<ConnectiveNode> parents, DerivativeJointId joint) {
    for (ConnectiveNode parent : parents) {
      findNodeToReplace(parent, joint).ifPresent(toRemove -> {
        if (toRemove.getOriginal() == orgOfOldDervs) {
          swappedNodes.addAll(toRemove.remove(userOpe));
        }
      });
    }
  }
}
