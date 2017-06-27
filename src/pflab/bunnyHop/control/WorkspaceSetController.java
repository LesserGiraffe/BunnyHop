package pflab.bunnyHop.control;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgReceiver;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.model.WorkspaceSet;
import pflab.bunnyHop.view.BhNodeSelectionView;
import pflab.bunnyHop.view.WorkspaceView;
import javafx.scene.layout.VBox;

/**
 * ワークスペースセットのコントローラ + ビュークラス
 * @author K.Koike
 * */
public class WorkspaceSetController implements MsgReceiver {

	private WorkspaceSet model;
	@FXML private VBox workspaceSetViewBase;
	@FXML private StackPane workspaceSetStackPane;
	@FXML private TabPane workspaceSetTab;	//!< ワークスペース表示タブ
	@FXML private Separator bottomSeparator;
	@FXML private TextArea bottomMsgArea;
	@FXML private ImageView openedTrashboxIV;
	@FXML private ImageView closedTrashboxIV;
	private final List<BhNodeSelectionView> bhNodeSelectionViewList = new ArrayList<>();
	
	/**
	 * モデルとイベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 */
	public void init(WorkspaceSet wss) {
		model = wss;
		setEventHandlers();
	}
	
	/**
	 * イベントハンドラを登録する
	 */
	private void setEventHandlers() {

		bottomMsgArea.setTextFormatter(
			new TextFormatter<>(change -> { 
				if (change.getControlNewText().length() > BhParams.numMaxBottomTextAreaChars) {
					if (change.getText().length() >= BhParams.numMaxBottomTextAreaChars) {	//追加分だけで最大文字数オーバー
						bottomMsgArea.clear();
						int newStrLen = change.getText().length();
						bottomMsgArea.setText(change.getText().substring(newStrLen - BhParams.numMaxBottomTextAreaChars, newStrLen));
					}
					else {
						bottomMsgArea.deleteText(0, change.getControlNewText().length() - BhParams.numMaxBottomTextAreaChars);
						bottomMsgArea.appendText(change.getText());
					}
					return null;
				}
				return change;}));
		
		setDragResizeEventHandlers();
		MsgPrinter.instance.setMainMsgArea(bottomMsgArea); //メインメッセージエリアの登録
	}
	
	/**
	 * マウスドラッグ時に起こるイベントハンドラを登録する
	 */
	private void setDragResizeEventHandlers() {
			
		DoubleProperty wssStackPaneHeight = new SimpleDoubleProperty();
		DoubleProperty mousePressedY = new SimpleDoubleProperty();
	
		bottomSeparator.setOnMouseEntered(event -> {
			bottomSeparator.setCursor(Cursor.V_RESIZE);
		});
		
		bottomSeparator.setOnMouseExited(event -> {
			bottomSeparator.setCursor(Cursor.DEFAULT);
		});
		
		bottomSeparator.setOnMousePressed(event -> {
			wssStackPaneHeight.set(workspaceSetStackPane.getHeight());
			mousePressedY.set(event.getSceneY());
			event.consume();
		});
		
		bottomSeparator.setOnMouseDragged(event -> {
			double newHeight = event.getSceneY() - mousePressedY.get() + wssStackPaneHeight.get();
			newHeight = Math.max(Util.rem, newHeight);
			if (event.getSceneY() < workspaceSetViewBase.getScene().getHeight() - Util.rem) {
				workspaceSetStackPane.setPrefHeight(newHeight);
				bottomMsgArea.setPrefHeight(bottomMsgArea.getHeight() - newHeight);
			}
			event.consume();
		});
		
		//ワークスペースセットの大きさ変更時にノード選択ビューの高さを再計算する
        workspaceSetTab.heightProperty().addListener(
            (observable, oldValue, newValue) -> {
				bhNodeSelectionViewList.forEach(
				//タブの大きさ分Y方向に移動するので, その分ノード選択ビューの高さを小さくする
				selectionVeiw -> {
					selectionVeiw.setMaxHeight(newValue.doubleValue() - selectionVeiw.getTranslateY());
				});
		});
		
		//ワークスペースセットの大きさ変更時にテキストエリアとワークスペースセットの大きさを更新する
		workspaceSetViewBase.heightProperty().addListener((obs, old, newVal) -> {
			workspaceSetStackPane.setPrefHeight(0.9 * newVal.doubleValue());
			bottomMsgArea.setPrefHeight(0.1 * newVal.doubleValue());
		});
	}
	
	/**
	 * ノード選択ビューを追加する
	 * @param nodeSelectionView 表示するノードテンプレート
	 * */
	private void addNodeSelectionView(BhNodeSelectionView nodeSelectionView) {
		workspaceSetStackPane.getChildren().add(nodeSelectionView);
		nodeSelectionView.toFront();				
		bhNodeSelectionViewList.add(nodeSelectionView);
		
		//タブの高さ分移動したときもノード選択ビューの高さを再計算する
		nodeSelectionView.translateYProperty().addListener((observable, oldValue, newValue) -> {
				nodeSelectionView.setMaxHeight(workspaceSetTab.getHeight() - newValue.doubleValue());
			});
	}
	
	/**
	 * ワークスペース表示用のタブペインを返す
	 * @return ワークスペース表示用のタブペイン
	 */
	public TabPane getTabPane() {
		return workspaceSetTab;
	}
	
	/**
	 * ゴミ箱を開閉する
	 * @param open ゴミ箱を開ける場合true
	 */
	private void openTrashBox(boolean open) {
		if (open) {
			openedTrashboxIV.setVisible(true);
			closedTrashboxIV.setVisible(false);
		}
		else {
			openedTrashboxIV.setVisible(false);
			closedTrashboxIV.setVisible(true);
		}
	}
	
	/**
	 * 引数で指定した位置がゴミ箱エリアにあるかどうか調べる
	 * @param sceneX シーン上でのX位置
	 * @param sceneY シーン上でのY位置
	 * @return 引数で指定した位置がゴミ箱エリアにある場合true
	 */
	private boolean isPointInTrashBoxArea(double sceneX, double sceneY) {
		
		Point2D localPos = closedTrashboxIV.sceneToLocal(sceneX, sceneY);
		return closedTrashboxIV.contains(localPos.getX(), localPos.getY());
	}

	@Override
	public MsgData receiveMsg(BhMsg msg, MsgData data){

		switch (msg) {

		case ADD_WORKSPACE:
			model.addWorkspace(data.workspace);
			workspaceSetTab.getTabs().add(data.workspaceView);
			workspaceSetTab.getSelectionModel().select(data.workspaceView);
			break;

		case DELETE_WORKSPACE:
			model.removeWorkspace(data.workspace);
			workspaceSetTab.getTabs().remove(data.workspaceView);
			break;
			
		case ADD_NODE_SELECTION_PANELS:
			data.nodeSelectionViewList.forEach(this::addNodeSelectionView);
			break;

		case GET_CURRENT_WORKSPACE:
			return new MsgData(getCurrentWorkspace());
		
		case IS_IN_TRASHBOX_AREA:
			return new MsgData(isPointInTrashBoxArea(data.doublePair._1, data.doublePair._2));
			
		case OPEN_TRAHBOX:
			openTrashBox(data.bool);
			break;
			
		default:
			MsgPrinter.instance.MsgForDebug("WorkspaceSetController.receiveMsg error message");
			assert false;
		}

		return null;
	}

	/**
	 * 現在選択中の Workspace を返す
	 * @return 現在選択中のWorkspace
	 * */
	private Workspace getCurrentWorkspace() {
		
		WorkspaceView newWorkspaceView = (WorkspaceView)workspaceSetTab.getSelectionModel().getSelectedItem();
		if (newWorkspaceView == null) {
			return null;
		}
		return newWorkspaceView.getWorkspace();
	}

	/**
	 * 現在選択中の Workspace を返す
	 * @return 現在選択中のWorkspace
	 * */
	public WorkspaceView getCurrentWorkspaceView() {
		return (WorkspaceView)workspaceSetTab.getSelectionModel().getSelectedItem();
	}
}












