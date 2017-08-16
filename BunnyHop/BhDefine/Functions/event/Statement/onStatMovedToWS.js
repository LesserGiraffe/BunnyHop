(function() {
	bhCommon.appendRemovedNode(bhReplacedNewNode, bhThis, bhManuallyReplaced, bhNodeHandler, bhUserOpeCmd);
	
	let loopCtrlStatList = [];
	bhThis.findSymbolInDescendants(1, true, loopCtrlStatList, 'BreakStat', 'ContinueStat');
	for (let i = 0; i < loopCtrlStatList.length; ++i) {
		bhNodeHandler.deleteNode(loopCtrlStatList[i], bhUserOpeCmd);
	}
	
})();
