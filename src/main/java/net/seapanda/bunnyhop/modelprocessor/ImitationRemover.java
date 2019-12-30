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
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードをオリジナルノードのイミテーションノードリストから取り除くクラス
 * @author K.Koike
 */
public class ImitationRemover implements BhModelProcessor {

	private UserOperationCommand userOpeCmd;	//!< undo用コマンドオブジェクト

	/**
	 * 引数で指定したノード以下にあるイミテーションノードをオリジナルノードのイミテーションノードリストから取り除く.
	 * @param node このノード以下のイミテーションノードをイミテーションノードリストから取り除く.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public static void remove(BhNode node, UserOperationCommand userOpeCmd) {
		node.accept(new ImitationRemover(userOpeCmd));
	}

	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private ImitationRemover(UserOperationCommand userOpeCmd) {
		this.userOpeCmd = userOpeCmd;
	}

	/**
	 * node の削除処理を行う
	 * @param node 削除するノード
	 * */
	@Override
	public void visit(ConnectiveNode node) {

		//このノードがイミテーションノードだった場合, オリジナルにイミテーションが消えたことを伝える
		if (node.isImitationNode())
			node.getOriginal().disconnectOrgImitRelation(node, userOpeCmd);

		node.sendToSections(this);
	}

	@Override
	public void visit(TextNode node) {

		//このノードがイミテーションノードだった場合, オリジナルにイミテーションが消えたことを伝える
		if (node.isImitationNode())
			node.getOriginal().disconnectOrgImitRelation(node, userOpeCmd);
	}
}
