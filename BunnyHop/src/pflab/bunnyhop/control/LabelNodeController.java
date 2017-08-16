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

import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.view.LabelNodeView;

/**
 * TextNodeとLabelNodeViewのコントローラ
 * @author K.Koike
 */
public class LabelNodeController extends BhNodeController {

	private final TextNode model;	//!< 管理するモデル
	private final LabelNodeView view;	//!< 管理するビュー

	public LabelNodeController(TextNode model, LabelNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		setInitStr(model, view);
		view.setCreateImitHandler(model);		
	}
	
	/**
	 * view に初期文字列をセットする
	 * @param model セット初期文字列を持つTextNode
	 * @param view 初期文字列をセットするLabelNodeView
	 */
	public static void setInitStr(TextNode model, LabelNodeView view) {
		String initText = model.getText();
		view.setText(initText + " ");	//初期文字列が空文字だったときのため
		view.setText(initText);
	}
	
	/**
	 * 受信したメッセージを処理する
	 * @param msg メッセージの種類
	 * @param data メッセージの種類に応じて処理するデータ
	 * @return メッセージを処理した結果返すデータ
	 * */
	@Override
	public MsgData receiveMsg(BhMsg msg, MsgData data) {
	
		switch (msg) {
			case IMITATE_TEXT:
				view.setText(data.text);
				model.setText(data.text);
				break;
			
			default:
				return super.receiveMsg(msg, data);
		}
		return null;
	}
}
