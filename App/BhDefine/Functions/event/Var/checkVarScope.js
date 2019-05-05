(function() {

	// オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
	let rootOfOriginal = bhThis.getOriginalNode().findRootNode();
	let varDeclSctnInRoot = rootOfOriginal.findSymbolInDescendants('VarDeclSctn');
	if (varDeclSctnInRoot !== null)
		return true;
	
	let scope = bhThis.getOriginalNode().findSymbolInAncestors('VarScopeSctn', 1, true);
	if (scope === null)
		return false;

	return bhThis.isDescendantOf(scope);
})();
