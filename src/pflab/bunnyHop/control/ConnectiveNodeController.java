package pflab.bunnyHop.control;

import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.view.BhNodeView;
import pflab.bunnyHop.view.ConnectiveNodeView;

/**
 * ConnectiveNode のコントローラ
 * @author K.Koike
 */
public class ConnectiveNodeController extends BhNodeController {

	private final ConnectiveNode model;	//!< 管理するモデル
	private final ConnectiveNodeView view;	//!< 管理するビュー

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public ConnectiveNodeController(ConnectiveNode model, ConnectiveNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		view.setCreateImitHandler(model);
	}
}











