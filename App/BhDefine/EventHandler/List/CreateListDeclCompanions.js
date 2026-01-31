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

let listDeclToEmptyList = {
  'NumListDecl'   : 'idNumEmptyList',
  'StrListDecl'   : 'idStrEmptyList',
  'BoolListDecl'  : 'idBoolEmptyList',
  'ColorListDecl' : 'idColorEmptyList',
  'SoundListDecl' : 'idSoundEmptyList'
};

let listDeclToMinMaxExp = {
  'NumListDecl'   : 'idNumArrayMaxMinExp',
  'StrListDecl'   : 'idStrArrayMaxMinExp'};

let listDeclToSortStat = {
  'NumListDecl'   : 'idNumArraySortStat',
  'StrListDecl'   : 'idAnyArraySortStat'};

function genAnyArrayControlNode(arrayCtrlNodeId, listCnctrName, argNameToSymbolMap) {
  let arrayCtrlNode = bhCommon.createBhNode(arrayCtrlNodeId, bhUserOpe);
  let arg = arrayCtrlNode.findDescendantOf('*', listCnctrName, '*');
  let newNode = bhCommon.buildDerivative(bhThis, dervIdIdVar, bhUserOpe);
  arg.replace(newNode, bhUserOpe);

  if (argNameToSymbolMap === undefined) {
    return arrayCtrlNode;
  }
  for (let argName in argNameToSymbolMap) {
    cnctr = arrayCtrlNode.findDescendantOf('*', argName);
    let defaultNodeId = argNameToSymbolMap[argName][listDeclName];
    bhCommon.changeDefaultNode(cnctr, defaultNodeId, bhUserOpe);
  }
  return arrayCtrlNode;
}

(function() {
  let list = bhCommon.buildDerivative(bhThis, dervIdIdVar, bhUserOpe);
  let nameOfGetItem = listDeclToGetExp[listDeclName];
  let templates = [
    list,
    genAnyArrayControlNode('idAnyArrayToStrExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayPushStat', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode(listDeclToGetExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArrayLengthExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayInsertStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArraySetStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayAppendStat', 'Arg0', {'Arg2' : listDeclToEmptyList}),
    genAnyArrayControlNode('idAnyArraySpliceStat', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayIndexOfExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayIncludesExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayReverseStat', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayAssignStat', 'LeftVar', {'RightExp' : listDeclToEmptyList}),
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
