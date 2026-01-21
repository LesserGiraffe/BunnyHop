let dervIdIdentifierName = 'dervIdIdentifierName';


/** 代入ノード作成 */
function genAssignStat() {
  let listDeclToLiteral = {
    'NumVarDecl'   : 'idNumLiteralExp',
    'StrVarDecl'   : 'idStrLiteralExp',
    'BoolVarDecl'  : 'idBoolLiteralExp',
    'ColorVarDecl' : 'idColorLiteralExp',
    'SoundVarDecl' : 'idFreqSoundLiteralExp'
  };

  let assignStatID = 'idAnyAssignStat';
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let assignStat = bhCommon.createBhNode(assignStatID, bhUserOpe);
  let leftVar = assignStat.findDescendantOf('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpe);
  let rightExpCnctr = assignStat.findDescendantOf('*', 'RightExp');
  bhCommon.changeDefaultNode(rightExpCnctr, listDeclToLiteral[bhThis.getSymbolName()], bhUserOpe);
  return assignStat;
}

function genAddAssignStat(addAssignStatId, text) {
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let addAssignStat = bhCommon.createBhNode(addAssignStatId, bhUserOpe);
  let leftVar = addAssignStat.findDescendantOf('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpe);
  
  let rightLiteral = addAssignStat.findDescendantOf('*', 'RightExp', '*', '*', 'Literal', '*');
  if (text) {
    rightLiteral.setText(text);
  }
  return addAssignStat;
}

(function() {
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let templates = [variable, genAssignStat()];
  let varDeclName = String(bhThis.getSymbolName());
  if (varDeclName === 'NumVarDecl')
    templates.push(genAddAssignStat('idNumAddAssignStat', 1));
  else if (varDeclName === 'StrVarDecl')
    templates.push(genAddAssignStat('idStrAddAssignStat'));

  return templates;
})();
