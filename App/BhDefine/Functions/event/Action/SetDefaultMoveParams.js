(function() {
  let moveSpeed = bhThis.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
  let moveTime = bhThis.findSymbolInDescendants('*', 'Arg1', '*', '*', 'Literal', '*');
  moveSpeed.setText('2');
  moveTime.setText('1');
})();
  