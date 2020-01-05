(function() {
	
	let CauseOfDeletion = net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;

	if (bhCauseOfDeletion.equals(CauseOfDeletion.INFLUENCE_OF_ORIGINAL_DELETION) ||
		bhCauseOfDeletion.equals(CauseOfDeletion.SELECTED_FOR_DELETION)) {
		bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhNodeHandler, bhUserOpeCmd);
	}

	return true;
})();
