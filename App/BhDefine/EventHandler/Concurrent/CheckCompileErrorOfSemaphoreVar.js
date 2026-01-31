(function() {
  if (bhCommon.isTemplateNode(bhThis)) {
    return []
  }
  if (bhThis.getOriginal() === null) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'semaphore-not-declared'));
    return [errMessage];
  }
  return [];
})();
