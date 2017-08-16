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
package pflab.bunnyhop.control;

import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.view.ComboBoxNodeView;

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
		view.setCreateImitHandler(model);
		setItemChangeHandler(model, view);
	}
	
	/**
	 * ComboBoxView のアイテム変更時のイベントハンドラを登録する
	 *@param model ComboBoxView に対応する model
	 * @param view イベントハンドラを登録するview
	 */
	public static void setItemChangeHandler(TextNode model, ComboBoxNodeView view) {
		
		view.setTextChangeListener((observable, oldVal, newVal) -> {
			if (newVal.equals(model.getText())) {
				return;
			}
			
			if (model.isTextAcceptable(newVal)) {
				model.setText(newVal);	//model の文字列をComboBox のものにする
				model.imitateText();	//イミテーションのテキストを変える (イミテーションの View がtextFieldの場合のみ有効)	
			}
			else {
				view.setText(oldVal);
			}
		});
		
		view.setText(model.getText());
	}
}
