(function() {
	bhCommon.appendRemovedNode(bhReplacedNewNode, bhThis, bhManuallyRemoved, bhNodeHandler, bhUserOpeCmd);

	let loopCtrlStatList = new java.util.ArrayList();
	bhThis.findSymbolInDescendants(1, true, loopCtrlStatList, 'BreakStat', 'ContinueStat');
	for (let i = 0; i < loopCtrlStatList.size(); ++i) {
		let loopStat = loopCtrlStatList.get(i).findSymbolInAncestors('LoopStat', 1, true);
		if (loopStat === null)
			bhNodeHandler.deleteNode(loopCtrlStatList.get(i), bhUserOpeCmd);
	}

})();
