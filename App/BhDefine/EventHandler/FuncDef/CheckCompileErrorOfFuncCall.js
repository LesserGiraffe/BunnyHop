(function() {
  if (bhThis.getOriginal() === null) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'function-not-defined'));
    return [errMessage];
  }
  return [];
})();
