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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextInputControl;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.model.node.TextNode;

public abstract class TextInputNodeView extends BhNodeView {

	protected TextInputNodeView(TextNode model, BhNodeViewStyle viewStyle) {
		super(viewStyle, model);
	}

	/**
	 * テキスト入力用GUIコンポーネントを取得する.
	 * */
	protected abstract TextInputControl getTextInputControl();

	/**
	 * テキスト変更時のイベントハンドラを登録する
	 * @param checkFormatFunc 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
	 * */
	public abstract void setTextChangeListener(Function<String, Boolean> checkFormatFunc);

	/**
	 * テキストフィールドのカーソルon/off時のイベントハンドラを登録する
	 * @param changeFocusFunc テキストフィールドのカーソルon/off時のイベントハンドラ
	 * */
	public final void setObservableListener(ChangeListener<? super Boolean> changeFocusFunc) {
		getTextInputControl().focusedProperty().addListener(changeFocusFunc);
	}

	/**
	 * テキストを整形する関数を登録する
	 * @param formatterFunc テキストを整形する関数. <br>
	 * formatterFunc の第1引数 -> 整形対象の全文字列 <br>
	 * formatterFunc の第2引数 -> 前回整形したテキストから新たに追加された文字列 <br>
	 * formatterFunc の戻り値 <br>
	 * &nbsp;&nbsp; _1 -> テキスト全体を整形した場合 true. 追加分だけ整形した場合 false.<br>
	 * &nbsp;&nbsp; _2 -> 整形したテキスト
	 * */
	public final void setTextFormatHandler(BiFunction<String, String, Pair<Boolean, String>> formatterFunc) {

		TextInputControl control = getTextInputControl();

		UnaryOperator<Change> filter = change -> {

			Pair<Boolean, String> result = formatterFunc.apply(change.getControlNewText(), change.getText());
			boolean isEntireTextFormatted = result._1;
			String formattedText = result._2;
			if (isEntireTextFormatted)
				change.setRange(0, control.getLength());

			change.setText(formattedText);
		    return change;
		};
		control.setTextFormatter(new TextFormatter<Object>(filter));
	}

	/**
	 * View に表示されたテキストを取得する
	 * */
	public final String getText() {
		return getTextInputControl().getText();
	}

	/**
	 * View に表示するテキストを設定する
	 * */
	public final void setText(String text) {
		getTextInputControl().setText(text);
	}


	/**
	 * テキストフィールドが編集可能かどうかをセットする
	 * @param editable テキストフィールドが編集可能なときtrue
	 * */
	public final void setEditable(boolean editable) {
		getTextInputControl().setEditable(editable);
	}

	/**
	 * テキストフィールドが編集可能かどうかチェックする
	 * @return テキストフィールドが編集可能な場合 true
	 * */
	public final boolean getEditable() {
		return getTextInputControl().editableProperty().getValue();
	}
}
