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

package net.seapanda.bunnyhop.model.node.event;

/**
 * ノードに対して登録されたイベントの種類を表す列挙型.
 *
 * @author K.Koike
 */
public enum BhNodeEvent {

  /** 子ノードからワークスペースに移されたときに発行されるイベント. */
  ON_MOVED_FROM_CHILD_TO_WS,
  /** ワークスペースもしくは, 子ノードから子ノードに移されたときに発行されるイベント. */
  ON_MOVED_TO_CHILD,
  /** 子ノードが入れ替わった時に発行されるイベント. */
  ON_CHILD_REPLACED,
  /** このノードが削除候補になったときに発行されるイベント. */
  ON_DELETION_REQUESTED,
  /** ユーザー操作により, このノードがカット&ペーストされるときに発行されるイベント. */
  ON_CUT_REQUESTED,
  /** ノードごとのノードテンプレートを作成するときに発行されるイベント. */
  ON_PRIVATE_TEMPLATE_CREATING,
  /** ユーザー操作により, このノードがコピー&ペーストされるときに発行されるイベント. */
  ON_COPY_REQUESTED,
  /** 構文エラーをチェックするときに発行されるイベント. */
  ON_SYNTAX_CHECKING,
  /** テキストノードのテキストを整形するときに発行されるイベント. */
  ON_TEXT_FORMATTING,
  /** テキストノードに入力されたテキストが受理可能かどうか判断するときに発行されるイベント. */
  ON_TEXT_CHECKING,
  /** ノードビューが提示する選択肢を生成するときに発行されるイベント. */
  ON_VIEW_OPTIONS_CREATING,
  /** テンプレートノードが作成されたときに発行されるイベント. */
  ON_TEMPLATE_CREATED,
}
