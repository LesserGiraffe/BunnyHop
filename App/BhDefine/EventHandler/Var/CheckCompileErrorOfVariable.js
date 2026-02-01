// true -> スコープ外, false -> スコープ内
function isOutOfScope() {
  let original = bhThis.getOriginal();
  // オリジナルノードのルートノードが VarDecl ならオリジナルノードはグローバル宣言されている
  let rootOfOriginal = original.findRootNode();
  let varDeclInRoot = rootOfOriginal.findDescendantOf('VarDecl');
  if (varDeclInRoot !== null) {
    return false;
  }
  let scope = original.findAncestorOf('VarScope', 1, true);
  if (scope === null) {
    return true;
  }
  return !bhThis.isDescendantOf(scope);
}

(function() {
  if (bhThis.getOriginal() === null) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'variable-not-declared'));
    return [errMessage];
  }
  if (isOutOfScope()) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'out-of-scope'));
    return [errMessage];
  }
  return [];
})();
