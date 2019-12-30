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

import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.VoidNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ノードを貼り付け候補から取り除くクラス
 * @author K.Koike
 */
public class PasteCanceler implements BhModelProcessor {

	private final UserOperationCommand userOpeCmd;	//!< undo 用コマンドオブジェクト

	/**
	 * 引数で指定したノード以下のノードを貼り付け候補から取り除く
	 * @param node このノード以下のノードを貼り付け候補から取り除く
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public static void cancel(BhNode node, UserOperationCommand userOpeCmd) {
		node.accept(new PasteCanceler(userOpeCmd));
	}

	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private PasteCanceler(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}

	@Override
	public void visit(ConnectiveNode node) {

		MsgService.INSTANCE.removeFromPasteList(node, userOpeCmd);
		node.sendToSections(this);
	}

	@Override
	public void visit(VoidNode node) {
		MsgService.INSTANCE.removeFromPasteList(node, userOpeCmd);
	}

	@Override
	public void visit(TextNode node) {
		MsgService.INSTANCE.removeFromPasteList(node, userOpeCmd);
	}
}
