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

package net.seapanda.bunnyhop.compiler;

import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;

/**
 * コンパイルの対象となるノード一覧を提供する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface SourceSet {
  
  /**
   * プログラム開始時に実行されるノードを取得する.
   *
   * @return プログラム開始時に実行されるノード.  存在しない場合は null.
   */
  BhNode getMainEntryPoint();

  /**
   * コンパイルの対象となるノード群のうち, ルートノードであるものの一覧を取得する.
   *
   * @return コンパイルの対象となるノード群うち, ルートノードであるものの一覧
   */
  Set<BhNode> getRootNodes();
}
