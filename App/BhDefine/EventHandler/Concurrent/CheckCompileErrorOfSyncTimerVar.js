(function() {
  if (bhThis.getOriginal() === null) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'sync-timer-not-declared'));
    return [errMessage];
  }
  return [];
})();
