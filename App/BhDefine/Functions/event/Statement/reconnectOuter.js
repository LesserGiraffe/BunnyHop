(function() {

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
	}
	else {
		let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
		bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, bhUserOpeCmd);
		bhNodeHandler.exchangeNodes(nodeToReconnect, nodeToReplace, bhUserOpeCmd);
	}
})();
