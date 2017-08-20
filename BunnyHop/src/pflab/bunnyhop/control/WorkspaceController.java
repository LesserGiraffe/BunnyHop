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

import java.lang.reflect.Field;
import pflab.bunnyhop.quadtree.QuadTreeManager;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.Point2D;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.root.BunnyHop;
import pflab.bunnyhop.modelhandler.DelayedDeleter;
import pflab.bunnyhop.undo.UserOpeCmdManager;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.view.WorkspaceView;
import pflab.bunnyhop.message.MsgProcessor;


/**
 * ワークスペースとそれに関連するビューのコントローラ
 * @author K.Koike
 */
public class WorkspaceController implements MsgProcessor {

	private Workspace model; // 操作対象のモデル
	private WorkspaceView view;

	/**
	 * コンストラクタ
	 * @param model コントローラが操作するモデル
	 * @param view コントローラが操作するビュー
	 */
	public WorkspaceController(Workspace model, WorkspaceView view) {
		this.model = model;
		this.view = view;

		this.view.setOnMousePressedEvent(
			event -> {
				UserOperationCommand userOpeCmd = new UserOperationCommand();
				BunnyHop.instance().hideTemplatePanel();
				model.clearSelectedNodeList(userOpeCmd);
				BunnyHop.instance().pushUserOpeCmd(userOpeCmd);
			});
	}

	/**
	 * メッセージ受信
	 * @param msg メッセージの種類
	 * @param data メッセージの種類に応じて処理するもの
	 * */
	@Override
	public MsgData processMsg(BhMsg msg, MsgData data) {

		switch (msg) {
			
			case ADD_ROOT_NODE:
				model.addRootNode(data.node);
				view.addNodeView(data.nodeView);
				break;

			case REMOVE_ROOT_NODE:
				model.removeRootNode(data.node);
				view.removeNodeView(data.nodeView);
				break;

			case ADD_QT_RECTANGLE:
				view.addRectangleToQTSpace(data.nodeView);
				break;

			case CHANGE_WORKSPACE_VIEW_SIZE:
				view.changeWorkspaceViewSize(data.bool);
				break;

			case SCENE_TO_WORKSPACE:
				javafx.geometry.Point2D pos = view.sceneToWorkspace(data.doublePair._1, data.doublePair._2);
				return new MsgData(pos.getX(), pos.getY());
				
			case ZOOM:
				view.zoom(data.bool);
				break;
			
			case GET_WORKSPACE_SIZE:
				Point2D size = view.getWorkspaceSize();
				return new MsgData(size.x, size.y);
			
			case ADD_WORKSPACE:
				return new MsgData(model, view, data.userOpeCmd);
				
			case DELETE_WORKSPACE:
				model.deleteNodes(model.getRootNodeList(), data.userOpeCmd);
				return new MsgData(model, view, data.userOpeCmd);
				
			default:
				MsgPrinter.instance.ErrMsgForDebug(WorkspaceController.class.getSimpleName() + ".receiveMsg unknown msg " + msg);
				assert false;
		}

		return null;
	};

	//デバッグ用
	private void printDebugInfo() {
	
		//4分木登録ノード数表示
		Class<WorkspaceView> c = WorkspaceView.class;
		Field f = null;
		try {
			f = c.getDeclaredField("quadTreeMngForConnector");
			f.setAccessible(true);
			QuadTreeManager quadTreeMngForConnector = (QuadTreeManager)f.get(view);
			MsgPrinter.instance.MsgForDebug("num of QuadTreeNodes " + quadTreeMngForConnector.calcRegisteredNodeNum());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MsgPrinter.instance.MsgForDebug("num of root nodes " + model.getRootNodeList().size());
		MsgPrinter.instance.MsgForDebug("num of deletion candidates " + DelayedDeleter.instance.getDeletionCadidateList());
		MsgPrinter.instance.MsgForDebug("num of selected nodes " + model.getSelectedNodeList().size());
	}
}












