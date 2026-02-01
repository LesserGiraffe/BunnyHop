(function() {
  let argument = bhThis.findDescendantOf('*', 'Arg', '*');
  let argName = String(argument.getSymbolName());
  let voidNodes = [
    'NumExpVoid',   'NumVarVoid',   'NumListExpVoid',   'NumListVoid',
    'StrExpVoid',   'StrVarVoid',   'StrListExpVoid',   'StrListVoid',
    'BoolExpVoid',  'BoolVarVoid',  'BoolListExpVoid',  'BoolListVoid',
    'ColorExpVoid', 'ColorVarVoid', 'ColorListExpVoid', 'ColorListVoid',
    'SoundExpVoid', 'SoundVarVoid', 'SoundListExpVoid', 'SoundListVoid'];
  return voidNodes.includes(argName)
      ? [bhTextDb.get('node', 'compile-error', 'argument-not-specified')] : [];
})();
