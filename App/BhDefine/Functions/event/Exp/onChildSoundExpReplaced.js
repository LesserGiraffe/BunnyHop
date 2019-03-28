(function() {
	
	let newNodeSection = bhParentConnector.getConnectedNode().findSymbolInDescendants('*');
	let newNodeSectionName = null;
	if (newNodeSection !== null)
		newNodeSectionName = newNodeSection.getSymbolName();

	let nextMelodyExp = bhThis.findSymbolInDescendants('*', 'Arg1', '*');
	let oldNodeSection = bhReplacedOldNode.findSymbolInDescendants('*');
	let oldNodeSectionName = null;
	if (oldNodeSection !== null)
		oldNodeSectionName = oldNodeSection.getSymbolName();

	// 音挿入
	if (String(newNodeSectionName) === 'SoundExpSctn') {
	
		// 新メロディノード作成
		let posOnWS = bhMsgService.getPosOnWS(bhThis);
		let newMelodyExp = bhCommon.addNewNodeToWS('idMelodyExp', bhThis.getWorkspace(), posOnWS, bhNodeHandler, bhNodeTemplates, bhUserOpeCmd);
		bhNodeHandler.exchangeNodes(nextMelodyExp, newMelodyExp, bhUserOpeCmd);
		
		// 新ノードの末尾に旧ノードを追加
		let newMelodyExpNext = newMelodyExp.findSymbolInDescendants('*', 'Arg1', '*');
		bhNodeHandler.exchangeNodes(newMelodyExpNext, nextMelodyExp, bhUserOpeCmd);
		bhNodeHandler.deleteNode(newMelodyExpNext, bhUserOpeCmd);
		
		// 旧音ノードを新メロディノードに繋ぎ直し
		let soundExpVoid = newMelodyExp.findSymbolInDescendants('*', 'Arg0', '*');
		bhNodeHandler.exchangeNodes(soundExpVoid, bhReplacedOldNode, bhUserOpeCmd);
		bhNodeHandler.deleteNode(soundExpVoid, bhUserOpeCmd);
	}
	else if (String(bhParentConnector.getConnectedNode().getSymbolName()) === 'SoundLiteralVoid') {
		if (String(nextMelodyExp.getSymbolName()) === 'MelodyExp') {
			bhNodeHandler.replaceChild(bhThis, nextMelodyExp, bhUserOpeCmd)
			bhNodeHandler.deleteNodeIncompletely(bhThis, true, true, bhUserOpeCmd);
			bhMsgService.setNodeVisibility(bhThis, false, bhUserOpeCmd);
		}
	}
})();
