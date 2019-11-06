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
package net.seapanda.bunnyhop.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;

import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNodeViewType;
import net.seapanda.bunnyhop.model.node.SyntaxSymbolID;
import net.seapanda.bunnyhop.modelprocessor.CallbackInvoker;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * @author K.Koike
 * 全ノードの構造を保存するスナップショット
 */
public class NodeGraphSnapshot {

	private final Set<BhNode> rootNodeSet;	//!< スナップショット作成時の全ルートノードのコピー
	private final WorkspaceSet wss;
	/**
	 * SyntaxSymbolID と対応する BhNodeView のマップ. <br>
	 * BhNodeView はスナップショットではなく, オリジナルのモデル (BhNode) に対応するビューオブジェクト.
	 */
	private final Map<SyntaxSymbolID, BhNodeView> symbolIdToNodeView;
	private final Map<SyntaxSymbolID, BhNode> symbolIdToNode;	//!< SyntaxSymbolID とそれに対応する BhNode のマップ.

	/**
	 * 引数で指定したワークスペースセットにある全ノードの構造を保持するスナップショットを構築する
	 * @param wss 保存するノードを含むワークスペースセット
	 */
	public NodeGraphSnapshot(WorkspaceSet wss) {

		this.wss = wss;
		HashSet<BhNode> originalRootNodeSet = collectRootNodes(wss);
		symbolIdToNodeView = collectNodeView(originalRootNodeSet);
		rootNodeSet = SerializationUtils.clone(originalRootNodeSet);
		symbolIdToNode = collectNode(rootNodeSet);
	}

	/**
	 * ルートノードを集めて返す
	 */
	private HashSet<BhNode> collectRootNodes(WorkspaceSet wss) {

		var rootNodes = wss.getWorkspaceList().stream()
			.flatMap(ws -> ws.getRootNodeList().stream())
			.collect(Collectors.toSet());
		return new HashSet<BhNode>(rootNodes);
	}

	/**
	 * 引数のノードリストから辿れるノードのノードビューをシンボルIDと共に集めて返す
	 */
	private Map<SyntaxSymbolID, BhNodeView> collectNodeView(Collection<BhNode> rootNodeList) {

		var symbolIdToNodeView = new HashMap<SyntaxSymbolID, BhNodeView>();
		var registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(
			node -> {
				if (!node.getType().equals(BhNodeViewType.NO_VIEW)) {
					BhNodeView view = MsgService.INSTANCE.getBhNodeView(node);
					symbolIdToNodeView.put(node.getSymbolID(), view);
				}
			});

		rootNodeList.forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
		return symbolIdToNodeView;
	}

	/**
	 * 引数のノードリストから辿れるノードをシンボルIDと共に集めて返す
	 */
	private Map<SyntaxSymbolID, BhNode> collectNode(Collection<BhNode> rootNodeList) {

		var symbolIdToNode = new HashMap<SyntaxSymbolID, BhNode>();
		var registry = CallbackInvoker.newCallbackRegistry().setForAllNodes(
			node -> symbolIdToNode.put(node.getSymbolID(), node));

		rootNodeList.forEach(rootNode -> CallbackInvoker.invoke(registry, rootNode));
		return symbolIdToNode;
	}

	/**
	 * スナップショットを取ったノードが存在したワークスペースセットを返す
	 * @return スナップショットを取ったノードが存在したワークスペースセット
	 */
	public WorkspaceSet getWorkspaceSet() {
		return wss;
	}

	/**
	 * 全ルートノードのスナップショットを返す
	 * @return 全ルートノードのスナップショット
	 */
	public Collection<BhNode> getRootNodeList() {
		return new ArrayList<>(rootNodeSet);
	}

	/**
	 * シンボルIDとそれに対応するノードビューのマップを返す. <br>
	 * マップの BhNodeView はスナップショットではなく, オリジナルのモデル (BhNode) に対応するビューオブジェクト.
	 * @return シンボルIDとそれに対応するノードビューのマップ
	 */
	public Map<SyntaxSymbolID, BhNodeView> getMapOfSymbolIdToNodeView() {
		return new HashMap<>(symbolIdToNodeView);
	}

	/**
	 * シンボルIDとそれに対応するノード (スナップショット) のマップを返す.
	 * @return シンボルIDとそれに対応するノード (スナップショット) のマップ
	 */
	public Map<SyntaxSymbolID, BhNode> getMapOfSymbolIdToNode() {
		return new HashMap<>(symbolIdToNode);
	}
}
























