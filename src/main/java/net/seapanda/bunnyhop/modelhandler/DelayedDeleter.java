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
package net.seapanda.bunnyhop.modelhandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 遅延削除を実装するクラス
 * @author Koike
 */
public class DelayedDeleter {

	public static final DelayedDeleter INSTANCE = new DelayedDeleter();	//!< シングルトンインスタンス
	private List<BhNodeWithUnexecutedOpe> deletionCandidateNodeList = new ArrayList<>();	//!< 特定のタイミングで削除するノードのリスト.

	private DelayedDeleter(){}

	/**
	 * 削除候補のノードを追加する<br>
	 * 登録するリストは, 4分木空間, ワークスペースからの削除とモデルの親子関係の削除が済んでいること
	 * @param candidate 削除候補のノード
	 * @param unexecutedModelDeletion モデル間の関係 (親子関係除く) の削除を行っていない場合true
	 */
	public void addDeletionCandidate(
		BhNode candidate,
		boolean unexecutedModelDeletion) {
		deletionCandidateNodeList.add(
			new BhNodeWithUnexecutedOpe(candidate, unexecutedModelDeletion));
	}

	/**
	 * 引数で指定した削除候補のノードを削除し, 削除候補リストから消す. <br>
	 * ただし, 指定したノードがワークスペース上にあった場合は削除されずに, 削除候補リストから消える.
	 * @param nodeToDelete 削除するノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteCandidate(BhNode nodeToDelete, UserOperationCommand userOpeCmd) {

		BhNodeWithUnexecutedOpe deleted = null;
		for (BhNodeWithUnexecutedOpe candidate : deletionCandidateNodeList) {
			if (candidate.bhNode == nodeToDelete) {

				deleted = candidate;
				// ワークスペースにあるノードは削除をキャンセルされたものとみなす
				if (!candidate.bhNode.isInWorkspace()) {
					if (candidate.modelDeletion)
						candidate.bhNode.delete(userOpeCmd);
				}
				break;
			}
		}
		deletionCandidateNodeList.remove(deleted);
	}

	/**
	 * 削除候補のノードを全て削除し, 削除候補リストから消す. <br>
	 * ただし, ワークスペース上にあるノードは削除されずに, 削除候補リストから消える.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteCandidates(UserOperationCommand userOpeCmd) {

		deletionCandidateNodeList.forEach(
			candidate -> {

				// ワークスペースにあるノードは削除をキャンセルされたものとみなす
				if (!candidate.bhNode.isInWorkspace()) {
					if (candidate.modelDeletion)
						candidate.bhNode.delete(userOpeCmd);
				}
			});
		deletionCandidateNodeList.clear();
	}

	/**
	 * 削除候補リストの中に引数で指定したノードが入っているか調べる
	 * @param node このノードが削除候補リストの中に入っているか調べる
	 * @return 削除候補リストの中に引数で指定したノードが入っている場合true
	 */
	public boolean containsInCandidateList(BhNode node) {

		for (BhNodeWithUnexecutedOpe candidate : deletionCandidateNodeList)
			if (candidate.bhNode == node)
				return true;

		return false;
	}

	/**
	 * 削除候補のリストを返す
	 * @return 削除候補のリスト
	 */
	public Collection<BhNodeWithUnexecutedOpe> getDeletionCadidateList() {
		return deletionCandidateNodeList;
	}

	/**
	 * 削除候補のノードと削除操作のうち未完了の操作
	 * */
	private static class BhNodeWithUnexecutedOpe {

		public final boolean modelDeletion;
		public final BhNode bhNode;

		public BhNodeWithUnexecutedOpe (BhNode bhNode, boolean modelDeletion) {
			this.bhNode = bhNode;
			this.modelDeletion = modelDeletion;
		}

		@Override
		public String toString() {
			return "modelDeletion : " + modelDeletion + "  " + bhNode;
		}
	}
}


















