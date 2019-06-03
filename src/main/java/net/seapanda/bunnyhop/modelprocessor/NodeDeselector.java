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
package net.seapanda.bunnyhop.modelprocessor;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.VoidNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ノードツリーの全ノードを非選択にするクラス
 * @author K.Koike
 */
public class NodeDeselector implements BhModelProcessor {

	private final UserOperationCommand userOpeCmd;

	/**
	 * 引数で指定したノード以下のノードを非選択にする.
	 * @param node このノード以下のノードを非選択にする.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public static void deselect(BhNode node, UserOperationCommand userOpeCmd) {
		node.accept(new NodeDeselector(userOpeCmd));
	}

	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private NodeDeselector(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}

	@Override
	public void visit(ConnectiveNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
		node.sendToSections(this);
	}

	@Override
	public void visit(VoidNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
	}

	@Override
	public void visit(TextNode node) {
		if (node.isSelected())
			node.getWorkspace().removeSelectedNode(node, userOpeCmd);
	}
}
