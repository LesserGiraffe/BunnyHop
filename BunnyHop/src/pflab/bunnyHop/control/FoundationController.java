package pflab.bunnyHop.control;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNodeCategoryList;
import pflab.bunnyHop.model.WorkspaceSet;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * GUIの基底部分のコントローラ
 * @author K.Koike
 */
public class FoundationController {
	
	//View
	@FXML SplitPane horizontalSplitter;
	
	//Controller
	@FXML private BhBasicOperationController bhBasicOperationController;
	@FXML private WorkspaceSetController workspaceSetController;
	@FXML private BhNodeCategoryListController nodeCategoryListController;
		
	/**
	 * モデルとイベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 * @param nodeCategoryList ノードカテゴリリストのモデル
	 */
	public void init(WorkspaceSet wss, BhNodeCategoryList nodeCategoryList) {
		
		workspaceSetController.init(wss);
		nodeCategoryListController.init(nodeCategoryList);
		bhBasicOperationController.init(
			wss,
			workspaceSetController.getTabPane(), 
			nodeCategoryListController.getView());
		
		MsgTransporter.instance().setSenderAndReceiver(wss, workspaceSetController, new UserOperationCommand());
		MsgTransporter.instance().setSenderAndReceiver(nodeCategoryList, nodeCategoryListController, new UserOperationCommand());
	}
}
