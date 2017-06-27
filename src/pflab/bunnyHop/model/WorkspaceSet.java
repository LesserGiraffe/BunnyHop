package pflab.bunnyHop.model;

import pflab.bunnyHop.modelHandler.DelayedDeleter;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import pflab.bunnyHop.ModelProcessor.NodeMVCBuilder;
import pflab.bunnyHop.ModelProcessor.UnscopedNodeCollector;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Pair;
import pflab.bunnyHop.common.Point2D;

import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgSender;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.root.BunnyHop;
import pflab.bunnyHop.saveAndLoad.ProjectSaveData;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * ワークスペースの集合を保持、管理するクラス
 * @author K.Koike
 * */
public class WorkspaceSet implements MsgSender {

	private final List<BhNode> readyToCopy = new ArrayList<>();	//!< コピー予定のノード
	private final List<BhNode> readyToCut = new ArrayList<>();	//!< カット予定のノード
	private final List<Workspace> workspaceList = new ArrayList<>();	//!< 全てのワークスペースのリスト

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
		MsgTransporter.instance().sendMessage(BhMsg.PUSH_USER_OPE_CMD, new MsgData(userOpeCmd), wsToPasteIn);
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
				NodeMVCBuilder mvcBuilder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, userOpeCmd);
				pasted.accept(mvcBuilder);
				BhNodeHandler.instance.addRootNode(wsToPasteIn, pasted, pasteBasePos.x, pasteBasePos.y, userOpeCmd);
				UnscopedNodeCollector unscopedNodeCollector = new UnscopedNodeCollector();
				pasted.accept(unscopedNodeCollector);
				BhNodeHandler.instance.deleteNodes(unscopedNodeCollector.getUnscopedNodeList(), userOpeCmd);
				
				//コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
				Pair<Double, Double> size = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW_SIZE_WITH_OUTER, new MsgData(true), node).doublePair;
				pasteBasePos.x += size._1 + BhParams.replacedNodePos * 2;
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
				Pair<Double, Double> size = MsgTransporter.instance().sendMessage(BhMsg.GET_VIEW_SIZE_WITH_OUTER, new MsgData(true),node).doublePair;
				pasteBasePos.x += size._1 + BhParams.replacedNodePos * 2;
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
	 * @param saved セーブファイル
	 * @return セーブに成功した場合true
	 */
	public boolean save(File saved) {
		ProjectSaveData saveData = new ProjectSaveData(workspaceList);
				
		try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(saved));){			
			outputStream.writeObject(saveData);
			return true;
		}
		catch(IOException e) {
			MsgPrinter.instance.MsgForDebug(e.toString());	//don't getMessage
			return false;
		}
	}
	
	/**
	 * ファイルからワークスペースをロードし追加する
	 * @param loaded ロードファイル
	 * @return ロードに成功した場合true
	 */
	public boolean load(File loaded) {
		
		try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(loaded));){
			ProjectSaveData loadData = (ProjectSaveData)inputStream.readObject();
			loadData.load().forEach(ws -> {
				BunnyHop.instance().addWorkSpace(ws);
			});
			return true;
		}
		catch(ClassNotFoundException | IOException | ClassCastException e) {
			MsgPrinter.instance.MsgForDebug(e.getMessage());
			return false;
		}
	}
	
	/**
	 * 現在操作対象のワークスペースを取得する
	 * @return 現在操作対象のワークスペース
	 */
	public Workspace getCurrentWorkspace() {
		return MsgTransporter.instance().sendMessage(BhMsg.GET_CURRENT_WORKSPACE, this).workspace;
	}
}