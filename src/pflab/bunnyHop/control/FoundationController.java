package pflab.bunnyHop.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNodeCategoryList;
import pflab.bunnyHop.model.WorkspaceSet;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * GUIの基底部分のコントローラ
 * @author K.Koike
 */
public class FoundationController{
	
	//View
	@FXML private Separator leftSeparator;
	
	//Controller
	@FXML private UserOperationController userOperationController;
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
		userOperationController.init(
			wss,
			workspaceSetController.getTabPane(), 
			nodeCategoryListController.getView().getSelectionViewList());
		
		MsgTransporter.instance().setSenderAndReceiver(wss, workspaceSetController, new UserOperationCommand());
		MsgTransporter.instance().setSenderAndReceiver(nodeCategoryList, nodeCategoryListController, new UserOperationCommand());
		setSeparatorEventHandlers();
	}
	
	private void setSeparatorEventHandlers() {
		DoubleProperty categoryViewWidth = new SimpleDoubleProperty();
		DoubleProperty mousePressedX = new SimpleDoubleProperty();
		
		leftSeparator.setOnMouseEntered(event -> {
			leftSeparator.setCursor(Cursor.H_RESIZE);
		});
		
		leftSeparator.setOnMouseExited(event -> {
			leftSeparator.setCursor(Cursor.DEFAULT);
		});
		
		leftSeparator.setOnMousePressed(event -> {
			categoryViewWidth.set(nodeCategoryListController.getCategoryListViewBase().getWidth());
			mousePressedX.set(event.getSceneX());
			event.consume();
		});
		
		leftSeparator.setOnMouseDragged(event -> {
			Region view = nodeCategoryListController.getCategoryListViewBase();
			double newWidth = event.getSceneX() - mousePressedX.get() + categoryViewWidth.get();
			newWidth = Math.min(newWidth, view.getScene().getWidth() / 2);
			newWidth = Math.max(newWidth, 1);
			view.setPrefWidth(newWidth);
			event.consume();
		});
	}
}
