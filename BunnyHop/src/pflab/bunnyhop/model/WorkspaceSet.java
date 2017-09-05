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
package pflab.bunnyhop.model;

import pflab.bunnyhop.modelhandler.DelayedDeleter;
import pflab.bunnyhop.modelhandler.BhNodeHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.Alert;
import pflab.bunnyhop.modelprocessor.NodeMVCBuilder;
import pflab.bunnyhop.modelprocessor.UnscopedNodeCollector;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Pair;
import pflab.bunnyhop.common.Point2D;

import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgProcessor;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.root.BunnyHop;
import pflab.bunnyhop.saveandload.ProjectSaveData;
import pflab.bunnyhop.undo.UserOperationCommand;
import pflab.bunnyhop.message.MsgReceptionWindow;

/**
 * ワークスペースの集合を保持、管理するクラス
 * @author K.Koike
 * */
public class WorkspaceSet implements MsgReceptionWindow {

	private final List<BhNode> readyToCopy = new ArrayList<>();	//!< コピー予定のノード
	private final List<BhNode> readyToCut = new ArrayList<>();	//!< カット予定のノード
	private final List<Workspace> workspaceList = new ArrayList<>();	//!< 全てのワークスペースのリスト
	private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト

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
	 */
	public void addNodeListReadyToCopy(Collection<BhNode> nodeList) {	
		readyToCut.clear();
		readyToCopy.clear();
		readyToCopy.addAll(nodeList);
	}
	
	/**
	 * カット予定のBhNodeリストを追加する
	 * @param nodeList カット予定のBhNodeリスト
	 */
	public void addNodeListReadyToCut(Collection<BhNode> nodeList) {
		readyToCut.clear();
		readyToCopy.clear();
		readyToCut.addAll(nodeList);
	}
	
	/**
	 * ペースト処理を行う
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 */
	public void paste(Workspace wsToPasteIn, Point2D pasteBasePos) {
		UserOperationCommand userOpeCmd = new UserOperationCommand();
		copyAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
		cutAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
		BunnyHop.instance.pushUserOpeCmd(userOpeCmd);
	}
	
	/**
	 * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private void copyAndPaste(Workspace wsToPasteIn, Point2D pasteBasePos, UserOperationCommand userOpeCmd) {

		for (BhNode node : readyToCopy) {
			//ワークスペースに無いノードはコピーできない
			if (!node.isInWorkspace())
				continue;
			
			//手動脱着可能なノードもしくはルートノードをコピーする
			if (node.isRemovable() ||
				node.findRootNode().getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS) {
				
				BhNode pasted = node.copy(userOpeCmd);
				NodeMVCBuilder mvcBuilder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default);
				pasted.accept(mvcBuilder);
				BhNodeHandler.instance.addRootNode(wsToPasteIn, pasted, pasteBasePos.x, pasteBasePos.y, userOpeCmd);
				UnscopedNodeCollector unscopedNodeCollector = new UnscopedNodeCollector();
				pasted.accept(unscopedNodeCollector);
				BhNodeHandler.instance.deleteNodes(unscopedNodeCollector.getUnscopedNodeList(), userOpeCmd);
				
				//コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
				Pair<Double, Double> size = MsgTransporter.instance.sendMessage(BhMsg.GET_VIEW_SIZE_WITH_OUTER, new MsgData(true), node).doublePair;
				pasteBasePos.x += size._1+ BhParams.REPLACED_NODE_POS * 2;
			}
		}
	}

	/**
	 * カット予定リストのノードを引数で指定したワークスペースに移動する
	 * @param wsToPasteIn 貼り付け先のワークスペース
	 * @param pasteBasePos 貼り付け基準位置
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */	
	private void cutAndPaste(Workspace wsToPasteIn, Point2D pasteBasePos, UserOperationCommand userOpeCmd) {
		
		for (BhNode node : readyToCut) {
			//ワークスペースに無いノードはコピーできない
			if (!node.isInWorkspace())
				continue;
			
			//手動脱着可能なノードもしくはルートノードをカットし貼り付ける
			if (node.isRemovable() ||
				node.findRootNode().getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS) {			
				
				BhNodeHandler.instance.deleteNodeIncompletely(node, userOpeCmd);
				BhNodeHandler.instance.addRootNode(wsToPasteIn, node, pasteBasePos.x, pasteBasePos.y, userOpeCmd);
				UnscopedNodeCollector unscopedNodeCollector = new UnscopedNodeCollector();
				node.accept(unscopedNodeCollector);
				BhNodeHandler.instance.deleteNodes(unscopedNodeCollector.getUnscopedNodeList(), userOpeCmd);
				Pair<Double, Double> size = MsgTransporter.instance.sendMessage(BhMsg.GET_VIEW_SIZE_WITH_OUTER, new MsgData(true),node).doublePair;
				pasteBasePos.x += size._1+ BhParams.REPLACED_NODE_POS * 2;
			}
		}
		readyToCut.clear();
		DelayedDeleter.instance.deleteCandidates(userOpeCmd);
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
			MsgPrinter.instance.MsgForUser("-- 保存完了 (" + fileToSave.getPath() + ") --\n");
			return true;
		}
		catch(IOException e) {
			MsgPrinter.instance.alert(
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
	public boolean load(File loaded, Supplier<Boolean> isOldWsCleared) {
		
		try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(loaded));){
			ProjectSaveData loadData = (ProjectSaveData)inputStream.readObject();
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			if (isOldWsCleared.get())
				BunnyHop.instance.deleteAllWorkspace(userOpeCmd);
			
			loadData.load(userOpeCmd).forEach(ws -> {
				BunnyHop.instance.addWorkspace(ws, userOpeCmd);
			});
			BunnyHop.instance.pushUserOpeCmd(userOpeCmd);
			return true;
		}
		catch(ClassNotFoundException | IOException | ClassCastException e) {
			MsgPrinter.instance.ErrMsgForDebug(WorkspaceSet.class.getSimpleName() + ".load\n" + e.toString());
			return false;
		}
	}
	
	/**
	 * 現在操作対象のワークスペースを取得する
	 * @return 現在操作対象のワークスペース
	 */
	public Workspace getCurrentWorkspace() {
		return MsgTransporter.instance.sendMessage(BhMsg.GET_CURRENT_WORKSPACE, this).workspace;
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