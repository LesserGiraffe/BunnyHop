(function() {
    let fileName = bhThis.findDescendantOf('*', 'Arg1', '*', '*', 'Literal', '*');
    fileName.setText(bhTextDb.get('node', 'save-text', 'file'));
})();
