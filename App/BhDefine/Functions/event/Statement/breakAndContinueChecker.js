(function() {
	// true -> ループ文の外, false -> ループ文の中 or ルート
	return bhThis.findSymbolInAncestors('LoopStat', 1, true) === null;
})();
