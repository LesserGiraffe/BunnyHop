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
 *
 */

package net.seapanda.bunnyhop.node.view.effect;

/**
 * ノードビューに適用可能な視覚効果一覧.
 *
 * @author K.Koike
 */
public enum NodeViewEffectType {
  /** ノードが選択されていることを示す視覚効果. */
  SELECTION,
  /** まとまって移動するノード群であることを示す視覚効果. */
  MOVE_GROUP,
  /** 置き換え可能なノードが重なっていることを示す視覚効果. */
  OVERLAP,
  /** 実行中もしくは次に実行するノードであることを示す視覚効果. */
  EXEC_STEP,
  /** オリジナル - 派生関係にあるノード群であることを示す視覚効果. */
  DERIVATION_GROUP,
  /** ブレークポイントが指定されていることを示す視覚効果. */
  BREAKPOINT,
  /** 破損したノードであることを示す視覚効果. */
  CORRUPTION,
  /** 何らかの理由で注目されていること示す視覚効果. */
  MISC_FOCUSED,
}
