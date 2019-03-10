(function() {

	const section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = section.getSymbolName();
	}

	return sectionName === 'NumberExpSctn' ||
		   sectionName === 'AnyExpSctn';
})();
