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
package net.seapanda.bunnyhop.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.modelhandler.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * コンパイル対象のノードを準備するクラス
 * @author K.Koike
 * */
public class CompileNodesArranger {

	private CompileNodesArranger() {}

	/**
	 * コンパイル対象のノードを準備する.
	 * @param wss このワークスペースセットからコンパイル対象ノードを集める.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return コンパイル対象ノードと実行対象ノードのペア
	 * */
	public static Optional<Pair<List<BhNode>, BhNode>> arrange(WorkspaceSet wss, UserOperationCommand userOpeCmd) {
		return new CompileNodesArranger().collectNodesToCompile(wss, userOpeCmd);

	}

	/**
	 * コンパイル前の準備をする
	 * @param wss このワークスペースセットからコンパイル対象ノードを集める.
	 * @param userOpeCmd undo用コマンドオブジェクト.
	 * @return コンパイル対象ノードと実行対象ノードのペア
	 */
	private Optional<Pair<List<BhNode>, BhNode>> collectNodesToCompile(
		WorkspaceSet wss, UserOperationCommand userOpeCmd) {

		if (!deleteSyntaxErrorNodes(userOpeCmd)) {
			BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
			return Optional.empty();
		}

		Optional<BhNode> nodeToExec = findNodeToExecute(wss.getCurrentWorkspace(), userOpeCmd);
		if (nodeToExec.isEmpty())
			return Optional.empty();

		//コンパイル対象ノードを集める
		var nodesToCompile = new ArrayList<BhNode>();
		wss.getWorkspaceList().forEach(ws -> {
			ws.getRootNodeList().forEach(node -> {
				nodesToCompile.add(node);
			});
		});
		nodesToCompile.remove(nodeToExec.get());
		return Optional.of(new Pair<>(nodesToCompile, nodeToExec.get()));
	}

	/**
	 * 構文エラーノードを削除する.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 全ての構文エラーノードが無くなった場合 true.
	 * */
	private boolean deleteSyntaxErrorNodes(UserOperationCommand userOpeCmd) {

		if (!SyntaxErrorNodeManager.INSTANCE.hasErrorNodes())
			return true;

		Optional<ButtonType> btnType = MsgPrinter.INSTANCE.alert(
			Alert.AlertType.CONFIRMATION,
			"構文エラーノードの削除",
			null,
			"構文エラーノードを削除してもよろしいですか?\n「いいえ」を選択した場合、実行を中止します",
			ButtonType.NO,
			ButtonType.YES);

		if (!btnType.isPresent())
			return false;

		return btnType
			.map(type -> {
				if (type.equals(ButtonType.YES)) {
					SyntaxErrorNodeManager.INSTANCE.deleteErrorNodes(userOpeCmd);
					return true;
				}
				return false;
			})
			.orElse(false);
	}

	/**
	 * 実行対象のノードを探す
	 * @param ws このワークスペースに実行対象があるかどうかチェックする.
	 * @param userOpeCmd undo用コマンドオブジェクト.
	 * @return
	 * */
	private Optional<BhNode> findNodeToExecute(Workspace ws, UserOperationCommand userOpeCmd) {

		Set<BhNode> selectedNodeList = ws.getSelectedNodeList();
		if (selectedNodeList.isEmpty()) {
			MsgPrinter.INSTANCE.alert(AlertType.ERROR, "実行対象の選択", null,"実行対象を一つ選択してください");
			return Optional.empty();
		}

		// 実行対象以外を非選択に.
		BhNode nodeToExec = selectedNodeList.iterator().next().findRootNode();
		ws.clearSelectedNodeList(userOpeCmd);
		ws.addSelectedNode(nodeToExec, userOpeCmd);
		BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
		return Optional.of(nodeToExec);
	}
}

























