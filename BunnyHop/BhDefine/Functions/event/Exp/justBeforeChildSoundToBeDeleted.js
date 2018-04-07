(function() {

	if (bhNodeToDelete.getParentConnector().getSymbolName() === 'Arg0') {
		let nextMelodyExp = bhThis.findSymbolInDescendants('*', 'Arg1', '*');
		bhNodeHandler.replaceChild(bhThis, nextMelodyExp, bhUserOpeCmd)
		bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
	}
})();
