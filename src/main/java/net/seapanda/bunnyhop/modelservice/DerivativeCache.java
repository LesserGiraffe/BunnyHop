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

package net.seapanda.bunnyhop.modelservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;

/**
 * オリジナルノードと派生ノードの対応を保持するクラス.
 *
 * @author K.Koike
 */
public class DerivativeCache {
  
  public static final DerivativeCache INSTANCE = new DerivativeCache();
  /** インスタンス ID と派生ノードのマップ. */
  private final Map<Derivative, Set<Derivative>> orgToDerivatives = new HashMap<>();

  private DerivativeCache() {}

  /**
   * {@code derivative} で指定した派生ノードをキャッシュに格納する.
   * <p>
   * 格納された派生ノードは, {@link #get} メソッドで {@code derivative} のオリジナルノードを指定することで取得できる.
   * {@code derivative} が派生ノードでなかった場合, 何もしない.
   * </p>
   */
  public void put(Derivative derivative) {
    if (derivative.getOriginal() instanceof Derivative derv) {
      orgToDerivatives.putIfAbsent(derv, new HashSet<>());
      orgToDerivatives.get(derv).add(derivative);
    }
  }

  /** {@code original} の派生ノードとして格納されたノード一式を取得する.*/
  public Set<Derivative> get(Derivative original) {
    return new HashSet<>(orgToDerivatives.getOrDefault(original, new HashSet<>()));
  }

  /** {@code derivative} で指定した派生ノードをキャッシュから削除する. */
  public void remove(Derivative derivative) {
    for (Set<Derivative> dervs : orgToDerivatives.values()) {
      dervs.remove(derivative);
    }
  }

  /** キャッシュされている派生ノードを全て消す. */
  public void clearAll() {
    orgToDerivatives.clear();
  }
}
