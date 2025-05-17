(function() {
  let msg = bhThis.findDescendantOf('*', 'Arg0', '*', '*', 'Literal', '*');
  msg.setText(bhTextDb.get('node', 'scan-exp', 'prompt-text'));
})();
  