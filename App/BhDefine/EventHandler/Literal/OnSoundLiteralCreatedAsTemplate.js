(function() {
  let volume = bhThis.findDescendantOf('*', '*', '*', 'Volume', '*', '*', 'Literal', '*');
  let duration = bhThis.findDescendantOf('*', '*', '*', 'Duration', '*', '*', 'Literal', '*');
  let freq = bhThis.findDescendantOf('*', '*', '*', 'Frequency', '*', '*', 'Literal', '*');
  let octave = bhThis.findDescendantOf('*', '*', '*', 'Octave', '*');

  volume.setText('100');
  duration.setText('1');
  if (freq !== null) {
    freq.setText('500');
  }
  if (octave !== null) {
    octave.setText('0');
  }
})();
