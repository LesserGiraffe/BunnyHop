(function() {
  let fileName = bhThis.findDescendantOf('*', 'Arg0', '*', '*', 'Literal', '*');
  fileName.setText(bhTextDb.get('node', 'record-audio', 'file'));
  let time = bhThis.findDescendantOf('*', 'Arg1', '*', '*', 'Literal', '*');
  time.setText('3');
})();
