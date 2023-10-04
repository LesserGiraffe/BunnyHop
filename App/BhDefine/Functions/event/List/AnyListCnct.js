(function() {
	let name = String(bhNodeToConnect.getSymbolName());
	return 'NumList'   === name ||
    	   'StrList'   === name ||
    	   'BoolList'  === name ||
    	   'ColorList' === name ||
    	   'SoundList' === name;
})();
