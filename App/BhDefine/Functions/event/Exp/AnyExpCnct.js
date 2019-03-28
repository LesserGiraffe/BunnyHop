(function() {

	let section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = String(section.getSymbolName());
	}

	return sectionName === 'NumberExpSctn'  ||
		   sectionName === 'StringExpSctn'  ||
		   sectionName === 'BooleanExpSctn' ||
		   sectionName === 'SoundExpSctn'   ||
		   sectionName === 'ColorExpSctn';
})();
