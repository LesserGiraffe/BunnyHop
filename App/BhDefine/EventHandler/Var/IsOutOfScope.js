(function() {
  // true -> スコープ外, false -> スコープ内
  
  let original = bhThis.getOriginal();
  if (original === null)
    return true;
  
  // オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
  let rootOfOriginal = original.findRootNode();
  let varDeclSctnInRoot = rootOfOriginal.findDescendantOf('VarDeclSctn');
  if (varDeclSctnInRoot !== null)
    return false;
  
  let scope = original.findAncestorOf('VarScopeSctn', 1, true);
  if (scope === null)
    return true;

  return !bhThis.isDescendantOf(scope);
})();
