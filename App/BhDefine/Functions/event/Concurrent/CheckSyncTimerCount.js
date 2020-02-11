(function() {
	
	let maxCount = 65535;
	try {
		let num = java.lang.Integer.parseInt(bhText);
		return (0 <= num && num <= maxCount);
	}
	catch(e){}

	if (bhText.length >= 2) {
		if (String(bhText.substr(0,2)) === "0x") {
			try {
				let num = java.lang.Integer.parseInt(bhText.substr(2, bhText.length), 16);
				return (0 <= num && num <= maxCount);;
			}
			catch(e){}
		}
	}
	
	return false;
})();


