(function() {
  let volume = bhThis.findDescendantOf('*', 'Volume', '*', '*', 'Literal', '*'); 
  let duration = bhThis.findDescendantOf('*', 'Duration', '*', '*', 'Literal', '*');
  let freq = bhThis.findDescendantOf('*', 'Frequency', '*', '*', 'Literal', '*');
  volume.setText('100');
  duration.setText('1');
  if (freq !== null)  {
    freq.setText('500');
  }
})();
