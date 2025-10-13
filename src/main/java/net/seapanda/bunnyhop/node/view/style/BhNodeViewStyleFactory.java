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

package net.seapanda.bunnyhop.node.view.style;


import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;

/**
 * {@link BhNodeViewStyle} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public interface BhNodeViewStyleFactory {
  /**
   * スタイル ID から ノードスタイルオブジェクトを取得する.
   *
   * @param styleId この ID のノードスタイルを取得する.
   * @return ノードスタイルオブジェクト.  対応するスタイルが見つからない場合は null.
   */
  public BhNodeViewStyle createStyleOf(BhNodeViewStyleId styleId);

  /**
   * {@code styleId} に対応する {@link BhNodeViewStyle} を作成可能か調べる.
   *
   * @param styleId この ID に対応する {@link BhNodeViewStyle} を作成可能か調べる.
   * @return {@code styleId} に対応する {@link BhNodeViewStyle} を作成可能な場合 true
   */
  public boolean canCreateStyleOf(BhNodeViewStyleId styleId);
}
