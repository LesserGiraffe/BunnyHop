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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.modelprocessor.UnscopedNodeCollector;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * @author K.Koike
 * スコープ外ノードを集めて管理するクラス
 * */
public class UnscopedNodeManager {

	public static final UnscopedNodeManager INSTANCE = new UnscopedNodeManager();	//!< シングルトンインスタンス
	private final Set<Imitatable> unscopedNodeList = new HashSet<>();	//スコープ外ノードのリスト

	private UnscopedNodeManager() {}

	/**
	 * 以下の2種類のスコープ外ノードを管理対象に入れる
	 *   ・引数のノード以下にあるスコープ外イミテーションノード
	 *   ・引数のノード以下にあるオリジナルノードのスコープ外イミテーションノード
	 * */
	public void collect(BhNode node, UserOperationCommand userOpeCmd) {

		List<Imitatable> unscopdeNodes = UnscopedNodeCollector.collect(node);
		unscopdeNodes.forEach(unscoped -> {
			if (!unscopedNodeList.contains(unscoped)) {
				unscopedNodeList.add(unscoped);
				userOpeCmd.pushCmdOfAddToList(unscopedNodeList, unscoped);
			}
		});
	}

	/**
	 * 管理下のノードのスコープ外警告表示を更新する
	 * */
	public void updateUnscopedNodeWarning(UserOperationCommand userOpeCmd) {
		unscopedNodeList.forEach(node -> {
			if (node.getState() != BhNode.State.DELETED)
				MsgService.INSTANCE.setUnscopedNodeWarning(node, node.isUnscoped(), userOpeCmd);
		});
	}

	/**
	 * スコープ外ノード以外のノードを全て管理下から外す.
	 * */
	public void unmanageScopedNodes(UserOperationCommand userOpeCmd) {

		var nodesToRemove =
			unscopedNodeList.stream()
			.filter(node -> !node.isUnscoped())
			.collect(Collectors.toCollection(ArrayList::new));

		unscopedNodeList.removeAll(nodesToRemove);
		userOpeCmd.pushCmdOfRemoveFromList(unscopedNodeList, nodesToRemove);
	}

	/**
	 * 全てのスコープ外ノードを削除する.
	 * */
	public void deleteUnscopedNodes(UserOperationCommand userOpeCmd) {

		var nodesToDelete =
			unscopedNodeList.stream()
			.filter(node -> (node.isUnscoped()))
			.collect(Collectors.toCollection(ArrayList::new));

		nodesToDelete.forEach(imit -> imit.execScriptOnImitDeletionOrdered(userOpeCmd));
		BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpeCmd)
		.forEach(oldAndNewNode -> {
			BhNode oldNode = oldAndNewNode._1;
			BhNode newNode = oldAndNewNode._2;
			newNode.findParentNode().execScriptOnChildReplaced(oldNode, newNode, newNode.getParentConnector(), userOpeCmd);
		});

		unscopedNodeList.removeAll(nodesToDelete);
		userOpeCmd.pushCmdOfRemoveFromList(unscopedNodeList, nodesToDelete);
	}

	/**
	 * スコープ外ノードがあるかどうか調べる.
	 * @return スコープ外ノードが1つでもある場合 true
	 * */
	public boolean hasUnscopedNodes() {
		return unscopedNodeList.stream().anyMatch(node -> node.isUnscoped());
	}
}
















