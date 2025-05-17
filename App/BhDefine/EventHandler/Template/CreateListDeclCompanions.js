let dervIdIdentifierName = 'dervIdIdentifierName';

let listDeclToGetStat = {
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
  'SoundListDecl' : 'idFreqSoundLiteral'
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

function genNodeToReplace(symbolMaps) {
  nodes = [];
  for (let symbolMap of symbolMaps) {
    nodes.push(
      bhCommon.createBhNode(symbolMap[bhThis.getSymbolName()], bhUserOpe));
  }
  return nodes;
}

function genAnyArrayControlNode(arrayCtrlNodeId, argNames, symbolMaps) {
  let arrayCtrlNode = bhCommon.createBhNode(arrayCtrlNodeId, bhUserOpe);
  let arg = arrayCtrlNode.findDescendantOf('*', 'Arg0', '*');
  let newNode = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  arg.replace(newNode, bhUserOpe);

  for (let i = 0; i < argNames.length; ++i) {
    cnctr = arrayCtrlNode.findDescendantOf('*', argNames[i]);
    bhCommon.changeDefaultNode(cnctr, symbolMaps[i][bhThis.getSymbolName()], bhUserOpe);
  }
  return arrayCtrlNode;
}

(function() {
  let list = bhCommon.buildDerivative(bhThis, dervIdIdentifierName, bhUserOpe);
  let templates = [
    list,
    genAnyArrayControlNode('idAnyArrayToStrExp', [], []),
    genAnyArrayControlNode('idAnyArrayPushStat', ['Arg1'], [listDeclToLiteral]),
    genAnyArrayControlNode(listDeclToGetStat[bhThis.getSymbolName()], [], []),
    genAnyArrayControlNode('idAnyArrayLengthExp', [], []),
    genAnyArrayControlNode('idAnyArrayInsertStat', ['Arg2'], [listDeclToLiteral]),
    genAnyArrayControlNode('idAnyArraySetStat', ['Arg2'], [listDeclToLiteral]),
    genAnyArrayControlNode('idAnyArrayAppendStat', ['Arg2'], [listDeclToEmptyList]),
    genAnyArrayControlNode('idAnyArraySpliceStat', [], []),
    genAnyArrayControlNode('idAnyArrayIndexOfExp', ['Arg1'], [listDeclToLiteral]),
    genAnyArrayControlNode('idAnyArrayIncludesExp', ['Arg1'], [listDeclToLiteral]),
    genAnyArrayControlNode('idAnyArrayReverseStat', [], [])
  ];

  if (String(bhThis.getSymbolName()) === 'SoundListDecl') {
    templates.splice(2, 0, genAnyArrayControlNode('idPlaySoundListStat',[], []));
  }
  if (String(bhThis.getSymbolName()) === 'NumListDecl'
    || String(bhThis.getSymbolName()) === 'StrListDecl') {
    templates.push(genAnyArrayControlNode(listDeclToMinMaxExp[bhThis.getSymbolName()], [], []));
    templates.push(genAnyArrayControlNode(listDeclToSortStat[bhThis.getSymbolName()], [], []));
  }
  return templates;
})();
