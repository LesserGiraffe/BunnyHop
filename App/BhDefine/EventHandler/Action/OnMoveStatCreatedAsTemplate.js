(function() {
  let moveSpeed = bhThis.findDescendantOf('*', 'Arg0', '*', '*', 'Literal', '*');
  let moveTime = bhThis.findDescendantOf('*', 'Arg1', '*', '*', 'Literal', '*');
  moveSpeed.setText('2');
  moveTime.setText('1');
})();
