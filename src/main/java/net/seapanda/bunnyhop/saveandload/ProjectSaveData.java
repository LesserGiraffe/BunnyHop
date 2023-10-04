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
package net.seapanda.bunnyhop.saveandload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.control.workspace.WorkspaceController;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelprocessor.CallbackInvoker;
import net.seapanda.bunnyhop.modelprocessor.NodeMVCBuilder;
import net.seapanda.bunnyhop.modelprocessor.TemplateImitationCollector;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * 全ワークスペースの保存に必要なデータを保持するクラス
 * @author Koike
 */
public class ProjectSaveData implements Serializable{

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	private final List<WorkspaceSaveData> workspaceSaveList;

	/**
	 * コンストラクタ
	 * @param workspaceList 保存するワークスペースのリスト
	 */
	public ProjectSaveData(Collection<Workspace> workspaceList) {

		var imitToDelete = workspaceList.stream()
		.flatMap(ws -> ws.getRootNodeList().stream())
		.flatMap(rootNode -> TemplateImitationCollector.collect(rootNode).stream())
		.collect(Collectors.toCollection(ArrayList::new));
		BhNodeHandler.INSTANCE.deleteNodes(imitToDelete, new UserOperationCommand());

		workspaceSaveList = workspaceList.stream()
			.map(workspace -> {
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
		return workspaceSaveList.stream()
				.map(wsSaveData -> wsSaveData.load(userOpeCmd).orElse(null))
				.filter(ws -> ws != null)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * ワークスペースの保存に必要なデータを保持するクラス
	 */
	private class WorkspaceSaveData implements Serializable {

		private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
		private final Workspace ws;	//!< 保存するワークスペース
		private final Vec2D workspaceSize;	//!< ワークスペースのサイズ
		private final List<RootNodeSaveData> rootNodeSaveList;

		public WorkspaceSaveData(Workspace ws){
			this.ws = ws;
			Vec2D wsSize = MsgService.INSTANCE.getWorkspaceSize(ws);
			workspaceSize = new Vec2D(wsSize.x, wsSize.y);
			rootNodeSaveList = ws.getRootNodeList().stream()
				.map(rootNode -> this.new RootNodeSaveData(rootNode))
				.collect(Collectors.toCollection(ArrayList::new));
		}

		/**
		 * ワークスペース以下の全てのBhNode を初期化する
		 * @param userOpeCmd undo用コマンドオブジェクト
		 */
		public void initBhNodes() {

			rootNodeSaveList.forEach(rootNodeSaveData -> rootNodeSaveData.buildMVC());
			rootNodeSaveList.forEach(rootNodeSaveData -> rootNodeSaveData.imitOriginalNode());
			rootNodeSaveList.forEach(rootNodeSaveData -> rootNodeSaveData.giveNewSymbolID());
		}

		/**
		 * ワークスペースをワークスペースセットに追加できる状態にして返す
		 * @param userOpeCmd undo用コマンドオブジェクト
		 * @return ロードしたワークスペース. 失敗した場合 empty.
		 */
		public Optional<Workspace> load(UserOperationCommand userOpeCmd) {

			WorkspaceView wsView = new WorkspaceView(ws);
			wsView.init(workspaceSize.x, workspaceSize.y);

			WorkspaceController wsController;
			try {
				wsController = new WorkspaceController(ws, wsView, new MultiNodeShifterView());
			}
			catch (ViewInitializationException e) {
				MsgPrinter.INSTANCE.errMsgForDebug(getClass().getSimpleName() + ".load\n" + e);
				return Optional.empty();
			}
			ws.setMsgProcessor(wsController);
			ws.initForLoad();
			rootNodeSaveList.forEach(nodeSaveData -> {
				Pair<BhNode, Vec2D> rootNode_pos = nodeSaveData.getBhNodeAndPos();
				Vec2D pos = rootNode_pos._2;
				BhNode rootNode = rootNode_pos._1;
				BhNodeHandler.INSTANCE.addRootNode(ws, rootNode, pos.x, pos.y, userOpeCmd);
			});
			return Optional.of(ws);
		}

		private class RootNodeSaveData implements Serializable {

			private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
			private final BhNode rootNode;	//!<保存するルートノード
			private final Vec2D nodePos;	//!< ルートノードの位置

			RootNodeSaveData(BhNode rootNode) {
				this.rootNode = rootNode;
				Vec2D pos = MsgService.INSTANCE.getPosOnWS(rootNode);
				nodePos = new Vec2D(pos.x, pos.y);
			}

			/**
			 * MVCを構築する
			 */
			public void buildMVC() {
				NodeMVCBuilder.build(rootNode);
			}

			/**
			 * イミテーションノードにオリジナルノードを模倣させる.
			 */
			public void imitOriginalNode() {
				TextImitationPrompter.prompt(rootNode);
			}

			/**
			 * このオブジェクトが保持するルートノード以下の全ての SyntaxSymbol の SymbolID を新しいものにする
			 */
			public void giveNewSymbolID() {

				var callbacks = CallbackInvoker.newCallbackRegistry()
					.setForAllSyntaxSymbols(SyntaxSymbol::renewSymbolID);
				CallbackInvoker.invoke(callbacks, rootNode);
			}

			/**
			 * BhNodeとその位置を返す
			 * @return ロードしたBhNodeとその位置のペア
			 */
			public Pair<BhNode, Vec2D> getBhNodeAndPos() {
				return new Pair<>(rootNode, nodePos);
			}
		}
	}
}




















