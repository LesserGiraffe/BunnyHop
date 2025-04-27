(function() {
  // true -> ループ文の外, false -> ループ文の中 or ルート
  return bhThis.findAncestorOf('LoopStat', 1, true) === null;
})();
