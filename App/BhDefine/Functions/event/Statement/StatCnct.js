(function() {

	let newNodeName = String(bhReplacedNewNode.getSymbolName());
	let section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = String(section.getSymbolName());
	}
	
	java.lang.System.out.println

	return sectionName === 'StatSctn' ||
		   newNodeName === 'BreakStat'||
		   newNodeName === 'ContinueStat';
})();