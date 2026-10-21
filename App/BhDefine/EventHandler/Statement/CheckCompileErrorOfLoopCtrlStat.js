(function() {
  // true -> ループ文の外, false -> ループ文の中 or ルート
  let isOutOfLoop = bhThis.findAncestor(1, true, 'LoopStat') === null;
  if (isOutOfLoop) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'out-of-loop'));
    return [errMessage];
  }
  return [];
})();
