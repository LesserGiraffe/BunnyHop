(function() {

	let UnscopedNodeCollector = net.seapanda.bunnyhop.modelprocessor.UnscopedNodeCollector;

	function findNodeToBeReplaced(nodeToReconnect, nodeToCheckReplaceability) {

		let parent = nodeToCheckReplaceability.findParentNode();
		if (parent == null)
			return null;

		if (bhCandidateNodeList.contains(parent))
			return findNodeToBeReplaced(nodeToReconnect, parent);

		return nodeToCheckReplaceability;
	}

	// 移動させる外部ノードを探す
	let nodeToReconnect = bhThis.findOuterNode(1);
	if (nodeToReconnect === null)
		return;

	let outersNotToReconnect = ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'];
	let isOuterNotToReconnect = outersNotToReconnect.some(nodeName => nodeName === String(nodeToReconnect.getSymbolName()));
	if (isOuterNotToReconnect)
		return;

	if (bhCandidateNodeList.contains(nodeToReconnect))
		return;

	let nodeToReplace = findNodeToBeReplaced(nodeToReconnect, bhThis);

	// 接続先が無い場合は, ワークスペースへ
	if (nodeToReplace == null) {
		let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
		bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, bhUserOpeCmd);
		let unscopedNodes = UnscopedNodeCollector.collect(nodeToReconnect);
		bhNodeHandler.deleteNodes(unscopedNodes, bhUserOpeCmd);
	}
	else {
		let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
		bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, bhUserOpeCmd);
		bhNodeHandler.exchangeNodes(nodeToReconnect, nodeToReplace, bhUserOpeCmd);
		let unscopedNodes = UnscopedNodeCollector.collect(nodeToReconnect);
		bhNodeHandler.deleteNodes(unscopedNodes, bhUserOpeCmd);
		// 以下の用に, 削除もしくはカット対象のノードから辿れるスコープ外ノードを削除してはいけない.
		// オリジナルの削除時にイミテーションノードは消えるので, ここで消すと重複削除になる.
		// カット時もペースト後にペーストノードから辿れるスコープ外ノードは削除されるので, ここで消す必要はない.
		// let unscopedNodes = UnscopedNodeCollector.collect(nodeToReplace);
		// bhNodeHandler.deleteNodes(unscopedNodes, bhUserOpeCmd);
	}
})();
