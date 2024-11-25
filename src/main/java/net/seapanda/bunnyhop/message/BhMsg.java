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

package net.seapanda.bunnyhop.message;

/**
 * MsgTransporter が送信するメッセージ一覧.
 *
 * @author K.Koike
 */
public enum BhMsg {

  /** ワークスペースにルートノードを追加. */
  ADD_ROOT_NODE,
  /** ワークスペースからルートノードが消えた. */
  REMOVE_ROOT_NODE,
  /** ノードビューを入れ替える. */
  REPLACE_NODE_VIEW,
  /** 新しいワークスペースを追加. */
  ADD_WORKSPACE,
  /** 新しいワークスペースを削除する. */
  DELETE_WORKSPACE,
  /** 現在選択中のワークスペースを取得する. */
  GET_CURRENT_WORKSPACE,
  /** ワークスペースの大きさを変える. */
  CHANGE_WORKSPACE_VIEW_SIZE,
  /**
   * 4文木に登録される矩形オブジェクトを追加する.
   * (このメッセージを受け取ったBhNodeController の BhNodeView以下の全てのViewの矩形を登録する)
   */
  SET_QT_RECTANGLE,
  /** 4文木に登録される矩形オブジェクトを削除する. */
  REMOVE_QT_RECTANGLE,
  /** ワークスペース上の位置を設定する. (4 分木空間上の位置も更新する). */
  SET_POS_ON_WORKSPACE,
  /** ワークスペース上の位置を取得する. */
  GET_POS_ON_WORKSPACE,
  /** ワークスペース上のノードを動かす. */
  MOVE_NODE_ON_WORKSPACE,
  /** BhNodeView の外部ノード込みの大きさを取得する. */
  GET_VIEW_SIZE_INCLUDING_OUTER,
  /** 絶対位置 (= workspace からの相対位置) を更新する. (=4 分木空間上の位置を更新する). */
  UPDATE_ABS_POS,
  /** 擬似クラスの有効/無効を切り替える. */
  SWITCH_PSEUDO_CLASS_ACTIVATION,
  /** ノード選択ビューを追加する. */
  ADD_NODE_SELECTION_PANEL,
  /** Scene 上での位置をワークスペース上での位置に直す. */
  SCENE_TO_WORKSPACE,
  /** ワークスペースのサイズを取得する. */
  GET_WORKSPACE_SIZE,
  /** ビューを取得する. */
  GET_VIEW,
  /** Undoを命令する. */
  UNDO,
  /** Redoを命令する. */
  REDO,
  /** ユーザー操作を表すオブジェクトを登録する. */
  SET_USER_OPE_CMD,
  /** ユーザー操作を表すオブジェクトをundoスタックに追加する. */
  PUSH_USER_OPE_CMD,
  /** undo, redo の対象になっているコマンドを削除する. */
  DELETE_USER_OPE_CMD,
  /** GUIツリー上からViewを消す. */
  REMOVE_FROM_GUI_TREE,
  /** WSのズーム処理. */
  ZOOM,
  /** ゴミ箱エリアに入っているかどうかを調べる. */
  IS_IN_TRASHBOX_AREA,
  /** ゴミ箱を開閉する. */
  OPEN_TRASHBOX,
  /** ノードの可視性をセットする. */
  SET_VISIBLE,
  /** ノードの構文エラー警告表示を変更する. */
  SET_SYNTAX_ERRPR_INDICATOR,
  /** マルチノードシフタ(複数ノード移動用マルチノードシフタ)とリンクを更新する. */
  UPDATE_MULTI_NODE_SHIFTER,
  /** BhNodeのボディのワークスペース上での範囲を取得する. */
  GET_NODE_BODY_RANGE,
  /** 貼り付け予定のノードを貼り付け候補から取り除く. */
  REMOVE_NODE_TO_PASTE,
  /** ノードビューの選択表示の有効/無効状態を切り替える. */
  SELECT_NODE_VIEW,
  /** 特定のノードビューをワークスぺース中央に表示する. */
  LOOK_AT_NODE_VIEW,
  /** テンプレートノードかどうかを調べる. */
  IS_TEMPLATE_NODE,
  /** TextNode が持つ文字列に合わせて, その View の内容を変更する. */
  MATCH_VIEW_CONTENT_TO_MODEL,
}
