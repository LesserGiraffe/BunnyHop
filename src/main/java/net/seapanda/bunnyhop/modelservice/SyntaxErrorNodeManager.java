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
package net.seapanda.bunnyhop.modelservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.modelprocessor.SyntaxErrorNodeCollector;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * @author K.Koike
 * 構文エラーノードを集めて管理するクラス
 * */
public class SyntaxErrorNodeManager {

	public static final SyntaxErrorNodeManager INSTANCE = new SyntaxErrorNodeManager();	//!< シングルトンインスタンス
	private final Set<BhNode> errorNodeList = new HashSet<>();	//構文エラーノードのリスト

	private SyntaxErrorNodeManager() {}

	/**
	 * 以下の2種類の構文エラーノードを管理対象に入れる
	 *
	 * <pre>
	 *   ・引数のノード以下にある構文エラーノード
	 *   ・引数のノード以下にあるオリジナルノードが持つ構文エラーを起こしているイミテーションノード
	 *</pre>
	 * */
	public void collect(BhNode node, UserOperationCommand userOpeCmd) {

		if (MsgService.INSTANCE.isTemplateNode(node))
			return;

		List<BhNode> errorNodes = SyntaxErrorNodeCollector.collect(node);
		errorNodes.forEach(errorNode -> {
			if (!errorNodeList.contains(errorNode)) {
				errorNodeList.add(errorNode);
				userOpeCmd.pushCmdOfAddToList(errorNodeList, errorNode);
			}
		});
	}

	/**
	 * 管理下のノードの構文エラー表示を更新する
	 * */
	public void updateErrorNodeIndicator(UserOperationCommand userOpeCmd) {
		errorNodeList.forEach(node -> {
			if (node.getState() != BhNode.State.DELETED)
				MsgService.INSTANCE.setSyntaxErrorIndicator(node, node.hasSyntaxError(), userOpeCmd);
		});
	}

	/**
	 * 構文エラーノード以外のノードを全て管理下から外す.
	 * */
	public void unmanageNonErrorNodes(UserOperationCommand userOpeCmd) {

		var nodesToRemove =
			errorNodeList.stream()
			.filter(node -> !node.hasSyntaxError())
			.collect(Collectors.toCollection(ArrayList::new));

		errorNodeList.removeAll(nodesToRemove);
		userOpeCmd.pushCmdOfRemoveFromList(errorNodeList, nodesToRemove);
	}

	/**
	 * 管理下の全ての構文エラーノードを削除する.
	 * */
	public void deleteErrorNodes(UserOperationCommand userOpeCmd) {

		var nodesToDelete =
			errorNodeList.stream()
			.filter(node -> node.hasSyntaxError())
			.collect(Collectors.toCollection(HashSet::new));

		nodesToDelete.forEach(node -> node.getEventDispatcher().dispatchOnDeletionRequested(
			nodesToDelete, CauseOfDeletion.SYNTAX_ERROR, userOpeCmd));

		List<Pair<BhNode, BhNode>> oldAndNewNodeList =
			BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpeCmd);
		for (var oldAndNewNode : oldAndNewNodeList) {
			BhNode oldNode = oldAndNewNode._1;
			BhNode newNode = oldAndNewNode._2;
			newNode.findParentNode().execScriptOnChildReplaced(
				oldNode, newNode, newNode.getParentConnector(), userOpeCmd);
		}

		errorNodeList.removeAll(nodesToDelete);
		userOpeCmd.pushCmdOfRemoveFromList(errorNodeList, nodesToDelete);
	}

	/**
	 * 管理下のノードに構文エラーノードがあるかどうか調べる.
	 * @return 構文エラーノードが1つでもある場合 true
	 * */
	public boolean hasErrorNodes() {
		return errorNodeList.stream().anyMatch(node -> node.hasSyntaxError());
	}
}
















