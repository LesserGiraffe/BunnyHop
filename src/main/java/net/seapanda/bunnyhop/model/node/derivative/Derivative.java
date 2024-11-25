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
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;

/**
 * 派生ノードおよびオリジナルノードのインタフェース.
 *
 * @author K.Koike
 */
public abstract class Derivative extends BhNode {

  protected Derivative(BhNodeAttributes attributes) {
    super(attributes);
  }

  protected Derivative(Derivative org) {
    super(org);
  }

  /**
   * 派生ノードであった場合 true を返す.
   *
   * @return 派生ノードであった場合true を返す
   */
  public abstract boolean isDerivative();

  /**
   * 引数で指定した派生先 ID に対応する派生ノード ID がある場合 true を返す.
   *
   * @param derivationId この派生先 ID に対応する派生ノード ID があるか調べる
   * @return 派生ノード ID が指定してある場合 true
   */
  public abstract boolean hasDerivativeOf(DerivationId derivationId);

  /**
   * {@code derivationId} で指定した派生先 ID に対応する派生ノード ID を返す.
   *
   * @param derivationId この派生先 ID に対応する派生ノード ID を返す
   * @return 引数で指定したコネクタ名に対応する派生ノード ID
   */
  public abstract BhNodeId getDerivativeIdOf(DerivationId derivationId);

  /**
   * このノードが持つ派生ノードリストを取得する.
   *
   * @return 派生ノードリスト. 派生ノードが存在し場合は空のコレクション
   */
  public abstract Collection<? extends Derivative> getDerivatives();
}
