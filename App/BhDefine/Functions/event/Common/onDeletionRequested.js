(function() {
	
	let CauseOfDletion = net.seapanda.bunnyhop.model.node.CauseOfDletion;

	if (bhCauseOfDeletion.eq(CauseOfDletion.INFLUENCE_OF_ORIGINAL_DELETION) ||
		bhCauseOfDeletion.eq(CauseOfDletion.SELECTED_FOR_DELETION)) {
		bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhNodeHandler, bhUserOpeCmd);
	}
})();
