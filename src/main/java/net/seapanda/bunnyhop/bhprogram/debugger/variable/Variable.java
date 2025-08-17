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

import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * 変数の情報を格納するクラスの基底クラス.
 *
 * @author K.Koike
 */
public abstract class Variable {

  /** 変数に対応する {@link BhNode}. */
  public final BhNode node;

  Variable(BhNode node) {
    this.node = node;
  }
}
