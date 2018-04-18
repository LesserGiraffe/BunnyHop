(function() {

	const newNodeName = bhReplacedNewNode.getSymbolName();
	const section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = section.getSymbolName();
	}

	return newNodeName === 'FreqSoundLiteral' ||
		   newNodeName === 'ScaleSoundLiteral' ||
		   sectionName === 'SoundExpSctn' ||
		   sectionName === 'AnyExpSctn';
})();