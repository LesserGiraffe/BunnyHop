(function() {

	const newNodeName = bhReplacedNewNode.getSymbolName();
	const section = bhReplacedNewNode.findSymbolInDescendants('*');
	let sectionName = null;
	if (section !== null) {
		sectionName = section.getSymbolName();
	}
    
	return newNodeName === 'NumLiteral' ||
		   newNodeName === 'StrLiteral' ||
		   newNodeName === 'LineFeed' ||
		   newNodeName === 'BoolLiteral' ||
		   newNodeName === 'FreqSoundLiteral' ||
		   newNodeName === 'ScaleSoundLiteral' ||
		   newNodeName === 'ColorLiteral' ||
		   
		   sectionName === 'NumberExpSctn' ||
		   sectionName === 'StringExpSctn' ||
		   sectionName === 'BooleanExpSctn' ||
		   sectionName === 'SoundExpSctn' ||
		   sectionName === 'ColorExpSctn';
})();