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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.seapanda.bunnyhop.common.VersionInfo;
import net.seapanda.bunnyhop.modelhandler.BhNodeHandler;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードとオリジナルノードを保持するクラス
 * @author K.Koike
 */
public class ImitationManager<T extends Imitatable> implements Serializable {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	private final List<T> imitNodeList;	//!< このオブジェクトを持つノードから作成されたイミテーションノードの集合
	private T orgNode;	//!< このオブジェクトを持つノードがイミテーションノードの場合、そのオリジナルノードを保持する

	/**
	 * コンストラクタ
	 * @param imitID_imitNodeID イミテーションIDとそれに対応するイミテーションノードIDのマップ
	 * @param canCreateImitManually イミテーションノードの手動作成機能の有無
	 * @param scopeName オリジナルノードと同じスコープにいるかチェックする際の名前
	 **/
	public ImitationManager() {
		imitNodeList = new ArrayList<>();
		orgNode = null;
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param owner このオブジェクトを持つノード
	 **/
	public ImitationManager(ImitationManager<T> org, UserOperationCommand userOpeCmd, T owner) {

		imitNodeList = new ArrayList<>();	//元ノードをコピーしても、イミテーションノードとのつながりは無いようにする
		orgNode = null;
		if (org.isImitationNode()) {
			//イミテーションをコピーした場合, コピー元と同じオリジナルノードのイミテーションノードとする
			T original = org.getOriginal();
			original.getImitationManager().addImitation(owner, userOpeCmd);
			setOriginal(original, userOpeCmd);
		}
	}

	/**
	 * イミテーションノードのオリジナルノードをセットする
	 * @param orgNode イミテーションノードの作成元ノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 **/
	public final void setOriginal(T orgNode, UserOperationCommand userOpeCmd) {
		userOpeCmd.<T>pushCmdOfSetOriginal(this, this.orgNode);
		this.orgNode = orgNode;
	}

	/**
	 * このオブジェクトを持つノードのオリジナルノードを返す
	 * @return このオブジェクトを持つノードのオリジナルノード
	 */
	public final T getOriginal() {
		return orgNode;
	}

	/**
	 * イミテーションノードを追加する
	 * @param imitNode 追加するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void addImitation(T imitNode, UserOperationCommand userOpeCmd) {
		imitNodeList.add(imitNode);
		userOpeCmd.<T>pushCmdOfAddImitation(this, imitNode);
	}

	/**
	 * イミテーションノードを削除する
	 * @param imitNode 削除するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void removeImitation(T imitNode, UserOperationCommand userOpeCmd) {
		imitNodeList.remove(imitNode);
		userOpeCmd.<T>pushCmdOfRemoveImitation(this, imitNode);
	}

	/**
	 * イミテーションノードリストを取得する
	 * @return イミテーションノードリスト
	 */
	public List<T> getImitationList() {
		return Collections.unmodifiableList(imitNodeList);
	}

	/**
	 * イミテーションノードである場合 trueを返す
	 * @return イミテーションノードである場合 true
	 */
	private boolean isImitationNode() {
		return orgNode != null;
	}

	/**
	 * 全てのイミテーションノードを消す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteAllImitations(UserOperationCommand userOpeCmd) {

		for (var nodeToDelete : imitNodeList) {
			if (nodeToDelete.isInWorkspace()) {
				nodeToDelete.execScriptOnImitDeletionRequested(userOpeCmd);
			}
		}

		while (!imitNodeList.isEmpty()) {	//重複削除を避けるため, while で空になるまで消す

			Imitatable nodeToDelete = imitNodeList.get(0);
			if (!nodeToDelete.isInWorkspace()) {
				nodeToDelete.getOriginalNode().disconnectOrgImitRelation(nodeToDelete, userOpeCmd);	//WSに居ない場合は, 削除予定のノードなので, オリジナル-イミテーションの関係だけ消しておく.
			}
			else {
				BhNodeHandler.INSTANCE.deleteNode(nodeToDelete, userOpeCmd)
				.ifPresent(
					newNode -> newNode.findParentNode().execScriptOnChildReplaced(
						nodeToDelete,
						newNode,
						newNode.getParentConnector(),
						userOpeCmd));
			}
		}
	}
}
