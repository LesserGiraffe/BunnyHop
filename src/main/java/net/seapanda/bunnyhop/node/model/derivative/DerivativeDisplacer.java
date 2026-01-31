/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenss/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.node.model.derivative;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.node.model.service.DerivativeCache;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * 派生ノードの入れ替えを行うクラス.
 *
 * @author K.Koike
 */
public class DerivativeDisplacer implements BhNodeWalker {

  /** undo 用コマンドオブジェクト. */
  private final UserOperation userOpe;
  /** このノードの派生ノードが置き換えられる可能性がある. */
  private final BhNode orgOfOldDervs;
  /** 入れ替える新しい派生ノードをこのキャッシュから探す. */
  private final DerivativeCache cache;
  /** 入れ替わった古い派生ノードと新しい派生ノードのペアのリスト. */
  private final Set<Swapped> swappedNodes = new HashSet<>();

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
   * C_r(i) を q と入れ替える. (q は入れ替える個数分作成する, もしくは {@code cache} から見つける.)
   * q が存在しない場合, C_r(i) ∈ E であったなら, その C_r(i) を取り除く.
   * </pre>
   * 
   * <pre>
   * -- {@code cache} から入れ替える新しいノードを取得する場合の条件について. --
   * 
   * {@code cache} にある削除済みの派生ノードの中で, {@code orgOfNewDervs} の派生ノードであったものの集合を A とする.
   * 以下の条件を満たす a (∈ A) があるとき, それを入れ替える新しいノードとして使用する.
   * 
   * 1. a の {@link BhNodeId} が, {@code orgOfNewDervs} と
   *    その先祖コネクタが持つ {@link DerivationId} で特定される {@link BhNodeId} に一致する.
   * 2. a と最後に入れ替わったノードと C_r(i) が直系である
   * 3. a 以下の各子孫ノード b に対し 4 が満たされる
   * 4. b を最後に保持していたオリジナルノードが削除されていない
   * </pre>
   *
   * @param orgOfNewDervs このノードの派生ノードで {@code orgOfOldDervs} の派生ノードを置き換える.
   * @param orgOfOldDervs このノードの派生ノードを取り除くまたは入れ替える.
   * @param cache 入れ替えられたまたは取り除かれた派生ノードをこのキャッシュに格納する.
   *              特定の条件 (関数コメントを参照) を満たした場合, 置き換える新しいノードをこのキャッシュから取得する.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public static Set<Swapped> displace(
      BhNode orgOfNewDervs, BhNode orgOfOldDervs, DerivativeCache cache, UserOperation userOpe) {
    var replacer = new DerivativeDisplacer(orgOfOldDervs, cache, userOpe);
    orgOfNewDervs.accept(replacer);
    return replacer.swappedNodes;
  }

  /**
   * コンストラクタ.
   *
   * @param orgOfOldDervs このノードの派生ノードを入れ替える.
   * @param cache このキャッシュに対して派生ノードの格納及び取得を行う
   * @param userOpe undo 用コマンドオブジェクト
   */
  private DerivativeDisplacer(BhNode orgOfOldDervs, DerivativeCache cache, UserOperation userOpe) {
    this.userOpe = userOpe;
    this.cache = cache;
    this.orgOfOldDervs = orgOfOldDervs;
  }

  @Override
  public void visit(ConnectiveNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.findDerivationIdUp();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    // orgOfNewDervs に対応する派生ノードがある場合
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      replaceChildren(orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
    } else {
      removeConnectiveChildren(orgOfNewDervs.findParentNode().getDerivatives(), joint);
    }
  }

  @Override
  public void visit(TextNode orgOfNewDervs) {
    DerivationId derivationId = orgOfNewDervs.findDerivationIdUp();
    DerivativeJointId joint = orgOfNewDervs.getParentConnector().getDerivativeJoint();
    if (orgOfNewDervs.hasDerivativeOf(derivationId)) {
      replaceChildren(orgOfNewDervs.findParentNode().getDerivatives(), orgOfNewDervs, joint);
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
      Collection<ConnectiveNode> parents, BhNode original, DerivativeJointId joint) {
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
  private void replaceChild(BhNode toBeReplaced, BhNode original) {
    BhNode newDerv = findOrCreateDerivative(toBeReplaced, original, userOpe);
    swappedNodes.addAll(toBeReplaced.replace(newDerv, userOpe));
    cache.put(toBeReplaced);
  }

  /**
   * 派生ノードのキャッシュ ({@link #cache}) から
   * {@code toBeReplaced} と入れ替える {@code original} の派生ノードを探す.
   * 見つからない場合は, 新規作成する.
   * 
   * <pre>
   * {@link #cache} にある削除済みの派生ノードの中で, {@code original} の派生ノードであったものの集合を A とする.
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
  private BhNode findOrCreateDerivative(
      BhNode toBeReplaced, BhNode original, UserOperation userOpe) {
    DerivationId derivationId = original.findDerivationIdUp();
    BhNodeId derivativeNodeId = original.getDerivativeIdOf(derivationId);
    for (BhNode derivative : cache.get(original)) {
      if (derivative.getLastOriginal() == original
          && derivative.getId().equals(derivativeNodeId)
          && toBeReplaced.isLinealWith(derivative.getLastReplaced())
          && allDeleted(derivative)
          && !anyOriginalDeleted(derivative)) {
        cache.remove(derivative);
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
   * @return true {@code root} 以下のノードが全て削除済みであった. <br>
   *         false {@code root} 以下に削除されていないノードがあった.
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
   * @return true {@code root} 以下の各ノード a の中に, a を保持していた最後のオリジナルノードが削除さているものが有った. <br>
   *         false {@code root} 以下のノード a の中に, a を保持していた最後のオリジナルノードが削除さているものは無かった.
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
          cache.put(toRemove);
        }
      });
    }
  }

  /**
   * オリジナルノードに派生ノードを割り当てるクラス.
   */
  private static class DerivativeReassigner implements BhNodeWalker {

    private final UserOperation userOpe;

    /** {@code root} 以下のノードが元々派生ノードであった場合, 元のオリジナルノードの派生ノード一式に加える. */
    public static void assign(BhNode root, UserOperation userOpe) {
      root.accept(new DerivativeReassigner(userOpe));
    }

    private DerivativeReassigner(UserOperation userOpe) {
      this.userOpe = userOpe;
    }

    @Override
    public void visit(ConnectiveNode node) {
      if (node.getOriginal() == null && node.getLastOriginal() != null) {
        node.getLastOriginal().addDerivative(node, userOpe);
      }
      node.sendToSections(this);
    }

    @Override
    public void visit(TextNode node) {
      if (node.getOriginal() == null && node.getLastOriginal() != null) {
        node.getLastOriginal().addDerivative(node, userOpe);
      }
    }
  }
}
