(function() {
  // true -> ループ文の外, false -> ループ文の中 or ルート
  let isOutOfLoop = bhThis.findAncestorOf('LoopStat', 1, true) === null;
  if (isOutOfLoop) {
    let errMessage = String(bhTextDb.get('node', 'compile-error', 'out-of-loop'));
    return [errMessage];
  }
  return [];
})();
