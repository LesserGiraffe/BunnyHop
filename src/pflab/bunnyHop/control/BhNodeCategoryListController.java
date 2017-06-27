package pflab.bunnyHop.control;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgReceiver;
import pflab.bunnyHop.model.BhNodeCategoryList;
import pflab.bunnyHop.view.BhNodeCategoryListView;

/**
 * BhNode のカテゴリ選択画面のController
 * @author K.Koike
 * */
public class BhNodeCategoryListController implements MsgReceiver {

	@FXML private ScrollPane nodeCategoryListViewBase;
	@FXML private TreeView<BhNodeCategoryListView.BhNodeCategory> categoryTree;
	private BhNodeCategoryList model;
	private BhNodeCategoryListView view;
	
	/**
	 * モデルとイベントハンドラの登録を行う
	 * @param categoryList ノードカテゴリリストのモデル
	 */
	public void init(BhNodeCategoryList categoryList) {
		model = categoryList;
		view = new BhNodeCategoryListView(categoryTree);
		nodeCategoryListViewBase.setMinWidth(Region.USE_PREF_SIZE);
	}
	
	/**
	 * カテゴリリスト部分の基底GUI部品を返す
	 * @return カテゴリリスト部分の基底GUI部品
	 */
	public ScrollPane getCategoryListViewBase() {
		return nodeCategoryListViewBase;
	}
	
	/**
	 * カテゴリリストのビューを返す
	 * @return カテゴリリストのビュー
	 */
	public BhNodeCategoryListView getView() {
		return view;
	}
			
	@Override
	public MsgData receiveMsg(BhMsg msg, MsgData data) {
		switch (msg) {
		case BUILD_NODE_CATEGORY_LIST_VIEW:
			view.buildCategoryList(model.getRootNode());
			break;
		
		case ADD_NODE_SELECTION_PANELS:
			return new MsgData(view.getSelectionViewList());

		case HIDE_NODE_SELECTION_PANEL:
			view.hideAll();
			break;

		default :
			MsgPrinter.instance.MsgForDebug("BhNodeCategoryListController.receiveMsg error msg");
			assert false;
		}
		return null;
	}
}
