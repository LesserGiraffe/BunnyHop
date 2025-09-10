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

package net.seapanda.bunnyhop.bhprogram.debugger.variable;

import java.util.Optional;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * 変数の情報を格納するクラスの基底クラス.
 *
 * @author K.Koike
 */
public abstract class Variable {

  /** 変数の ID. */
  public final BhSymbolId id;
  /** 変数名. */
  public final String name;
  /** 変数に対応する {@link BhNode}. */
  private volatile BhNode node;

  /**
   * コンストラクタ.
   *
   * @param id 変数の ID
   * @param name 変数名
   * @param node 変数に対応する {@link BhNode}. (nullable)
   */
  Variable(BhSymbolId id, String name, BhNode node) {
    this.id = id;
    this.name = name;
    this.node = node;
  }

  /** このオブジェクトが情報を保持する変数に対応する {@link BhNode} を取得する. */
  public Optional<BhNode> getNode() {
    return Optional.ofNullable(node);
  }

  /**
   * このオブジェクトの変数に対応する {@link BhNode} を設定する.
   *
   * @param node 設定する {@link BhNode}. (nullable)
   */
  public void setNode(BhNode node) {
    this.node = node;
  }
}
