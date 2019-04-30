(function() {

	try {
		let num = java.lang.Integer.parseInt(bhText);
		return (num >= 1);
	}
	catch(e){}

	if (bhText.length >= 2) {
		if (String(bhText.substr(0,2)) === "0x") {
			try {
				let num = java.lang.Integer.parseInt(bhText.substr(2, bhText.length), 16);
				return (num >= 1);
			}
			catch(e){}
		}
	}
	
	return false;
})();


