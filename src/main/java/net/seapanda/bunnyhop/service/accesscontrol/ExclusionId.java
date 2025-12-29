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

package net.seapanda.bunnyhop.service.accesscontrol;

/**
 * アプリケーション全体で排他的に実行すべき処理を指定するための ID.
 *
 * @author K.Koike
 */
public enum ExclusionId {
  /** ノードの構造 (親子関係やオリジナル - 派生関係) の変更に関連する処理の ID. */
  NODE_MANIPULATION,
}
