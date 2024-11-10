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

package net.seapanda.bunnyhop.model.node.derivative;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.modelprocessor.DerivativeBuilder;
import net.seapanda.bunnyhop.modelservice.DerivativeCache;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 派生ノードとオリジナルノードに対する操作を実装したクラス.
 *
 * @author K.Koike
 */
public abstract class DerivativeBase<T extends DerivativeBase<T>> extends Derivative {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** 派生先 ID とそれに対応する派生ノード ID のマップ. */
  private final Map<DerivationId, BhNodeId> derivationToDerivative;
  /** このオブジェクトが持つ派生ノードのリスト. */
  private final Set<T> derivatives;
  /** このノードが派生ノードの場合、そのオリジナルノードを保持する. */
  protected T original;

  /** サブタイプのインスタンスを返す. */
  protected abstract T self();

  /**
   * {@code derivationId} で指定した派生先 ID に対応した派生ノードを作成する.
   *
   * @param derivationId この派生先 ID に対応した派生ノードを作成する
   * @param userOpe undo 用コマンドオブジェクト
   * @return 作成された派生ノード. 派生ノードを持たないノードの場合 null を返す.
   */
  public abstract T createDerivative(DerivationId derivationId, UserOperation userOpe);

  /** コンストラクタ. */
  public DerivativeBase(
      BhNodeAttributes attributes, Map<DerivationId, BhNodeId> derivationToDeivative) {
    super(attributes);
    this.derivationToDerivative = derivationToDeivative;
    derivatives = new HashSet<>();
    original = null;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   * @param userOpe undo 用コマンドオブジェクト
   */
  public DerivativeBase(DerivativeBase<T> org, UserOperation userOpe) {
    super(org);
    derivationToDerivative = org.derivationToDerivative;
    derivatives = new HashSet<>();  //元ノードをコピーしても, 派生ノードとのつながりは無いようにする
    original = null;

    // 派生ノードをコピーした場合, コピー元と同じオリジナルノードの派生ノードとする
    if (org.isDerivative()) {
      T original = org.getOriginal();
      original.addDerivative(self(), userOpe);
    }
  }

  /**
   * このノードのオリジナルノードを返す.
   *
   * @return このノードのオリジナルノード.
   *         このノードが派生ノードではない場合 null.
   */
  @Override
  public final T getOriginal() {
    return original;
  }

  /**
   * 派生ノードを追加する.
   *
   * @param derivative 追加する派生ノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addDerivative(T derivative, UserOperation userOpe) {
    derivatives.add(derivative);
    derivative.original = self();
    userOpe.pushCmdOfAddDerivative(derivative, self());
  }

  /**
   * {@code derivative} で指定したノードが, このノードの派生ノードであった場合, 
   * このノードが保持する派生ノードの一覧から削除する.
   *
   * @param derivative 削除する派生ノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeDerivative(T derivative, UserOperation userOpe) {
    derivatives.remove(derivative);
    derivative.original = null;
    userOpe.pushCmdOfRemoveDerivative(derivative, self());
  }

  @Override
  public Collection<T> getDerivatives() {
    return new HashSet<>(derivatives);
  }

  @Override
  public boolean isDerivative() {
    return original != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Derivative findOrCreateDerivative(BhNode oldNode, UserOperation userOpe) {
    BhNode outerTailOfOldNode = oldNode.findOuterNode(-1);
    for (Derivative derivative : DerivativeCache.INSTANCE.get(this)) {
      if  (derivative.getLastReplaced() == null || !derivative.isDeleted()) {
        continue;
      }
      // oldNode の外部末尾ノードが, このノードと最後に入れ替わったノードの外部末尾ノードと
      // 一致する派生ノードを oldNode と入れ替える派生ノードとする
      if (derivative.getLastReplaced().findOuterNode(-1) == outerTailOfOldNode) {
        DerivativeCache.INSTANCE.remove(derivative);
        addDerivative((T) derivative, userOpe);
        return derivative;
      }
    }
    return DerivativeBuilder.buildFromAncestor(this, userOpe);
  }

  @Override
  public boolean hasDerivativeOf(DerivationId derivationId) {
    return derivationToDerivative.containsKey(derivationId);
  }


  @Override
  public BhNodeId getDerivativeIdOf(DerivationId derivationId) {
    Objects.requireNonNull(derivationId);
    return derivationToDerivative.get(derivationId);
  }

  @Override
  public boolean isRemovable() {
    if (parentConnector == null) {
      return false;
    }
    //デフォルトノードは移動不可
    if (isDefault()) {
      return false;
    }
    return !parentConnector.isFixed();
  }

  @Override
  public boolean canBeReplacedWith(BhNode node) {
    if (getState() != BhNode.State.CHILD) {
      return false;
    }
    if (findRootNode().getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS) {
      return false;
    }
    // 同じ tree に含まれている場合置き換え不可
    if (node.isDescendantOf(this) || this.isDescendantOf(node)) {
      return false;
    }
    return parentConnector.isConnectableWith(node);
  }
}
