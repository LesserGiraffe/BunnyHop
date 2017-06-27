package pflab.bunnyHop.control;

import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.view.VoidNodeView;

/**
 * VoidNode のコントローラ
 * @author K.Koike
 */
public class VoidNodeController extends BhNodeController {

	private final VoidNode model;	//!< 管理するモデル
	private final VoidNodeView view;	//!< 管理するビュー

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public VoidNodeController(VoidNode model, VoidNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		view.getEventManager().setOnMousePressedHandler(mouseEvent -> {
			mouseEvent.consume();
		});
		view.getEventManager().setOnMouseDraggedHandler(null);
		view.getEventManager().setOnDragDetectedHandler(null);
		view.getEventManager().setOnMouseReleasedHandler(null);
	}
}








