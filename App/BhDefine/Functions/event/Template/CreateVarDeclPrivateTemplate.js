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
  let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
  let assignStat = bhCommon.genBhNode(assignStatID, bhNodeTemplates, bhUserOpeCmd);
  let leftVar = assignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpeCmd);

  let literal = bhCommon.genBhNode(
    listDeclToLiteral[bhThis.getSymbolName()], bhNodeTemplates, bhUserOpeCmd);
  let rightExp = assignStat.findSymbolInDescendants('*', 'RightExp', '*');
  rightExp.replace(literal, bhUserOpeCmd);
  return assignStat;
}

function genNumAddAssignStat() {
  let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
  let NumAddAssignStat = bhCommon.genBhNode('idNumAddAssignStat', bhNodeTemplates, bhUserOpeCmd);
  let leftVar = NumAddAssignStat.findSymbolInDescendants('*', 'LeftVar', '*');
  leftVar.replace(variable, bhUserOpeCmd);
  return NumAddAssignStat;
}

(function() {
  let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
  templates = [variable, genAssignStat()];
  if (String(bhThis.getSymbolName()) === 'NumVarDecl')
    templates.push(genNumAddAssignStat());
  
  return templates;
})();
