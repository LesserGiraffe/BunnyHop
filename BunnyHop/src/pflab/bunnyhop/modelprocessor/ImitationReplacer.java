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
package pflab.bunnyhop.modelprocessor;

import java.util.List;
import java.util.Optional;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.model.connective.ConnectiveNode;
import pflab.bunnyhop.model.imitation.Imitatable;
import pflab.bunnyhop.modelhandler.BhNodeHandler;
import pflab.bunnyhop.model.VoidNode;
import pflab.bunnyhop.model.Workspace;
import pflab.bunnyhop.model.imitation.ImitationConnectionPos;
import pflab.bunnyhop.model.imitation.ImitationID;
import pflab.bunnyhop.undo.UserOperationCommand;
	
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
	 * @param newOriginal このノードのイミテーションを作成して、親ノードのイミテーションの子ノードと入れ替えを行う
	 */
	@Override
	public void visit(ConnectiveNode newOriginal) {	
		
		ImitationID imitID = newOriginal.getParentConnector().findImitationID();
		ImitationConnectionPos imitCnctPos = newOriginal.getParentConnector().getImitCnctPoint();
		//子オリジナルノードに対応するイミテーションがある場合
		if (newOriginal.getImitationInfo().imitationNodeExists(imitID)) {
			//オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
			replaceConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), newOriginal, imitCnctPos);
		}
		else {
			//オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
			removeConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), imitCnctPos);
		}			
	}
	
	@Override
	public void visit(TextNode newOriginal) {
		
		ImitationID imitID = newOriginal.getParentConnector().findImitationID();
		ImitationConnectionPos imitCnctPos = newOriginal.getParentConnector().getImitCnctPoint();
		//子オリジナルノードに対応するイミテーションがある場合
		if (newOriginal.getImitationInfo().imitationNodeExists(imitID)) {
			//オリジナルの親ノードが持つイミテーションの数だけ, 新たにイミテーションを作成して繋ぐ(入れ替える)
			replaceConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), newOriginal, imitCnctPos);
		}
		else {
			//オリジナルの親ノードが持つイミテーションの数だけ, その子ノードを削除
			removeConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), imitCnctPos);
		}
	}
	
	@Override
	public void  visit(VoidNode newOriginal) {
		ImitationConnectionPos imitCnctPos = newOriginal.getParentConnector().getImitCnctPoint();
		removeConnectiveChild(newOriginal.findParentNode().getImitationInfo().getImitationList(), imitCnctPos);	
	}
	
	/**
	 * imitParentが持つコネクタのイミテーションタグがimitTagと一致した場合そのノードを返す
	 * @param imitParent imitTagの位置に入れ替えもしくはremove対象になるイミテーションノードを持っているか探すノード
	 * @param imitCnctPos このイミテーションタグを指定されたコネクタがimitParentにあった場合, そのコネクタに接続されたノードを返す
	 * @return 入れ替えもしくは削除対象になるノード. 見つからなかった場合 Optional.emptyを返す
	 */
	private Optional<BhNode> getNodeToReplaceOrRemove(ConnectiveNode imitParent, ImitationConnectionPos imitCnctPos) {
		
		ConnectiveChildFinder finder = new ConnectiveChildFinder(imitCnctPos);
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
	 * @param imiCnctPos このイミテーション位置が指定されたコネクタにつながるノードを入れ替える
	 */
	private void replaceConnectiveChild(
		List<ConnectiveNode> parentNodeList,
		Imitatable original,
		ImitationConnectionPos imiCnctPos) {
		
		for (ConnectiveNode parent : parentNodeList) {
			Optional<BhNode> replacedNode = getNodeToReplaceOrRemove(parent, imiCnctPos);
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
	 * @param imitCnctPos このイミテーション接続位置が指定されたコネクタにつながるノードを削除する
	 */	
	private void removeConnectiveChild(List<ConnectiveNode> parentNodeList, ImitationConnectionPos imitCnctPos) {
		
		for (ConnectiveNode parent : parentNodeList) {
			Optional<BhNode> removedNode = getNodeToReplaceOrRemove(parent, imitCnctPos);
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
