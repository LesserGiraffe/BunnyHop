(function() {

	let underLoop = bhThis.findSymbolInAncestors('LoopStat', 1, true) !== null;
	if (!underLoop) {	//The new Connection point is under the loop stat.
		let loopCtrlStatList = [];
		bhThis.findSymbolInDescendants(1, true, loopCtrlStatList, 'BreakStat', 'ContinueStat');
		for (let i = 0; i < loopCtrlStatList.length; ++i) {
			bhNodeHandler.deleteNode(loopCtrlStatList[i], bhUserOpeCmd);
		}
	}
})();
