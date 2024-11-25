(function() {
  let duration = bhThis.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
  let freq = bhThis.findSymbolInDescendants('*', 'Frequency', '*', '*', 'Literal', '*');
  duration.setText('1');
  freq.setText('500');
})();
