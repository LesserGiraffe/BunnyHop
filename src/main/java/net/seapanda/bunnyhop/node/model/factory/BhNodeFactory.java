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

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * {@link BhNode} の作成と MVC 構造の作成機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeFactory {
  
  /**
   * ノード ID から {@link BhNode} を新しく作る.
   *
   * @param id 作成したいノードの ID
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code id} で指定した {@link BhNode} のオブジェクト.  
   *         {@code id} に対応するノードが見つからなかった場合は null.
   */
  BhNode create(BhNodeId id, UserOperation userOpe);

  /**
   * ノード ID から {@link BhNode} を新しく作る.
   *
   * @param id 作成したいノードの ID
   * @param type 作成したノードに対して適用する MVC 構造
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code id} で指定した {@link BhNode} のオブジェクト.
   *         {@code id} に対応するノードが見つからなかった場合は null.
   */
  BhNode create(BhNodeId id, MvcType type, UserOperation userOpe);

  /**
   * {@code node} 以下のノードに対し MVC 構造を作成する.
   *
   * @param node このノード以下のノードの MVC 構造を作成する
   * @param type 作成する MVC 構造のタイプ
   * @return 引数で指定したノードに対応する {@link BhNodeView}.
   *         View の作成に失敗した場合 null.
   *         {@code type} に {@code MvcType.NONE} を指定した場合も null.
   *         既に MVC 構造が構築されている場合, 何もせずに既存のビューを返す.
   */
  BhNodeView setMvc(BhNode node, MvcType type);

  /** ノード ID が {@code id} である {@link BhNode} を作れるか調べる. */
  boolean canCreate(BhNodeId id);

  /** MVC 構造のタイプ. */
  enum MvcType {
    /** MVC 構造を持たない. */
    NONE,
    /** ワークスペース上で操作可能なノード用の MVC 構造. */
    DEFAULT,
    /** テンプレートノード用の MVC 構造. */
    TEMPLATE,
  }
}
