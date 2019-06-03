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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.modelhandler.BhNodeHandler;
import net.seapanda.bunnyhop.modelhandler.DelayedDeleter;
import net.seapanda.bunnyhop.modelhandler.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.modelprocessor.NodeMVCBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.saveandload.ProjectSaveData;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ワークスペースの集合を保持、管理するクラス
 * @author K.Koike
 * */
public class WorkspaceSet implements MsgReceptionWindow {

	private final ObservableList<BhNode> readyToCopy = FXCollections.observableArrayList();	//!< コピー予定のノード
	private final ObservableList<BhNode> readyToCut = FXCollections.observableArrayList();	//!< カット予定のノード
	private final List<Workspace> workspaceList = new ArrayList<>();	//!< 全てのワークスペースのリスト
	private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト
	private int pastePosOffsetCount = -2; //!< ノードの貼り付け位置をずらすためのカウンタ

	public WorkspaceSet() {}

	/**
	 * ワークスペースを追加する
	 * @param workspace 追加されるワークスペース
	 * */
	public void addWorkspace(Workspace workspace) {
		workspaceList.add(workspace);
		workspace.setWorkspaceSet(this);
	}

	/**
	 * ワークスペースを取り除く
	 * @param workspace 取り除かれるワークスペース
	 */
	public void removeWorkspace(Workspace workspace) {
		workspaceList.remove(workspace);
		workspace.setWorkspaceSet(null);
	}

	/**
	 * コピー予定のBhNodeリストを追加する
	 * @param nodeList コピー予定のBhNodeリスト
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void addNodesToReadyToCopyList(Collection<BhNode> nodeList, UserOperationCommand userOpeCmd) {

		clearReadyToCutList(userOpeCmd);
		clearReadyToCopyList(userOpeCmd);
		readyToCopy.addAll(nodeList);
		userOpeCmd.pushCmdOfAddToList(readyToCopy, nodeList);
	}

	/**
	 * コピー予定のBhNodeリストをクリアする
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void clearReadyToCopyList(UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfRemoveFromList(readyToCopy, readyToCopy);
		readyToCopy.clear();
	}

	/**
	 * コピー予定のノードリストからノードを取り除く
	 * @param nodeToRemove 取り除くノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeNodeFromRedyToCopyList(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {

		if (readyToCopy.contains(nodeToRemove)) {
			userOpeCmd.pushCmdOfRemoveFromList(readyToCopy, nodeToRemove);
			readyToCopy.remove(nodeToRemove);
		}
	}

	/**
	 * カット予定のBhNodeリストを追加する
	 * @param nodeList カット予定のBhNodeリスト
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void addNodesToReadyToCutList(Collection<BhNode> nodeList, UserOperationCommand userOpeCmd) {

		clearReadyToCutList(userOpeCmd);
		clearReadyToCopyList(userOpeCmd);
		readyToCut.addAll(nodeList);
		userOpeCmd.pushCmdOfAddToList(readyToCut, nodeList);
	}

	/**
	 * カット予定のBhNodeリストをクリアする
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void clearReadyToCutList(UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfRemoveFromList(readyToCut, readyToCut);
		readyToCut.clear();
	}

	/**
	 * カット予定のノードリストからノードを取り除く
	 * @param nodeToRemove 取り除くノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeNodeFromRedyToCutList(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {

		if (readyToCut.contains(nodeToRemove)) {
			userOpeCmd.pushCmdOfRemoveFromList(readyToCut, nodeToRemove);
			readyToCut.remove(nodeToRemove);
		}
	}

	/**
	 * ペースト処理を行う
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 */
	public void paste(Workspace wsToPasteIn, Vec2D pasteBasePos) {

		UserOperationCommand userOpeCmd = new UserOperationCommand();
		copyAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
		cutAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
		DelayedDeleter.INSTANCE.deleteCandidates(userOpeCmd);
		SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
		SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(userOpeCmd);
		BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
	}

	/**
	 * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private void copyAndPaste(Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperationCommand userOpeCmd) {

		Collection<BhNode> candidates = readyToCopy.stream()
			.filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

		Collection<Pair<BhNode, BhNode>> orgs_copies = candidates.stream()
			.map(node -> new Pair<BhNode, BhNode>(node, node.genCopyNode(candidates, userOpeCmd)))
			.filter(org_copy -> org_copy._2 != null)
			.collect(Collectors.toCollection(ArrayList::new));

		// 外部ノードでかつ, コピー対象に含まれていないでかつ, 親はコピー対象 -> コピーしない
		Predicate<BhNode> isNodeToBeCopied = bhNode -> {
			return !(bhNode.isOuter() && !readyToCopy.contains(bhNode) && readyToCopy.contains(bhNode.findParentNode()));
		};

		// 貼り付け処理
		for (var org_copy : orgs_copies) {
			BhNode copy = org_copy._2;
			NodeMVCBuilder.build(copy);
			TextImitationPrompter.prompt(copy);
			BhNodeHandler.INSTANCE.addRootNode(
				wsToPasteIn,
				copy,
				pasteBasePos.x,
				pasteBasePos.y + pastePosOffsetCount * BhParams.LnF.REPLACED_NODE_SHIFT * 2,
				userOpeCmd);

			//コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
			Vec2D size = MsgService.INSTANCE.getViewSizeIncludingOuter(org_copy._1);
			pasteBasePos.x += size.x+ BhParams.LnF.REPLACED_NODE_SHIFT * 2;
		}
		pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
	}

	/**
	 * カット予定リストのノードを引数で指定したワークスペースに移動する
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private void cutAndPaste(Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperationCommand userOpeCmd) {

		if (readyToCut.isEmpty())
			return;

		Collection<BhNode> candidates = readyToCut.stream()
			.filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

		Collection<BhNode> nodesToPaste = candidates.stream()
			.filter(node -> node.execScriptOnCutRequested(candidates, userOpeCmd))
			.collect(Collectors.toCollection(ArrayList::new));

		// 貼り付け処理
		for (var node : nodesToPaste) {
			Optional<BhNode> newChild = BhNodeHandler.INSTANCE.deleteNodeIncompletely(node, true, userOpeCmd);
			BhNodeHandler.INSTANCE.addRootNode(
				wsToPasteIn,
				node,
				pasteBasePos.x,
				pasteBasePos.y + pastePosOffsetCount * BhParams.LnF.REPLACED_NODE_SHIFT * 2,
				userOpeCmd);
			Vec2D size = MsgService.INSTANCE.getViewSizeIncludingOuter(node);
			pasteBasePos.x += size.x + BhParams.LnF.REPLACED_NODE_SHIFT * 2;
			DelayedDeleter.INSTANCE.deleteCandidates(userOpeCmd);
			newChild.ifPresent(child -> {
				node.execScriptOnMovedFromChildToWS(child.findParentNode(), child.findRootNode(), child, true, userOpeCmd);
				child.findParentNode().execScriptOnChildReplaced(node, child, child.getParentConnector(), userOpeCmd);
			});
		}

		pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
		userOpeCmd.pushCmdOfRemoveFromList(readyToCut, readyToCut);
		readyToCut.clear();
	}

	/**
	 * コピーもしくはカットの対象になるかどうか判定する
	 * @param node 判定対象のノード
	 * @return コピーもしくはカットの対象になる場合 true
	 * */
	private boolean canCopyOrCut(BhNode node) {

		return
			(node.getState() == BhNode.State.CHILD &&
			node.findRootNode().getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS) ||
			node.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS;
	}

	public List<Workspace> getWorkspaceList() {
		return workspaceList;
	}

	/**
	 * 全ワークスペースを保存する
	 * @param fileToSave セーブファイル
	 * @return セーブに成功した場合true
	 */
	public boolean save(File fileToSave) {
		ProjectSaveData saveData = new ProjectSaveData(workspaceList);

		try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileToSave));){
			outputStream.writeObject(saveData);
			MsgPrinter.INSTANCE.msgForUser("-- 保存完了 (" + fileToSave.getPath() + ") --\n");
			BunnyHop.INSTANCE.shouldSave(false);
			return true;
		}
		catch(IOException e) {
			MsgPrinter.INSTANCE.alert(
				Alert.AlertType.ERROR,
				"ファイルの保存に失敗しました",
				null,
				fileToSave.getPath() + "\n" + e.toString());
			return false;
		}
	}

	/**
	 * ファイルからワークスペースをロードし追加する
	 * @param loaded ロードファイル
	 * @param isOldWsCleared ロード方法を確認する関数
	 * @return ロードに成功した場合true
	 */
	public boolean load(File loaded, Boolean isOldWsCleared) {

		try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(loaded));){
			ProjectSaveData loadData = (ProjectSaveData)inputStream.readObject();
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			if (isOldWsCleared)
				BunnyHop.INSTANCE.deleteAllWorkspace(userOpeCmd);

			loadData.load(userOpeCmd).forEach(ws -> {
				BunnyHop.INSTANCE.addWorkspace(ws, userOpeCmd);
			});
			BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
			return true;
		}
		catch(ClassNotFoundException | IOException | ClassCastException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(WorkspaceSet.class.getSimpleName() + ".load\n" + e.toString());
			return false;
		}
	}

	/**
	 * 現在操作対象のワークスペースを取得する
	 * @return 現在操作対象のワークスペース
	 */
	public Workspace getCurrentWorkspace() {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_CURRENT_WORKSPACE, this).workspace;
	}

	/**
	 * コピー予定ノードのリストを取得する
	 * @return コピー予定ノードのリスト
	 * */
	public ObservableList<BhNode> getListReadyToCopy() {
		return readyToCopy;
	}

	/**
	 * カット予定ノードのリストを取得する
	 * @return カット予定ノードのリスト
	 * */
	public ObservableList<BhNode> getListReadyToCut() {
		return readyToCut;
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

