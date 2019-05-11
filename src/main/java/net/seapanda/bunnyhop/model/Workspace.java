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
package net.seapanda.bunnyhop.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.VersionInfo;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ワークスペースクラス
 * @author K.Koike
 * */
public class Workspace implements MsgReceptionWindow, Serializable {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	private final Set<BhNode> rootNodeList = new HashSet<>();	//!< ワークスペースのルートノードのリスト
	private final Set<BhNode> selectedList = new LinkedHashSet<BhNode>();	//!< 選択中のノード. 挿入順を保持したいのでLinkedHashSetを使う
	private BhNode moveCandidate = null;	//!< 移動候補のノード
	private final String workspaceName;	//!< ワークスペース名
	transient private WorkspaceSet workspaceSet;	//!< このワークスペースを持つワークスペースセット
	transient private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト

	/**
	 * コンストラクタ
	 * @param workspaceName ワークスペース名
	 */
	public Workspace(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	/**
	 *ルートノードを追加する
	 * @param node 追加するBhノード
	 * */
	public void addRootNode(BhNode node) {
		rootNodeList.add(node);
	}

	/**
	 * ルートノードを削除する
	 * @param node 削除するノード
	 * */
	public void removeRootNode(BhNode node) {
		rootNodeList.remove(node);
	}

	/**
	 * このワークスペースを持つワークスペースセットをセットする
	 * @param wss このワークスペースを持つワークスペースセット
	 */
	public void setWorkspaceSet(WorkspaceSet wss) {
		workspaceSet = wss;
	}

	/**
	 * このワークスペースを持つワークスペースセットを返す
	 * @return このワークスペースを持つワークスペースセット
	 */
	public WorkspaceSet getWorkspaceSet() {
		return workspaceSet;
	}

	/**
	 * ロードのための初期化処理をする
	 */
	public void initForLoad() {
		rootNodeList.clear();
		selectedList.clear();
	}

	/**
	 * 引数で指定したノードをルートノードとして持っているかどうかチェックする
	 * @param node WS直下のルートノードかどうかを調べるノード
	 * @return 引数で指定したノードをルートノードとして持っている場合 true
	 */
	public boolean containsAsRoot(BhNode node) {
		return rootNodeList.contains(node);
	}

	/**
	 * ワークスペース内のルートBhNode の集合を返す
	 * @return ワークスペース内のルートBhNode の集合
	 * */
	public Collection<BhNode> getRootNodeList() {
		return rootNodeList;
	}

	/**
	 * 選択されたノードをセットする. このワークスペースの選択済みのノードは全て非選択になる.
	 * @param selected 新たに選択されたノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setSelectedNode(BhNode selected, UserOperationCommand userOpeCmd) {

		// 同じノードをクリックしたときにundoスタックにコマンドが積まれるのを避ける
		if ((selectedList.size() == 1) && selectedList.contains(selected))
			return;

		clearSelectedNodeList(userOpeCmd);
		addSelectedNode(selected, userOpeCmd);
	}

	/**
	 * 選択されたノードを選択済みリストに追加する
	 * @param nodeToAdd 追加されるノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void addSelectedNode(BhNode nodeToAdd, UserOperationCommand userOpeCmd) {

		if (selectedList.contains(nodeToAdd))
			return;

		MsgTransporter.INSTANCE.sendMessage(
			BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
			new MsgData(true, BhParams.CSS.PSEUDO_SELECTED),
			nodeToAdd);
		selectedList.add(nodeToAdd);
		MsgService.INSTANCE.updateMultiNodeShifter(nodeToAdd, this);

		if (nodeToAdd instanceof Imitatable) {
			List<Imitatable> imitationList = ((Imitatable)nodeToAdd).getImitationManager().getImitationList();
			imitationList.forEach(imitation -> {
				MsgTransporter.INSTANCE.sendMessage(
					BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
					new MsgData(true, BhParams.CSS.PSEUDO_HIGHLIGHT_IMIT),
					imitation);
			});
		}
		userOpeCmd.pushCmdOfAddSelectedNode(this, nodeToAdd);
	}

	/**
	 * 移動候補のノードをセットする.
	 * @param moveCandidate 移動候補のノード
	 * */
	public void setMoveCandidateNode(BhNode moveCandidate) {

		if (this.moveCandidate != null) {
			MsgTransporter.INSTANCE.sendMessage(
				BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
				new MsgData(false, BhParams.CSS.PSEUDO_MOVE),
				this.moveCandidate);
		}
		this.moveCandidate = moveCandidate;
		if (this.moveCandidate != null) {
			MsgTransporter.INSTANCE.sendMessage(
				BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
				new MsgData(true, BhParams.CSS.PSEUDO_MOVE),
				this.moveCandidate);
		}
	}

	/**
	 * 選択中のBhNodeのリストのコピーを返す
	 * @return 選択中のBhNodeのリスト
	 */
	public Set<BhNode> getSelectedNodeList() {
		return Collections.unmodifiableSet(selectedList);
	}

	/**
	 * 引数で指定したノードを選択済みリストから削除する
	 * @param nodeToRemove 選択済みリストから削除するBhNode
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void removeSelectedNode(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {

		MsgTransporter.INSTANCE.sendMessage(
			BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
			new MsgData(false, BhParams.CSS.PSEUDO_SELECTED),
			nodeToRemove);
		selectedList.remove(nodeToRemove);
		MsgService.INSTANCE.updateMultiNodeShifter(nodeToRemove, this);
		if (nodeToRemove instanceof Imitatable) {
			List<Imitatable> imitationList = ((Imitatable)nodeToRemove).getImitationManager().getImitationList();
			imitationList.forEach(imitation -> {
				MsgTransporter.INSTANCE.sendMessage(
					BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
					new MsgData(false, BhParams.CSS.PSEUDO_HIGHLIGHT_IMIT),
					imitation);
			});
		}
		userOpeCmd.pushCmdOfRemoveSelectedNode(this, nodeToRemove);
	}

	/**
	 * 選択中のノードをすべて非選択にする
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void clearSelectedNodeList(UserOperationCommand userOpeCmd) {
		BhNode[] nodesToDeselect = selectedList.toArray(new BhNode[selectedList.size()]);
		for (BhNode node : nodesToDeselect) {
			removeSelectedNode(node, userOpeCmd);
		}
	}

	/**
	 * ワークスペース名を取得する
	 * @return ワークスペース名
	 */
	public String getWorkspaceName() {
		return workspaceName;
	}

	@Override
	public void setMsgProcessor(MsgProcessor processor) {
		msgProcessor = processor;
	}

	@Override
	public MsgData passMsg(BhMsg msg, MsgData data) {
		return msgProcessor.processMsg(msg, data);
	}
}














