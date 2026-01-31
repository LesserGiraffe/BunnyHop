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

package net.seapanda.bunnyhop.node.model.derivative;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.event.NodeEventInvoker;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * 派生ノードとオリジナルノードに対する操作を実装したクラス.
 *
 * @author K.Koike
 */
public abstract class DerivativeBase<T extends DerivativeBase<T>> extends Derivative {

  /** 派生先 ID とそれに対応する派生ノード ID のマップ. */
  private final Map<DerivationId, BhNodeId> derivationToDerivative;
  /** このオブジェクトが持つ派生ノードのリスト. */
  private final Set<T> derivatives;
  /** このノードが派生ノードの場合, そのオリジナルノードを保持する. */
  private T original = null;
  /** 最後にこのノードのオリジナルノードとなったノードを保持する. */
  private T lastOriginal = null;

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
      BhNodeParameters params,
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeFactory factory,
      DerivativeReplacer replacer,
      NodeEventInvoker invoker) {
    super(params, factory, replacer, invoker);
    this.derivationToDerivative = derivationToDerivative;
    derivatives = new HashSet<>();
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

    // 派生ノードをコピーした場合, コピー元と同じオリジナルノードの派生ノードとする
    if (org.isDerivative()) {
      T original = org.getOriginal();
      original.addDerivative(self(), userOpe);
    }
  }

  @Override
  public abstract CallbackRegistry getCallbackRegistry();


  @Override
  public final T getOriginal() {
    return original;
  }

  /** このノードのオリジナルノードを設定する. */
  void setOriginal(T original, UserOperation userOpe) {
    if (this.original == original) {
      return;
    }
    T oldOriginal = this.original;
    this.original = original;
    if (original != null) {
      lastOriginal = original;
    }
    getCallbackRegistry().onOriginalNodeChangedEventInvoker.invoke(
        new OriginalNodeChangeEvent(this, oldOriginal, original, userOpe));
  }

  @Override
  public final T getLastOriginal() {
    return lastOriginal;
  }

  /**
   * 派生ノードを追加する.
   *
   * @param derivative 追加する派生ノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addDerivative(T derivative, UserOperation userOpe) {
    if (!derivatives.contains(derivative)) {
      derivatives.add(derivative);
      derivative.setOriginal(self(), userOpe);
      userOpe.pushCmd(ope -> removeDerivative(derivative, ope));
    }
  }

  /**
   * {@code derivative} で指定したノードが, このノードの派生ノードであった場合,
   * このノードが保持する派生ノードの一覧から削除する.
   *
   * @param derivative 削除する派生ノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeDerivative(T derivative, UserOperation userOpe) {
    if (derivatives.remove(derivative)) {
      derivative.setOriginal(null, userOpe);
      userOpe.pushCmd(ope -> addDerivative(derivative, ope));
    }
  }

  @Override
  public Set<T> getDerivatives() {
    return new HashSet<>(derivatives);
  }

  @Override
  public boolean isDerivative() {
    return original != null;
  }

  @Override
  public boolean hasDerivatives() {
    return !derivatives.isEmpty();
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
    if (!isChild()) {
      return false;
    }
    if (!findRootNode().isRoot()) {
      return false;
    }
    // 同じ tree に含まれている場合置き換え不可
    if (node.isDescendantOf(this) || this.isDescendantOf(node)) {
      return false;
    }
    return parentConnector.canConnect(node);
  }

  /** {@link Derivative} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry extends BhNode.CallbackRegistry {

    /** 関連するノードのオリジナルノードが変わったときのイベントハンドラを管理するオブジェクト. */
    final ConsumerInvoker<OriginalNodeChangeEvent> onOriginalNodeChangedEventInvoker =
        new SimpleConsumerInvoker<>();

    @Override
    public ConsumerInvoker<OriginalNodeChangeEvent>.Registry getOnOriginalNodeChanged() {
      return onOriginalNodeChangedEventInvoker.getRegistry();
    }
  }
}
