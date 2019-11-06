(function() {
	// true -> スコープ外, false -> スコープ内
	
	let original = bhThis.getOriginal();
	if (original === null)
		return true;
	
	// オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
	let rootOfOriginal = original.findRootNode();
	let varDeclSctnInRoot = rootOfOriginal.findSymbolInDescendants('VarDeclSctn');
	if (varDeclSctnInRoot !== null)
		return false;
	
	let scope = original.findSymbolInAncestors('VarScopeSctn', 1, true);
	if (scope === null)
		return true;

	return !bhThis.isDescendantOf(scope);
})();
