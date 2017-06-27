package pflab.bunnyHop.ModelProcessor;

import java.util.List;
import java.util.Optional;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.undo.UserOperationCommand;
	
/**
 * イミテーションノードの入れ替えを行うクラス
 * @author K.Koike
 */
public class ImitationReplacer implements BhModelProcessor {
	
	UserOperationCommand userOpeCmd;	//!< undo用コマンドオブジェクト
	BhNode oldOriginal;	//!< 入れ替え対象の古いオリジナルノード (このノードのイミテーションノードのみ入れ替え対象となる)
		
	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param oldOriginal 入れ替え対象の古いオリジナルノード (このノードのイミテーションノードのみ入れ替え対象となる)
	 */
	public ImitationReplacer(UserOperationCommand userOpeCmd, BhNode oldOriginal) {
		this.userOpeCmd = userOpeCmd;
		this.oldOriginal = oldOriginal;
	}
	
	/**
	 * @param newOriginal イミテーションを作成して、入れ替えを行いたいオリジナルノード
	 */
	@Override
	public void visit(ConnectiveNode newOriginal) {		
		
		String imitTag = newOriginal.getParentConnector().getImitationTag();	//オリジナルノードの接続先コネクタのイミテーションタグと一致するイミテーションの位置に接続する
		//子オリジナルノードに対応するイミテーションがある場合
		if (newOriginal.getImitationInfo().hasImitationID(imitTag)) {
			//オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
			replaceConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), newOriginal, imitTag);
		}
		else {
			//オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
			removeConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), imitTag);
		}			
	}
	
	@Override
	public void visit(TextNode newOrigianl) {
		
		String imitTag = newOrigianl.getParentConnector().getImitationTag();	//オリジナルノードの接続先コネクタのイミテーションタグと一致するイミテーションの位置に接続する
		//子オリジナルノードに対応するイミテーションがある場合
		if (newOrigianl.getImitationInfo().hasImitationID(imitTag)) {
			//オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
			replaceConnectiveChild(newOrigianl.findParentNode().getImitationInfo().getImitationList(), newOrigianl, imitTag);
		}
		else {
			//オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
			removeConnectiveChild(newOrigianl.findParentNode().getImitationInfo().getImitationList(), imitTag);
		}
	}
	
	@Override
	public void  visit(VoidNode newOriginal) {
		String imitTag = newOriginal.getParentConnector().getImitationTag();	//オリジナルノードの接続先コネクタのイミテーションタグと一致するイミテーションの位置に接続する
		removeConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), imitTag);	
	}
	
	/**
	 * imitParentが持つコネクタのイミテーションタグがimitTagと一致した場合そのノードを返す
	 * @param imitParent imitTagの位置に入れ替えもしくはremove対象になるイミテーションノードを持っているか探すノード
	 * @param imitTag このイミテーションタグを指定されたコネクタがimitParentにあった場合, そのコネクタに接続されたノードを返す
	 * @return 入れ替えもしくは削除対象になるノード. 見つからなかった場合 Optional.emptyを返す
	 */
	private Optional<BhNode> getNodeToReplaceOrRemove(ConnectiveNode imitParent, String imitTag) {
		
		ConnectiveChildFinder finder = new ConnectiveChildFinder(imitTag);
		imitParent.accept(finder);
		BhNode connectedNode = finder.getFoundNode();	//すでにイミテーションにつながっているノード
		if (connectedNode == null)
			return Optional.empty();
		
		if (!connectedNode.isInWorkspace())	//遅延削除待ち状態のノード
			return Optional.empty();
	
		return Optional.of(connectedNode);
	}
		
	/**
	 * ConnectiveNode の子を入れ替える
	 * @param parentNodeList 子ノードを入れ替えるConnecitveNodeのリスト
	 * @param original このノードのイミテーションで子ノードを置き換える
	 * @param imitTag このイミテーションタグが指定されたコネクタにつながるノードを入れ替える
	 */
	private void replaceConnectiveChild(
		List<ConnectiveNode> parentNodeList,
		Imitatable original,
		String imitTag) {
		
		for (ConnectiveNode parent : parentNodeList) {
			Optional<BhNode> replacedNode = getNodeToReplaceOrRemove(parent, imitTag);
			replacedNode.ifPresent(replacedImit -> {
				Workspace ws = replacedImit.getWorkspace();
				Imitatable newImit = original.findExistingOrCreateNewImit(replacedImit, userOpeCmd);
				BhNodeHandler.instance.replaceChildNewlyCreated(replacedImit, newImit, userOpeCmd);
				BhNodeHandler.instance.deleteNodeIncompletely(replacedImit, userOpeCmd);
			});
		}
	}
	
	/**
	 * ConnectiveNode の子を削除する
	 * @param parentNodeList 子ノードを削除するConnecitveノードのリスト
	 * @param imitTag このイミテーションタグが指定されたコネクタにつながるノードを削除する
	 */	
	private void removeConnectiveChild(List<ConnectiveNode> parentNodeList, String imitTag) {
		
		for (ConnectiveNode parent : parentNodeList) {
			Optional<BhNode> removedNode = getNodeToReplaceOrRemove(parent, imitTag);
			removedNode.ifPresent(removed -> {
				if (removed.getOriginalNode() == oldOriginal) {	//取り除くノードのオリジナルノードが入れ替え対象の古いノードであった場合
					Workspace ws = removed.getWorkspace();
					BhNodeHandler.instance.removeChild(removed, userOpeCmd);
					BhNodeHandler.instance.deleteNodeIncompletely(removed, userOpeCmd);				
				}
			});
		}
	}
}
