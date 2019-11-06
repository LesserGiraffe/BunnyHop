(function() {
	
	let CauseOfDeletion = net.seapanda.bunnyhop.model.node.CauseOfDeletion;

	if (bhCauseOfDeletion.eq(CauseOfDeletion.INFLUENCE_OF_ORIGINAL_DELETION) ||
		bhCauseOfDeletion.eq(CauseOfDeletion.SELECTED_FOR_DELETION)) {
		bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhNodeHandler, bhUserOpeCmd);
	}
})();
