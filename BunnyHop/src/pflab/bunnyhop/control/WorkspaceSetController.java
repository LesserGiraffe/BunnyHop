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

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.model.WorkspaceSet;
import pflab.bunnyhop.undo.UserOpeCmdManager;
import pflab.bunnyhop.view.BhNodeSelectionView;
import pflab.bunnyhop.view.WorkspaceView;
import pflab.bunnyhop.message.MsgProcessor;

/**
 * ワークスペースセットのコントローラ + ビュークラス
 * @author K.Koike
 * */
public class WorkspaceSetController implements MsgProcessor {

	private WorkspaceSet model;
	@FXML private SplitPane workspaceSetViewBase;
	@FXML private StackPane workspaceSetStackPane;
	@FXML private TabPane workspaceSetTab;	//!< ワークスペース表示タブ
	@FXML private TextArea mainMsgArea;
	@FXML private ImageView openedTrashboxIV;
	@FXML private ImageView closedTrashboxIV;
	private final List<BhNodeSelectionView> bhNodeSelectionViewList = new ArrayList<>();
	private final UserOpeCmdManager userOpeCmdManager = new UserOpeCmdManager();
	
	/**
	 * モデルとイベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 */
	public void init(WorkspaceSet wss) {
		model = wss;
		setEventHandlers();
		workspaceSetViewBase.setDividerPositions(BhParams.DEFAULT_VERTICAL_DIV_POS);
		MsgPrinter.instance.setMainMsgArea(mainMsgArea); //メインメッセージエリアの登録
	}
	
	/**
	 * イベントハンドラを登録する
	 */
	private void setEventHandlers() {
		
		mainMsgArea.textProperty().addListener((observable, oldVal, newVal)-> {
			if (newVal.length() > BhParams.MAX_MAIN_MSG_AREA_CHARS) {
				int numDeleteChars = newVal.length() - BhParams.MAX_MAIN_MSG_AREA_CHARS;
				mainMsgArea.deleteText(0, numDeleteChars);
			}
			mainMsgArea.setScrollTop(Double.MAX_VALUE);
		});

		mainMsgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
			if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0)
				mainMsgArea.setScrollTop(Double.MAX_VALUE);
		});
		
		setResizeEventHandlers();
	}
	
	/**
	 * ワークスペースリサイズ時におこるイベントハンドラを登録する
	 */
	private void setResizeEventHandlers() {
		
		//ワークスペースセットの大きさ変更時にノード選択ビューの高さを再計算する
        workspaceSetTab.heightProperty().addListener(
            (observable, oldValue, newValue) -> {
				bhNodeSelectionViewList.forEach(
				//タブの大きさ分Y方向に移動するので, その分ノード選択ビューの高さを小さくする
				selectionVeiw -> {
					selectionVeiw.setMaxHeight(newValue.doubleValue() - selectionVeiw.getTranslateY());
				});
		});
		
//		//ワークスペースセットの大きさ変更時にテキストエリアとワークスペースセットの大きさを更新する
//		workspaceSetViewBase.heightProperty().addListener((obs, old, newVal) -> {
//			workspaceSetStackPane.setPrefHeight(0.9 * newVal.doubleValue());
//			bottomMsgArea.setPrefHeight(0.1 * newVal.doubleValue());
//		});
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
	public MsgData processMsg(BhMsg msg, MsgData data){

		switch (msg) {

		case ADD_WORKSPACE:
			model.addWorkspace(data.workspace);
			workspaceSetTab.getTabs().add(data.workspaceView);
			workspaceSetTab.getSelectionModel().select(data.workspaceView);
			data.userOpeCmd.pushCmdOfAddWorkspace(data.workspace, data.workspaceView, model);
			break;

		case DELETE_WORKSPACE:
			model.removeWorkspace(data.workspace);
			workspaceSetTab.getTabs().remove(data.workspaceView);
			data.userOpeCmd.pushCmdOfDeleteWorkspace(data.workspace, data.workspaceView, model);
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
			
		case UNDO:
			userOpeCmdManager.undo();
			break;
				
		case REDO:
			userOpeCmdManager.redo();
			break;
		
		case PUSH_USER_OPE_CMD:
			if (data.userOpeCmd.getNumSubOpe() > 0) {
				userOpeCmdManager.pushUndoCommand(data.userOpeCmd);
			}
			break;

			
		default:
			MsgPrinter.instance.ErrMsgForDebug(WorkspaceSetController.class.getSimpleName() +  ".receiveMsg unknown msg  " + msg);
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












