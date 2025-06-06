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

package net.seapanda.bunnyhop.model.node.event;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;

/**
 * コネクタに対して定義されたイベントハンドラを呼び出すオブジェクトのインタフェース.
 *
 * @author K.Koike
 */
public interface ConnectorEventInvoker {
  
  /**
   * 引数で指定したノードが {@code target} に接続可能か調べる.
   *
   * @param target イベントハンドラが定義されたコネクタ
   * @param node 接続可能か調べるノード.
   * @return 引数で指定したノードがこのコネクタに接続可能な場合 true を返す
   */
  boolean onConnectabilityChecking(Connector target, BhNode node);
}
