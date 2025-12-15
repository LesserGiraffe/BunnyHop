(function() {
  let moveSpeed = bhThis.findDescendantOf('*', 'Arg0', '*', '*', 'Literal', '*');
  let moveTime = bhThis.findDescendantOf('*', 'Arg1', '*', '*', 'Literal', '*');
  moveSpeed.setText('3');
  moveTime.setText('1');
})();
