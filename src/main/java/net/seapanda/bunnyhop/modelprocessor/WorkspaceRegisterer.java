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
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ノードに対してワークスペースをセットする
 * @author K.Koike
 */
public class WorkspaceRegisterer implements BhModelProcessor {

	private final Workspace ws;	//!< 登録されるワークスペース
	private final UserOperationCommand userOpeCmd;

	/**
	 * 引数で指定したノード以下のノードに引数で指定したワークスペースを登録する
	 * @param node これ以下のノードにワークスペースを登録する
	 * @param ws 登録するワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public static void register(BhNode node, Workspace ws, UserOperationCommand userOpeCmd) {
		var registerer = new WorkspaceRegisterer(ws, userOpeCmd);
		node.accept(registerer);
	}

	/**
	 * 引数で指定したノード以下のノードのワークスペースの登録を解除する
	 * @param node このノード以下のノードのワークスペースの登録を解除する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public static void deregister(BhNode node, UserOperationCommand userOpeCmd) {
		var registerer = new WorkspaceRegisterer(null, userOpeCmd);
		node.accept(registerer);
	}

	/**
	 * @param ws 登録されるワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private WorkspaceRegisterer(Workspace ws, UserOperationCommand userOpeCmd) {
		this.ws = ws;
		this.userOpeCmd = userOpeCmd;
	}

	@Override
	public void visit(ConnectiveNode node) {
		node.setWorkspace(ws, userOpeCmd);
		node.sendToSections(this);
	}

	@Override
	public void visit(TextNode node) {
		node.setWorkspace(ws, userOpeCmd);
	}
}
