let imitIdVar = 'imitIdVar';
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
  let variable = bhCommon.buildImitation(bhThis, imitIdVar, bhUserOpeCmd);
  let assignStat = bhCommon.genBhNode(assignStatID, bhNodeTemplates, bhUserOpeCmd);
  let leftVar = assignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpeCmd);

  let literal = bhCommon.genBhNode(
    listDeclToLiteral[bhThis.getSymbolName()], bhNodeTemplates, bhUserOpeCmd);
  let rightExp = assignStat.findSymbolInDescendants('*', 'RightExp', '*');
  rightExp.replace(literal, bhUserOpeCmd);
  literal.getParentConnector().setDefaultNodeId(literal.getId());
  return assignStat;
}

function genNumAddAssignStat() {
  let variable = bhCommon.buildImitation(bhThis, imitIdVar, bhUserOpeCmd);
  let numAddAssignStat = bhCommon.genBhNode('idNumAddAssignStat', bhNodeTemplates, bhUserOpeCmd);
  let leftVar = numAddAssignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpeCmd);
  
  let rightLiteral = numAddAssignStat.findSymbolInDescendants('*', 'RightExp', '*', '*', 'Literal', '*');
  rightLiteral.setText(1);
  return numAddAssignStat;
}

(function() {
  let variable = bhCommon.buildImitation(bhThis, imitIdVar, bhUserOpeCmd);
  templates = [variable, genAssignStat()];
  if (String(bhThis.getSymbolName()) === 'NumVarDecl')
    templates.push(genNumAddAssignStat());
  
  return templates;
})();
