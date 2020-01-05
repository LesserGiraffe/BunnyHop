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
package net.seapanda.bunnyhop.message;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * MsgTransporterが送信するデータ
 * @author K.Koike
 * */
public class MsgData {

	public final BhNode node;
	public final BhNodeView nodeView;
	public final Vec2D vec2d;
	public final Pair<String, String> strPair;
	public final Pair<Vec2D, Vec2D> vec2dPair;
	public final Workspace workspace;
	public final WorkspaceView workspaceView;
	public final boolean bool;
	public final UserOperationCommand userOpeCmd;
	public final String text;
	public final BhNodeSelectionView nodeSelectionView;

	public MsgData() {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(BhNodeView view) {
		this.node = null;
		this.nodeView = view;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(Vec2D vec2d) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = vec2d;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(Workspace workspace, WorkspaceView workspaceView, UserOperationCommand userOpeCmd) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = workspace;
		this.workspaceView = workspaceView;
		this.bool = false;
		this.userOpeCmd = userOpeCmd;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(BhNode node, BhNodeView view) {
		this.node = node;
		this.nodeView = view;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(BhNode node) {
		this.node = node;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}


	public MsgData(boolean bool) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = bool;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(UserOperationCommand userOpeCmd) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = userOpeCmd;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(String text) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = text;
		this.nodeSelectionView = null;
	}

	public MsgData(Workspace workspace) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = workspace;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(boolean bool, String text) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = bool;
		this.userOpeCmd = null;
		this.text = text;
		this.nodeSelectionView = null;
	}

	public MsgData(BhNodeSelectionView nodeSelectionView) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = nodeSelectionView;
	}

	public MsgData(String textA, String textB) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = new Pair<>(textA, textB);
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(boolean bool, UserOperationCommand userOpeCmd) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = bool;
		this.userOpeCmd = userOpeCmd;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(Vec2D vecA, Vec2D vecB) {
		this.node = null;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = new Pair<Vec2D, Vec2D> (vecA, vecB);
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionView = null;
	}

	public MsgData(BhNode node, UserOperationCommand userOpeCmd) {
		this.node = node;
		this.nodeView = null;
		this.vec2d = null;
		this.strPair = null;
		this.vec2dPair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = userOpeCmd;
		this.text = null;
		this.nodeSelectionView = null;
	}
}






































