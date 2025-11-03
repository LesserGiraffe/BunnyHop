(function() {
  let fileName = bhThis.findDescendantOf('*', 'Arg0', '*', '*', 'Literal', '*');
  fileName.setText(bhTextDb.get('node', 'delete-audio-file', 'file'));
})();
