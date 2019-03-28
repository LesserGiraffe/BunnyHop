(function() {

	let section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = String(section.getSymbolName());
	}
	return sectionName === 'BooleanExpSctn' ||
		   sectionName === 'AnyExpSctn';
})();