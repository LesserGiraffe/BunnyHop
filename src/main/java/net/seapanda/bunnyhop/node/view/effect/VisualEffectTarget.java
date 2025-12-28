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
 * ノードビューの視覚効果を適用する対象の選択肢.
 *
 * @author K.Koike
 */
public enum VisualEffectTarget {
  /** 指定したノードビュー自身. */
  SELF,
  /** 指定したノードビューとその全ての子要素. */
  CHILDREN,
  /** 指定したノードビューとそこから外部ノードビューのみを辿って到達できる全てのノードビュー. */
  OUTERS;

  /** 外部スクリプトから列挙子にアクセスするための変数. */
  public static final VisualEffectTarget T_SELF = SELF;
  public static final VisualEffectTarget T_CHILDREN = CHILDREN;
  public static final VisualEffectTarget T_OUTERS = OUTERS;
}
