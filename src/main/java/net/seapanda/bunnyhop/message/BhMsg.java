/**
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
 * MsgTransporter が送信するメッセージ一覧
 * @author K.Koike
 */
public enum BhMsg {

	ADD_ROOT_NODE,	 //!< ワークスペースにルートノードを追加
	REMOVE_ROOT_NODE, //!< ワークスペースからルートノードが消えた
	REPLACE_NODE_VIEW,	//!< ノードビューを入れ替える
	ADD_WORKSPACE,	//!< 新しいワークスペースを追加
	DELETE_WORKSPACE,	//!< 新しいワークスペースを削除する
	GET_CURRENT_WORKSPACE, //!< 現在選択中のワークスペースを取得する
	CHANGE_WORKSPACE_VIEW_SIZE,	//!< ワークスペースの大きさを変える
	ADD_QT_RECTANGLE,	//!< 4文木に登録される矩形オブジェクトを追加する(このメッセージを受け取ったBhNodeController の BhNodeView以下の全てのViewの矩形を登録する)
	REMOVE_QT_RECTANGLE, //!< 4文木に登録される矩形オブジェクトを削除する
	SET_POS_ON_WORKSPACE,	//!< ワークスペース上の位置を設定する. (4分木空間上の位置も更新する)
	GET_POS_ON_WORKSPACE,	//!< ワークスペース上の位置を取得する.
	MOVE_NODE_ON_WORKSPACE, //!< ワークスペース上のノードを動かす
	GET_VIEW_SIZE_INCLUDING_OUTER,	//!< BhNodeView の外部ノード込みの大きさを取得する
	UPDATE_ABS_POS,		//!< 絶対位置 (= workspace からの相対位置) を更新する. (=4分木空間上の位置を更新する)
	BUILD_NODE_CATEGORY_LIST_VIEW,	//!< GUI画面に BhNodeカテゴリ選択画面を追加する
	SWITCH_PSEUDO_CLASS_ACTIVATION,	//!< 擬似クラスの有効/無効を切り替える
	ADD_NODE_SELECTION_PANELS,	//!< BhNode のテンプレートの載ったパネルを追加する
	HIDE_NODE_SELECTION_PANEL,	//!< BhNode のテンプレートの載ったパネルを非表示にする
	SCENE_TO_WORKSPACE,		//!< Scene 上での位置をワークスペース上での位置に直す
	GET_WORKSPACE_SIZE,		//!< ワークスペースのサイズを取得する
	GET_VIEW,				//!< ビューを取得する
	UNDO,					//!< Undoを命令する
	REDO,					//!< Redoを命令する
	SET_USER_OPE_CMD,		//!< ユーザー操作を表すオブジェクトを登録する
	PUSH_USER_OPE_CMD,		//!< ユーザー操作を表すオブジェクトをundoスタックに追加する
	DELETE_USER_OPE_CMD,	//!< undo, redo の対象になっているコマンドを削除する
	REMOVE_FROM_GUI_TREE,	//!< GUIツリー上からViewを消す
	IMITATE_TEXT,			//!< イミテーションノードのテキストをオリジナルと一致させる
	GET_MODEL_AND_VIEW_TEXT,//!< モデルとビューのテキストを取得する
	ZOOM,					//!< WSのズーム処理
	IS_IN_TRASHBOX_AREA,	//!< ゴミ箱エリアに入っているかどうかを調べる
	OPEN_TRASHBOX,			//!< ゴミ箱を開閉する
	SET_VISIBLE,	//!< ノードの可視性をセットする
	SET_UNSCOPED_NODE_WARNING,	//!< ノードのスコープ外警告表示を変更する
	UPDATE_MULTI_NODE_SHIFTER,	//!< マルチノードシフタ(複数ノード移動用マルチノードシフタ)とリンクを更新する
	GET_NODE_BODY_RANGE, //!< BhNodeのボディのワークスペース上での範囲を取得する
	SET_TEXT,	//!< テキストを設定可能なノードにテキストを設定する. テキストフォーマットチェックは行われる.
	REMOVE_NODE_TO_PASTE,	//!< 貼り付け予定のノードを貼り付け候補から取り除く
}
