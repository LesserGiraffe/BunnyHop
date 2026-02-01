let listDeclName = String(bhThis.getSymbolName());
let dervIdIdVar = 'dervIdVar';

let listDeclToGetExp = {
  'NumListDecl'   : 'idNumArrayGetExp',
  'StrListDecl'   : 'idStrArrayGetExp',
  'BoolListDecl'  : 'idBoolArrayGetExp',
  'ColorListDecl' : 'idColorArrayGetExp',
  'SoundListDecl' : 'idSoundArrayGetExp'};

let listDeclToLiteral = {
  'NumListDecl'   : 'idNumLiteralExp',
  'StrListDecl'   : 'idStrLiteralExp',
  'BoolListDecl'  : 'idBoolLiteralExp',
  'ColorListDecl' : 'idColorLiteralExp',
  'SoundListDecl' : 'idFreqSoundLiteralExp'
};

let listDeclToExpVoid = {
  'NumListDecl'   : 'idNumListExpVoid',
  'StrListDecl'   : 'idStrListExpVoid',
  'BoolListDecl'  : 'idBoolListExpVoid',
  'ColorListDecl' : 'idColorListExpVoid',
  'SoundListDecl' : 'idSoundListExpVoid'
};

let listDeclToMinMaxExp = {
  'NumListDecl'   : 'idNumArrayMaxMinExp',
  'StrListDecl'   : 'idStrArrayMaxMinExp'};

let listDeclToSortStat = {
  'NumListDecl'   : 'idNumArraySortStat',
  'StrListDecl'   : 'idAnyArraySortStat'};

function genAnyArrayControlNode(arrayCtrlNodeId, listCnctrName, argNameToSymbolMap) {
  let arrayCtrlNode = bhUtil.createBhNode(arrayCtrlNodeId, bhUserOpe);
  let arg = arrayCtrlNode.findDescendantOf('*', listCnctrName, '*');
  let newNode = bhUtil.buildDerivative(bhThis, dervIdIdVar, bhUserOpe);
  arg.replace(newNode, bhUserOpe);

  if (argNameToSymbolMap === undefined) {
    return arrayCtrlNode;
  }
  for (let argName in argNameToSymbolMap) {
    cnctr = arrayCtrlNode.findDescendantOf('*', argName);
    let defaultNodeId = argNameToSymbolMap[argName][listDeclName];
    bhUtil.changeDefaultNode(cnctr, defaultNodeId, bhUserOpe);
  }
  return arrayCtrlNode;
}

(function() {
  let list = bhUtil.buildDerivative(bhThis, dervIdIdVar, bhUserOpe);
  let nameOfGetItem = listDeclToGetExp[listDeclName];
  let templates = [
    list,
    genAnyArrayControlNode('idAnyArrayToStrExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayPushStat', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode(listDeclToGetExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArrayLengthExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayInsertStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArraySetStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayAppendStat', 'Arg0', {'Arg2' : listDeclToExpVoid}),
    genAnyArrayControlNode('idAnyArraySpliceStat', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayIndexOfExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayIncludesExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayReverseStat', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayAssignStat', 'LeftVar', {'RightExp' : listDeclToExpVoid}),
  ];

  if (listDeclName === 'SoundListDecl') {
    templates.splice(2, 0, genAnyArrayControlNode('idPlaySoundListStat', 'Arg0'));
  }
  if (listDeclName === 'NumListDecl' || listDeclName=== 'StrListDecl') {
    templates.push(genAnyArrayControlNode(listDeclToMinMaxExp[listDeclName], 'Arg0'));
    templates.push(genAnyArrayControlNode(listDeclToSortStat[listDeclName], 'Arg0'));
  }
  return templates;
})();
