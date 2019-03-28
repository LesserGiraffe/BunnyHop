(function() {

	let newNodeName = String(bhReplacedNewNode.getSymbolName());
	if (newNodeName === 'BreakStat' ||
		newNodeName === 'ContinueStat') {	//Break and Continue must be under the loop statement.
		return bhReplacedOldNode.findSymbolInAncestors('LoopStat', 1, true) !== null;
	}

	let section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = String(section.getSymbolName());
	}

	return sectionName === 'StatSctn';
})();