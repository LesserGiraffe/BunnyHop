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
package net.seapanda.bunnyhop.model.imitation;

import java.util.Collection;
import java.util.Map;

import net.seapanda.bunnyhop.common.VersionInfo;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.model.node.CauseOfDletion;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.modelprocessor.ImitationBuilder;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
/**
 * イミテーションノード操作のインタフェース<br>
 * @author K.Koike
 */
public abstract class Imitatable extends BhNode {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	/** オリジナルノードとのつながりが切れた際のイミテーションノードの削除直前に呼ばれるスクリプトの名前 */
	public final boolean canCreateImitManually;	//!< このオブジェクトを持つノードがイミテーションノードの手動作成機能を持つ場合 true
	private final Map<ImitationID, BhNodeID> imitID_imitNodeID;	//!< イミテーションタグとそれに対応するイミテーションノードIDのマップ

	public Imitatable(String type, BhNodeAttributes attributes, Map<ImitationID, BhNodeID> imitID_imitNodeID) {

		super(type, attributes);
		this.canCreateImitManually = attributes.getCanCreateImitManually();
		this.imitID_imitNodeID = imitID_imitNodeID;
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	public Imitatable(Imitatable org) {
		super(org);
		imitID_imitNodeID = org.imitID_imitNodeID;
		canCreateImitManually = org.canCreateImitManually;
	}

	/**
	 * 引数で指定したイミテーションタグに対応したイミテーションノードを作成する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param imitID このイミテーションIDに対応したイミテーションノードを作成する
	 * @return 作成されたイミテーションノード. イミテーションを持たないノードの場合nullを返す
	 */
	public abstract BhNode createImitNode(UserOperationCommand userOpeCmd, ImitationID imitID);

	/**
	 * イミテーションノードであった場合true を返す
	 * @return イミテーションノードであった場合true を返す
	 */
	public boolean isImitationNode() {
		return getImitationManager().getOriginal() != null;
	}

	/**
	 * イミテーションノード管理オブジェクトを取得する
	 * @return イミテーションノード管理オブジェクト
	 */
	public abstract <T extends Imitatable> ImitationManager<T> getImitationManager();

	/**
	 * 入れ替え用の既存のイミテーションノードを探す. <br>
	 * 見つからない場合は, 新規作成する.
	 * @param oldNode この関数が返すノードと入れ替えられる古いノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return oldNodeと入れ替えるためのイミテーションノード
	 */
	public Imitatable findExistingOrCreateNewImit(BhNode oldNode, UserOperationCommand userOpeCmd) {

		BhNode outerTailOfOldNode = oldNode.findOuterNode(-1);
		for(Imitatable imit : getImitationManager().getImitationList()) {
			//新しく入れ替わるノードの外部末尾ノードが最後に入れ替わったノードの外部末尾ノードと一致するイミテーションノードを入れ替えイミテーションノードとする
			if  (imit.getLastReplaced() != null) {
				if(!imit.isInWorkspace() && imit.getLastReplaced().findOuterNode(-1) == outerTailOfOldNode) {
					return imit;
				}
			}
		}
		return ImitationBuilder.buildForAutoCreation(this, userOpeCmd);
	}

	/**
	 * オリジナル - イミテーションの関係を削除する
	 * @param toDelete 削除するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void disconnectOrgImitRelation(Imitatable toDelete, UserOperationCommand userOpeCmd) {
		getImitationManager().removeImitation(toDelete, userOpeCmd);
		toDelete.getImitationManager().setOriginal(null, userOpeCmd);
	}

	/**
	 * 引数で指定したイミテーションIDに対応するイミテーションノードIDがある場合true を返す
	 * @param imitID このイミテーションIDに対応するイミテーションノードIDがあるか調べる
	 * @return イミテーションノードIDが指定してある場合true
	 */
	public boolean imitationNodeExists(ImitationID imitID) {
		return imitID_imitNodeID.containsKey(imitID);
	}

	/**
	 * 引数で指定したイミテーションタグに対応するイミテーションノードIDを返す
	 * @param imitID このイミテーションIDに対応するイミテーションノードIDを返す
	 * @return 引数で指定したコネクタ名に対応するイミテーションノードID
	 */
	public BhNodeID getImitationNodeID(ImitationID imitID) {
		BhNodeID imitNodeID = imitID_imitNodeID.get(imitID);
		assert imitID != null;
		return imitNodeID;
	}

	/**
	 * イミテーションノード削除前のイベント処理を実行する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void execScriptOfImitDeletion(UserOperationCommand userOpeCmd) {

		Collection<? extends BhNode> imitationList = getImitationManager().getImitationList();
		imitationList.forEach(node -> {
			node.execScriptOnDeletionRequested(
				imitationList, CauseOfDletion.INFLUENCE_OF_ORIGINAL_DELETION, userOpeCmd);
		});
	}

	@Override
	public boolean isRemovable() {
		if (parentConnector == null)
			return false;

		if (isDefaultNode())	//デフォルトノードは移動不可
			return false;

		return !parentConnector.isFixed();
	}

	@Override
	public boolean canBeReplacedWith(BhNode node) {

		if (getState() != BhNode.State.CHILD)
			return false;

		if (findRootNode().getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS)
			return false;

		if (node.isDescendantOf(this) || this.isDescendantOf(node))	//同じtree に含まれている場合置き換え不可
			return false;

		return parentConnector.isConnectedNodeReplaceableWith(node);
	}
}


