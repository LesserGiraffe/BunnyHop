package pflab.bunnyHop.control;

import javafx.application.Platform;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.view.TextFieldNodeView;

/**
 * TextNodeとTextFieldNodeViewのコントローラ
 * @author K.Koike
 */
public class TextFieldNodeController extends BhNodeController {

	private final TextNode model;	//!< 管理するモデル
	private final TextFieldNodeView view;	//!< 管理するビュー

	public TextFieldNodeController(TextNode model, TextFieldNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		if (model.isImitationNode())
			view.setEditable(false);
		setTextChangeHandler(model, view);
		view.setCreateImitHandler(model);
	}

	/**
	 * TextNodeView に対して文字列変更時のハンドラを登録する
	 * @param model TextNodeView に対応する model
	 * @param view イベントハンドラを登録するview
	 * */
	static public void setTextChangeHandler(TextNode model, TextFieldNodeView view) {

		view.setTextChangeListener(model::isTextAcceptable);

		view.setObservableListener((observable, oldValue, newValue) -> {
			if (!newValue) {	//テキストフィールドからカーソルが外れたとき
				String currentGUIText = view.getText();
				boolean isValidFormat = model.isTextAcceptable(view.getText());
				if (isValidFormat) {	//正しいフォーマットの文字列が入力されていた場合
					model.setText(currentGUIText);	//model の文字列をTextField のものに変更する
					model.imitateText();
				}
				else {
					view.setText(model.getText());	//view の文字列を変更前の文字列に戻す
				}
			}
		});
		
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
				boolean editable = view.getEditable();
				view.setEditable(true);
				view.setText(data.text);
				view.setEditable(editable);
				model.setText(data.text);
				break;
			
			default:
				return super.receiveMsg(msg, data);
		}
		return null;
	}
}
