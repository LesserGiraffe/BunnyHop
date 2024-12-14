(function() {
  let msg = bhThis.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
  msg.setText(bhTextDb.get('node', 'scan-exp', 'prompt-text'));
})();
  