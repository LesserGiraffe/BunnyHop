(function() {
  let duration = bhThis.findDescendantOf('*', 'Duration', '*', '*', 'Literal', '*');
  let freq = bhThis.findDescendantOf('*', 'Frequency', '*', '*', 'Literal', '*');
  duration.setText('1');
  freq.setText('500');
})();
