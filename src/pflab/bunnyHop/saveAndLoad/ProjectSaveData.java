package pflab.bunnyHop.saveAndLoad;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import pflab.bunnyHop.ModelProcessor.NodeMVCBuilder;
import pflab.bunnyHop.common.Pair;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.control.WorkspaceController;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.WorkspaceView;

/**
 * 全ワークスペースの保存に必要なデータを保持するクラス
 * @author Koike
 */
public class ProjectSaveData implements Serializable{
	
	private final List<WorkspaceSaveData> workspaceSaveList;
		
	/**
	 * コンストラクタ
	 * @param workspaceList 保存するワークスペースのリスト
	 */
	public ProjectSaveData(Collection<Workspace> workspaceList) {		
		workspaceSaveList = workspaceList.stream().map(workspace -> {
			WorkspaceSaveData wsSaveData = this.new WorkspaceSaveData(workspace);
			return wsSaveData;
		})
		.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * ワークスペースのリストをワークスペースセットに追加できる状態にして返す
	 * @return ロードしたワークスペースのリスト
	 */
	public List<Workspace> load() {
		
		UserOperationCommand dummyCmd = new UserOperationCommand();
		workspaceSaveList.forEach(wsSaveData -> wsSaveData.initBhNodes(dummyCmd));
		return workspaceSaveList.stream().map(wsSaveData -> {
			return wsSaveData.load(dummyCmd);
		})
		.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * ワークスペースの保存に必要なデータを保持するクラス
	 */
	private class WorkspaceSaveData implements Serializable {
		
		private final Workspace ws;	//!< 保存するワークスペース
		private final Point2D workspaceSize;	//!< ワークスペースのサイズ
		private final List<RootNodeSaveData> rootNodeSaveList;
		
		public WorkspaceSaveData(Workspace ws){
			this.ws = ws;
			Pair<Double, Double> wsSize = MsgTransporter.instance().sendMessage(BhMsg.GET_WORKSPACE_SIZE, ws).doublePair;
			workspaceSize = new Point2D(wsSize._1, wsSize._2);
			rootNodeSaveList = ws.getRootNodeList().stream().map(rootNode -> {
				return this.new RootNodeSaveData(rootNode);
			})
			.collect(Collectors.toCollection(ArrayList::new));
		}
		
		/**
		 * ワークスペース以下の全てのBhNode を初期化する
		 * @param dummyCmd ダミーのundo用コマンドオブジェクト
		 */
		public void initBhNodes(UserOperationCommand dummyCmd) {
			rootNodeSaveList.forEach(nodeSaveData -> nodeSaveData.initBhNodes(dummyCmd));
		}
		
		/**
		 * ワークスペースをワークスペースセットに追加できる状態にして返す
		 * @param dummyCmd ダミーのundo用コマンドオブジェクト
		 * @return ロードしたワークスペース
		 */
		public Workspace load(UserOperationCommand dummyCmd) {
			
			WorkspaceView wsView = new WorkspaceView(ws);
			wsView.init(workspaceSize.x, workspaceSize.y);
			WorkspaceController wsController = new WorkspaceController(ws, wsView);
			MsgTransporter.instance().setSenderAndReceiver(ws, wsController, dummyCmd);
			ws.initForLoad();
			rootNodeSaveList.forEach(nodeSaveData -> {
				Pair<BhNode, Point2D> rootNode_pos = nodeSaveData.getBhNodeAndPos();
				Point2D pos = rootNode_pos._2;
				BhNode rootNode = rootNode_pos._1;
				BhNodeHandler.instance.addRootNode(ws, rootNode, pos.x, pos.y, dummyCmd);
			});
			return ws;
		}
		
		private class RootNodeSaveData implements Serializable {
			private final BhNode rootNode;	//!<保存するルートノード
			private final Point2D nodePos;	//!< ルートノードの位置
			
			RootNodeSaveData(BhNode rootNode) {
				this.rootNode = rootNode;				
				Pair<Double, Double> pos = MsgTransporter.instance().sendMessage(BhMsg.GET_POS_ON_WORKSPACE, rootNode).doublePair;
				nodePos = new Point2D(pos._1, pos._2);
			}
			
			/**
			 * BhNode を初期化する
			 * @param dummyCmd ダミーのundo用コマンドオブジェクト
			 */
			public void initBhNodes(UserOperationCommand dummyCmd) {
				NodeMVCBuilder builder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, dummyCmd);
				rootNode.accept(builder);	//MVC構築				
			}
			
			/**
			 * BhNodeとその位置を返す
			 * @return ロードしたBhNodeとその位置のペア
			 */
			public Pair<BhNode, Point2D> getBhNodeAndPos() {
				return new Pair<>(rootNode, nodePos);
			}
		}
	}
}
