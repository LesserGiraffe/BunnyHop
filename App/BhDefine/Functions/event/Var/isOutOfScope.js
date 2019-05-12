(function() {
	// true -> スコープ外, false -> スコープ内
	
	// オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
	let rootOfOriginal = bhThis.getOriginalNode().findRootNode();
	let varDeclSctnInRoot = rootOfOriginal.findSymbolInDescendants('VarDeclSctn');
	if (varDeclSctnInRoot !== null)
		return false;
	
	let scope = bhThis.getOriginalNode().findSymbolInAncestors('VarScopeSctn', 1, true);
	if (scope === null)
		return true;

	return !bhThis.isDescendantOf(scope);
})();
