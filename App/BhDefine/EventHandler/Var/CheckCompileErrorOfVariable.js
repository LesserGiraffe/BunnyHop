(function() {

  // true -> スコープ外, false -> スコープ内
  function isOutOfScope() {
    let original = bhThis.getOriginal();
    if (original === null) {
      return true;
    }
    // オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
    let rootOfOriginal = original.findRootNode();
    let VarDeclInRoot = rootOfOriginal.findDescendantOf('VarDecl');
    if (VarDeclInRoot !== null) {
      return false;
    }
    let scope = original.findAncestorOf('VarScope', 1, true);
    if (scope === null) {
      return true;
    }
    return !bhThis.isDescendantOf(scope);
  }

  if (isOutOfScope()) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'out-of-scope'));
    return [errMessage];
  }
  return [];
})();
