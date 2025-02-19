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

package net.seapanda.bunnyhop.bhprogram;

import java.util.Collection;
import java.util.Map;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;

/**
 * 実行可能なノードの一覧を保持するクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface ExecutableNodeSet {
  
  /**
   * プログラム開始時に実行されるノードを取得する.
   *
   * @return プログラム開始時に実行されるノード.  存在しない場合は null.
   */
  BhNode getEntryPoint();

  /**
   * 全ルートノードのスナップショットを返す.
   *
   * @return 全ルートノードのスナップショット
   */
  Collection<BhNode> getRootNodeList();

  /**
   * インスタンス ID とそれに対応するノード (スナップショット) のマップを返す.
   *
   * @return インスタンス ID とそれに対応するノード (スナップショット) のマップ
   */
  Map<InstanceId, BhNode> getMapOfSymbolIdToNode();
}
