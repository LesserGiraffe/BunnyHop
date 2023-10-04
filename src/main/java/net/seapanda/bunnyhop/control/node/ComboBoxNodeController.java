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
package net.seapanda.bunnyhop.control.node;

import java.util.Objects;

import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.part.SelectableItem;

/**
 * TextNode と ComboBoxNodeView のコントローラ
 * @author K.Koike
 */
public class ComboBoxNodeController extends BhNodeController {

	private final TextNode model;	//!< 管理するモデル
	private final ComboBoxNodeView view;	//!< 管理するビュー

	public ComboBoxNodeController(TextNode model, ComboBoxNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		setItemChangeHandler(model, view);
	}

	/**
	 * ComboBoxView のアイテム変更時のイベントハンドラを登録する
	 *@param model ComboBoxView に対応する model
	 * @param view イベントハンドラを登録するview
	 */
	public static void setItemChangeHandler(TextNode model, ComboBoxNodeView view) {

		view.setTextChangeListener(
			(observable, oldVal, newVal) -> checkAndSetContent(model, view, oldVal, newVal));

		view.getItemByModelText(model.getText())
		.ifPresentOrElse(
			item -> view.setItem(item),
			() -> model.setText(view.getItem().getModelText()));
	}

	/**
	 * 新しく選択されたコンボボックスのアイテムが適切かどうかを調べて, 適切ならビューとモデルに設定する.
	 */
	private static void checkAndSetContent(
		TextNode model, ComboBoxNodeView view, SelectableItem oldItem, SelectableItem newItem) {

		if (Objects.equals(newItem.getModelText(), model.getText()))
			return;

		ModelExclusiveControl.INSTANCE.lockForModification();
		try {

			if (model.isTextAcceptable(newItem.getModelText())) {
				// model の文字列を ComboBox の選択アイテムに対応したものにする
				model.setText(newItem.getModelText());
				model.getImitNodesToImitateContents();
			}
			else {
				view.setItem(oldItem);
			}
		}
		finally {
			ModelExclusiveControl.INSTANCE.unlockForModification();
		}
	}

	/**
	 * 受信したメッセージを処理する
	 * @param msg メッセージの種類
	 * @param data メッセージの種類に応じて処理するデータ
	 * @return メッセージを処理した結果返すデータ
	 */
	@Override
	public MsgData processMsg(BhMsg msg, MsgData data) {

		switch (msg) {
			case GET_VIEW_TEXT:
				return new MsgData(view.getItem().getViewText());

			default:
				return super.processMsg(msg, data);
		}
	}
}
























