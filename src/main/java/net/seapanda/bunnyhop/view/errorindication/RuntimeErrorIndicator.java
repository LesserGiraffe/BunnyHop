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

package net.seapanda.bunnyhop.view.errorindication;

import java.util.Map;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * 実行時エラーの表示に必要な情報を保持するクラス.
 *
 * @author K.Koike
 */
class RuntimeErrorIndicator {

  /**
   * コンストラクタ.
   *
   * @param exception 実行時エラー情報を含む例外オブジェクト
   * @param nodeIdToNodeView エラー表示を追加するビューの一覧
   */
  public RuntimeErrorIndicator(
      BhProgramException exception, Map<BhNodeId, BhNodeView> nodeIdToNodeView) { }
}
