package pflab.bunnyHop.control;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import pflab.bunnyHop.QuadTree.QuadTreeManager;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgReceiver;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.root.BunnyHop;
import pflab.bunnyHop.modelHandler.DelayedDeleter;
import pflab.bunnyHop.undo.UserOpeCmdManager;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.WorkspaceView;


/**
 * ワークスペースとそれに関連するビューのコントローラ
 * @author K.Koike
 */
public class WorkspaceController implements MsgReceiver {

	private Workspace model; // 操作対象のモデル
	private WorkspaceView view;
	private final UserOpeCmdManager userOpeCmdManager = new UserOpeCmdManager();

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
				if (userOpeCmd.getNumSubOpe() > 0)
					userOpeCmdManager.pushUndoCommand(userOpeCmd);
				
				//printDebugInfo();
			});
	}

	/**
	 * メッセージ受信
	 * @param msg メッセージの種類
	 * @param data メッセージの種類に応じて処理するもの
	 * */
	@Override
	public MsgData receiveMsg(BhMsg msg, MsgData data) {

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
				
			case UNDO:
				userOpeCmdManager.undo();
				break;
				
			case REDO:
				userOpeCmdManager.redo();
				break;
				
			case ZOOM:
				view.zoom(data.bool);
				break;
			
			case GET_WORKSPACE_SIZE:
				Point2D size = view.getWorkspaceSize();
				return new MsgData(size.x, size.y);
			
			case PUSH_USER_OPE_CMD:
				if (data.userOpeCmd.getNumSubOpe() > 0) {
					userOpeCmdManager.pushUndoCommand(data.userOpeCmd);
				}
				break;
			
			case ADD_WORKSPACE:
				return new MsgData(model, view);
				
			case DELETE_WORKSPACE:
				model.deleteNodes(model.getRootNodeList());
				return new MsgData(model, view);
				
			default:
				MsgPrinter.instance.MsgForDebug("WorkspaceController.receiveMsg error msg " + msg);
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
		
		MsgPrinter.instance.MsgForDebug("num of send-recv pairs " + MsgTransporter.instance().getNumPair());
		MsgPrinter.instance.MsgForDebug("num of root nodes " + model.getRootNodeList().size());
		MsgPrinter.instance.MsgForDebug("num of deletion candidates " + DelayedDeleter.instance.getDeletionCadidateList());
		MsgPrinter.instance.MsgForDebug("num of selected nodes " + model.getSelectedNodeList().size());
	}
}












