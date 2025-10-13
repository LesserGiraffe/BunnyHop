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

package net.seapanda.bunnyhop.node.model.factory;

import java.util.Collection;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;

/**
 * {@link BhNode} 取得する機能を持つクラス.
 *
 * @author K.Koike
 */
public interface BhNodeRepository {

  /**
   * {@code id} に対応する {@link BhNode} を取得する.
   *
   * @param nodeId このノード ID に対応する {@link BhNode} を取得する
   * @return {@code nodeId} に対応する {@link BhNode} オブジェクト.
   *         見つからない場合は null.
   */
  BhNode getNodeOf(BhNodeId nodeId);

  /** このリポジトリが保持する全ての {@link BhNode} を取得する. */
  Collection<BhNode> getAll();

  /**
   * {@code id} で指定したノード ID に対応する {@link BhNode} オブジェクトを取得可能か調べる.
   *
   * @return {@code id} で指定したノード ID に対応する {@link BhNode} オブジェクトを取得可能な場合 true
   */
  boolean hasNodeOf(BhNodeId id);
}
