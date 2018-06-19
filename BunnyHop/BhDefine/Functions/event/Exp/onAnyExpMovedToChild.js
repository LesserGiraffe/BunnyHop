(function() {

	let cnctr = bhThis.findSymbolInAncestors('*', 1, false);
	let staticTypeNodeID = bhCommon.getStaticTypeNodeID(bhThis.getSymbolName(), cnctr.getClaz().toString());

	if (staticTypeNodeID !== null) {
		let posOnWS = bhMsgService.getPosOnWS(bhThis);
		let staticTypeNode = bhCommon.addNewNodeToWS(
			staticTypeNodeID,
			bhThis.getWorkspace(),
			posOnWS,
			bhNodeHandler,
			bhNodeTemplates,
			bhUserOpeCmd);
		bhNodeHandler.exchangeNodes(bhThis, staticTypeNode, bhUserOpeCmd);
		bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
	}
})();










