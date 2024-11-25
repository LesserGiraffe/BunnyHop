let dervIdIdentifierName = 'dervIdIdentifierName';


/** 代入ノード作成 */
function genAssignStat() {
  let listDeclToLiteral = {
    'NumVarDecl'   : 'idNumLiteralExp',
    'StrVarDecl'   : 'idStrLiteralExp',
    'BoolVarDecl'  : 'idBoolLiteralExp',
    'ColorVarDecl' : 'idColorLiteralExp',
    'SoundVarDecl' : 'idFreqSoundLiteral'
  };

  let assignStatID = 'idAnyAssignStat';
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let assignStat = bhCommon.createBhNode(assignStatID, bhUserOpe);
  let leftVar = assignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpe);
  let rightExpCnctr = assignStat.findSymbolInDescendants('*', 'RightExp');
  bhCommon.changeDefaultNode(rightExpCnctr, listDeclToLiteral[bhThis.getSymbolName()], bhUserOpe);
  return assignStat;
}

function genNumAddAssignStat() {
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let numAddAssignStat = bhCommon.createBhNode('idNumAddAssignStat', bhUserOpe);
  let leftVar = numAddAssignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpe);
  
  let rightLiteral = numAddAssignStat.findSymbolInDescendants('*', 'RightExp', '*', '*', 'Literal', '*');
  rightLiteral.setText(1);
  return numAddAssignStat;
}

(function() {
  let variable = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  templates = [variable, genAssignStat()];
  if (String(bhThis.getSymbolName()) === 'NumVarDecl')
    templates.push(genNumAddAssignStat());
  
  return templates;
})();
