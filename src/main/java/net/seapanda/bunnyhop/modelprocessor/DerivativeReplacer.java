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
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.attribute.DerivativeJointId;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.modelprocessor.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.service.DerivativeCache;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * 派生ノードの入れ替えを行うクラス.
 *
 * @author K.Koike
 */
public class DerivativeReplacer implements BhNodeWalker {

  /** undo 用コマンドオブジェクト. */
  UserOperation userOpe;
  /** このノードの派生ノードが置き換えられる可能性がある. */
  private BhNode orgOfOldDervs;
  /** 入れ替わった古い派生ノードと新しい派生ノードのペアのリスト. */
  private List<Swapped> swappedNodes = new ArrayList<>();

  /**
   * <pre>
   * {@code orgOfOldDervs} の派生ノード一式を E とする
   * {@code orgOfNewDervs} の先祖コネクタが持つ派生先 ID を p とする
   * {@code orgOfNewDervs} の p に対応する派生ノードを q とする
   * {@code orgOfNewDervs} の親コネクタが持つ, 派生ノード接続先 ID を r とする.
   * {@code orgOfNewDervs} の親ノードが持つ m 個の派生ノードを D(0) ~ D(m-1) とする
   * D(i) の子コネクタの中で r が指定してあるものがあれば, それを D_r(i) とする.  (i = 0, 1, 2, ..., m - 1)
   * D_r(i) の子ノードを C_r(i) とする.
   * 
   * C_r(i) の子ノードを q と入れ替える. (q は入れ替える個数分作成する)
   * q が存在しない場合, C_r(i) ∈ E であったなら, その C_r(i) を取り除く.
   * </pre>
   *
   * @param orgOfNewDervs このノードの派生ノードで {@code orgOfOldDervs} の派生ノードを置き換える.
   * @param orgOfOldDervs このノードの派生ノードを取り除くまたは入れ替える.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public static List<Swapped> replace(
      BhNode orgOfNewDervs, BhNode orgOfOldDervs, UserOperation userOpe) {
    var replacer = new DerivativeReplacer(orgOfOldDervs, userOpe);
    orgOfNewDervs.accept(replacer);
    return replacer.swappedNodes;
  }

  /**
   * コンストラクタ.
   *
   * @param orgOfOldDervs このノードの派生ノードを入れ替える.
   * @param userOpe undo 用コマンドオブジェクト
   */
  private DerivativeReplacer(BhNode orgOfOldDervs, UserOperation userOpe) {
    this.userOpe = userOpe;
    this.orgOfOldDervs = orgOfOldDervs;
  }

  @Override
  public void visit(ConnectiveNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.findDerivationIdUp();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    // orgOfNewDervs に対応する派生ノードがある場合
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      replaceChildren(
          orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
    } else {
      removeConnectiveChildren(orgOfNewDervs.findParentNode().getDerivatives(), joint);
    }
  }

  @Override
  public void visit(TextNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.findDerivationIdUp();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      replaceChildren(
          orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
    } else {
      removeConnectiveChildren(orgOfNewDervs.findParentNode().getDerivatives(), joint);
    }
  }

  /**
   * {@code parent} が持つコネクタの派生ノード接続位置が {@code joint} と一致した場合そのノードを返す.
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
   * {@code parents} の子コネクタに {@code joint} が指定しれあるものがあれば, そのコネクタの子ノードを
   * {@code original} の派生ノードと入れ替える.
   *
   * @param parents このノードの子ノードから入れ替え対象を探す
   * @param original このノードの派生ノードで {@code parents} の子ノードを置き換える
   * @param joint {@code parents} の各ノードが持つコネクタに {@code joint} が指定されていた場合,
   *                   そこに繋がる子ノードを入れ替えの対象とする.
   */
  private void replaceChildren(
      Collection<ConnectiveNode> parents, Derivative original, DerivativeJointId joint) {
    for (ConnectiveNode parent : parents) {
      findNodeToReplace(parent, joint).ifPresent(
          toBeReplaced -> replaceChild(toBeReplaced, original));
    }
  }

  /**
   * {@code toBeReplaced} を {@code original} の派生ノードと入れ替える.
   *
   * @param toBeReplaced 入れ替えられるノード
   * @param original このノードの派生ノードで {@code toBeReplaced} を置き換える
   */
  private void replaceChild(BhNode toBeReplaced, Derivative original) {
    Derivative newDerv = findOrCreateDerivative(toBeReplaced, original, userOpe);
    swappedNodes.addAll(toBeReplaced.replace(newDerv, userOpe));
  }

  /**
   * 派生ノードのキャッシュ ({@link DerivativeCache}) から
   * {@code toBeReplaced} と入れ替える {@code original} の派生ノードを探す.
   * 見つからない場合は, 新規作成する.
   * 
   * <pre>
   * {@link DerivativeCache} にある削除済みの派生ノードの中で, {@code original} の派生ノードであったものの集合を A とする.
   * 以下の条件を満たす a (∈ A) があるとき, それを返す.
   * 条件を満たす a が存在しないとき, {@code original} の派生ノードを新規作成する.
   * 
   * 1. a の {@link BhNodeId} が, {@code original} と
   *    その先祖コネクタが持つ {@link DerivationId} で特定される {@link BhNodeId} に一致する.
   * 2. a と最後に入れ替わったノードと {@code toBeReplaced} が直系である
   * 3. a 以下の各ノード b に対し 4 が満たされる
   * 4. b を最後に保持していたオリジナルノードが削除されていない
   * </pre>
   *
   * @param toBeReplaced このノードと入れ替えるためのノードを探す.
   * @param original このノードの派生ノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code toReplace} と入れ替えるための派生ノード
   */
  private Derivative findOrCreateDerivative(
      BhNode toBeReplaced, Derivative original, UserOperation userOpe) {
    DerivationId derivationId = original.findDerivationIdUp();
    BhNodeId derivativeNodeId = original.getDerivativeIdOf(derivationId);
    for (Derivative derivative : DerivativeCache.INSTANCE.get(original)) {
      if (derivative.getLastOriginal() == original
          && derivative.getId().equals(derivativeNodeId)
          && toBeReplaced.isLinealWith(derivative.getLastReplaced())
          && allDeleted(derivative)
          && !anyOriginalDeleted(derivative)) {
        DerivativeCache.INSTANCE.remove(derivative);
        DerivativeReassigner.assign(derivative, userOpe);
        return derivative;
      }
    }
    return DerivativeBuilder.build(original, derivationId, userOpe);
  }


  /**
   * {@code root} 以下のノードが全て削除済みであるかどうか調べる.
   *
   * @param root このノード以下のノードが全て削除済みであるかどうか調べる
   * @retval true {@code root} 以下のノードが全て削除済みであった
   * @retval false {@code root} 以下に削除されていないノードがあった
   */
  private boolean allDeleted(BhNode root) {
    MutableBoolean allDeleted = new MutableBoolean(true);
    CallbackRegistry registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(node -> {
      if (!node.isDeleted()) {
        allDeleted.setFalse();
      }
    });
    CallbackInvoker.invoke(registry, root);
    return allDeleted.getValue();
  }

  /**
   * {@code root} 以下の各ノード a に対して, a を保持していた最後のオリジナルノードが削除されているかどうか調べる.
   *
   * @param root このノード以下の各ノード a に対して, a を保持していた最後のオリジナルノードが削除されているかどうか調べる
   * @retval true {@code root} 以下の各ノード a の中に, a を保持していた最後のオリジナルノードが削除さているものが有った
   * @retval false {@code root} 以下のノード a の中に, a を保持していた最後のオリジナルノードが削除さているものは無かった
   */
  private boolean anyOriginalDeleted(BhNode root) {
    MutableBoolean anyDeleted = new MutableBoolean(false);
    CallbackRegistry registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(node -> {
      if (node.getLastOriginal() != null && node.getLastOriginal().isDeleted()) {
        anyDeleted.setTrue();
      }
    });
    CallbackInvoker.invoke(registry, root);
    return anyDeleted.getValue();
  }

  /**
   * {@code parents} の子のうち, {@code joint} で指定された位置に繋がるものが
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

  /**
   * オリジナルノードに派生ノードを割り当てるクラス.
   */
  private static class DerivativeReassigner implements BhNodeWalker {

    private final UserOperation userOpe;

    /** {@code root} 以下の派生ノードをそのノードの最後のオリジナルノードの派生ノードとする. */
    public static void assign(BhNode root, UserOperation userOpe) {
      root.accept(new DerivativeReassigner(userOpe));
    }

    private DerivativeReassigner(UserOperation userOpe) {
      this.userOpe = userOpe;
    }

    @Override
    public void visit(ConnectiveNode node) {
      if (node.getLastOriginal() != null) {
        node.getLastOriginal().addDerivative(node, userOpe);
      }
      node.sendToSections(this);
    }

    @Override
    public void visit(TextNode node) {
      if (node.getLastOriginal() != null) {
        node.getLastOriginal().addDerivative(node, userOpe);
      }
    }
  }
}
