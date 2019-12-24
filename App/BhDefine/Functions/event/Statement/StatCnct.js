(function() {

	let newNodeName = String(bhReplacedNewNode.getSymbolName());
	let section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = String(section.getSymbolName());
	}

	return sectionName === 'StatSctn' ||
		   newNodeName === 'BreakStat'||
		   newNodeName === 'ContinueStat';
})();