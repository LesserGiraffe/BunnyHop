(function() {

	let sectionName = String(bhReplacedNewNode.getSymbolName());
	return sectionName === 'NumVar'  ||
		   sectionName === 'StrVar'   ||
		   sectionName === 'BoolVar'  ||
		   sectionName === 'ColorVar' ||
		   sectionName === 'SoundVar';
})();

