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
package pflab.bunnyhop.saveandload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import pflab.bunnyhop.modelprocessor.NodeMVCBuilder;
import pflab.bunnyhop.common.Pair;
import pflab.bunnyhop.common.Point2D;
import pflab.bunnyhop.control.WorkspaceController;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.modelhandler.BhNodeHandler;
import pflab.bunnyhop.modelprocessor.TextImitationPrompter;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.view.WorkspaceView;

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
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return ロードしたワークスペースのリスト
	 */
	public List<Workspace> load(UserOperationCommand userOpeCmd) {
		
		workspaceSaveList.forEach(wsSaveData -> wsSaveData.initBhNodes());
		return workspaceSaveList.stream().map(wsSaveData -> {
			return wsSaveData.load(userOpeCmd);
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
			Pair<Double, Double> wsSize = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_WORKSPACE_SIZE, ws).doublePair;
			workspaceSize = new Point2D(wsSize._1, wsSize._2);
			rootNodeSaveList = ws.getRootNodeList().stream().map(rootNode -> {
				return this.new RootNodeSaveData(rootNode);
			})
			.collect(Collectors.toCollection(ArrayList::new));
		}
		
		/**
		 * ワークスペース以下の全てのBhNode を初期化する
		 * @param userOpeCmd undo用コマンドオブジェクト
		 */
		public void initBhNodes() {
			rootNodeSaveList.forEach(nodeSaveData -> nodeSaveData.createMVC());
			rootNodeSaveList.forEach(nodeSaveData -> nodeSaveData.imitOrgNode());
		}
		
		/**
		 * ワークスペースをワークスペースセットに追加できる状態にして返す
		 * @param userOpeCmd undo用コマンドオブジェクト
		 * @return ロードしたワークスペース
		 */
		public Workspace load(UserOperationCommand userOpeCmd) {
			
			WorkspaceView wsView = new WorkspaceView(ws);
			wsView.init(workspaceSize.x, workspaceSize.y);
			WorkspaceController wsController = new WorkspaceController(ws, wsView);
			ws.setMsgProcessor(wsController);
			ws.initForLoad();
			rootNodeSaveList.forEach(nodeSaveData -> {
				Pair<BhNode, Point2D> rootNode_pos = nodeSaveData.getBhNodeAndPos();
				Point2D pos = rootNode_pos._2;
				BhNode rootNode = rootNode_pos._1;
				BhNodeHandler.INSTANCE.addRootNode(ws, rootNode, pos.x, pos.y, userOpeCmd);
			});
			return ws;
		}
		
		private class RootNodeSaveData implements Serializable {
			private final BhNode rootNode;	//!<保存するルートノード
			private final Point2D nodePos;	//!< ルートノードの位置
			
			RootNodeSaveData(BhNode rootNode) {
				this.rootNode = rootNode;				
				Pair<Double, Double> pos = MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_POS_ON_WORKSPACE, rootNode).doublePair;
				nodePos = new Point2D(pos._1, pos._2);
			}
			
			/**
			 * MVC構築する
			 */
			public void createMVC() {
				rootNode.accept(new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default));	//MVC構築
			}
			
			/**
			 * イミテーションノードにオリジナルノードを模倣させる.
			 */
			public void imitOrgNode() {
				rootNode.accept(new TextImitationPrompter());
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
