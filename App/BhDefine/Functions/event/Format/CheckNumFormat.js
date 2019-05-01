(function() {

	try {
		java.lang.Double.parseDouble(bhText);
		let lastChar = bhText.slice(-1);
		return (lastChar !== 'd' && lastChar !== 'D' && lastChar !== 'f' && lastChar !== 'F');
	}
	catch(e){}

	if (bhText.length >= 2) {
		if (String(bhText.substr(0,2)) === "0x") {
			try {
				java.lang.Integer.parseInt(bhText.substr(2, bhText.length), 16);
				return true;
			}
			catch(e){}
		}
	}
	
	return false;
})();


