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
package net.seapanda.bunnyhop.view.node;

import java.util.function.Function;

import javafx.beans.value.ChangeListener;

public interface TextInputNodeView {


	/**
	 * テキスト変更時のイベントハンドラを登録する
	 * @param checkFormatFunc 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
	 * */
	public void setTextChangeListener(Function<String, Boolean> checkFormatFunc);

	/**
	 * テキストフィールドのカーソルon/off時のイベントハンドラを登録する
	 * @param changeFocusFunc テキストフィールドのカーソルon/off時のイベントハンドラ
	 * */
	public void setObservableListener(ChangeListener<? super Boolean> changeFocusFunc);

	/**
	 * View に表示されたテキストを取得する
	 * */
	public String getText();

	/**
	 * View に表示するテキストを設定する
	 * */
	public void setText(String text);


	/**
	 * テキストフィールドが編集可能かどうかをセットする
	 * @param editable テキストフィールドが編集可能なときtrue
	 * */
	public void setEditable(boolean editable);

	/**
	 * テキストフィールドが編集可能かどうかチェックする
	 * @return テキストフィールドが編集可能な場合 true
	 * */
	public boolean getEditable();
}
