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

package net.seapanda.bunnyhop.node.model.event;

/**
 * ノードおよびコネクタに対して定義されたイベントの種類を表す列挙型.
 *
 * @author K.Koike
 */
public enum EventType {

  /** 子ノードがワークスペースに移されたときに発行されるイベント. */
  ON_MOVED_FROM_CHILD_TO_WS,
  /** ワークスペースから子ノードに移されたときに発行されるイベント. */
  ON_MOVED_FROM_WS_TO_CHILD,
  /** 子ノードが入れ替わった時に発行されるイベント. */
  ON_CHILD_REPLACED,
  /** このノードが削除候補になったときに発行されるイベント. */
  ON_DELETION_REQUESTED,
  /** ユーザー操作により, このノードがカット&ペーストされるときに発行されるイベント. */
  ON_CUT_REQUESTED,
  /** コンパニオンノードを生成するときに発行されるイベント. */
  ON_COMPANION_NODES_CREATING,
  /** ユーザー操作により, このノードがコピー&ペーストされるときに発行されるイベント. */
  ON_COPY_REQUESTED,
  /** コンパイルエラーをチェックするときに発行されるイベント. */
  ON_COMPILE_ERR_CHECKING,
  /** テキストノードのテキストを整形するときに発行されるイベント. */
  ON_TEXT_FORMATTING,
  /** テキストノードに入力されたテキストが受理可能かどうか判断するときに発行されるイベント. */
  ON_TEXT_CHECKING,
  /** テキストノードに設定可能な要素の選択肢を作成するときに発行されるイベント. */
  ON_TEXT_OPTIONS_CREATING,
  /** テンプレートノードが作成されたときに発行されるイベント. */
  ON_CREATED_AS_TEMPLATE,
  /** ノードのドラッグ操作が始まったときに発行されるイベント. */
  ON_UI_EVENT_RECEIVED,
  /** コネクタにノードが接続可能か調べるときに発行されるイベント. */
  ON_CONNECTABILITY_CHECKING,
  /** ノードのエイリアスを取得するときに発行されるイベント. */
  ON_ALIAS_ASKED,
  /** ノードのユーザ定義名を取得するときに発行されるイベント. */
  ON_USER_DEFINED_NAME_ASKED,
  /** あるノードに関連するノード群を強調表示するときに発行されるイベント. */
  ON_RELATED_NODES_REQUIRED,
}
