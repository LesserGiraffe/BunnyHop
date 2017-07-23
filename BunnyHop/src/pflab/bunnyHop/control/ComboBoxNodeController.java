package pflab.bunnyHop.control;

import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.view.ComboBoxNodeView;

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
