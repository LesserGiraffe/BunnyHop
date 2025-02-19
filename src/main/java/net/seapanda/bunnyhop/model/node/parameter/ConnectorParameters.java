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

package net.seapanda.bunnyhop.model.node.parameter;

import java.io.Serializable;
import net.seapanda.bunnyhop.model.node.Connector;

/**
 * {@link Connector} のパラメータをまとめたレコード.
 *
 * @param connectorId コネクタ ID
 * @param name コネクタの名前
 * @param defaultNodeId デフォルトノード (コネクタに接続されているノードが削除されたときに代わりにつながるノード) の ID
 * @param derivationId 派生ノードを特定するための ID
 * @param derivativeJointId 派生ノードの接続先を特定するための ID
 * @param fixed 接続されたノードの削除や入れ替えができるかどうかのフラグ
 *
 * @author K.Koike
 */
public record ConnectorParameters(
    ConnectorId connectorId,    
    String name,
    BhNodeId defaultNodeId,
    DerivationId derivationId,
    DerivativeJointId derivativeJointId,
    boolean fixed) implements Serializable {}
