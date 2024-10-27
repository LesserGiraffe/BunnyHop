let imitIdList = 'imitIdList';

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

function genNodeToReplace(symbolMaps) {
  nodes = [];
  for (let symbolMap of symbolMaps) {
    nodes.push(
      bhCommon.genBhNode(symbolMap[bhThis.getSymbolName()], bhNodeTemplates, bhUserOpeCmd));
  }
  return nodes;
}

function genAnyArrayControlNode(arrayCtrlNodeId, argNames, symbolMaps) {
  let arrayCtrlNode = bhCommon.genBhNode(arrayCtrlNodeId, bhNodeTemplates, bhUserOpeCmd);
  argNames = ['Arg0'].concat(argNames);
  newNodes = [bhCommon.buildImitation(bhThis, imitIdList, bhUserOpeCmd)]
    .concat(genNodeToReplace(symbolMaps));

  for (let i = 0; i < argNames.length; ++i) {
    let arg = arrayCtrlNode.findSymbolInDescendants('*', argNames[i], '*');
    arg.replace(newNodes[i], bhUserOpeCmd);
    newNodes[i].getParentConnector().setDefaultNodeId(newNodes[i].getId());
  }
  return arrayCtrlNode;
}

(function() {
  let list = bhCommon.buildImitation(bhThis, imitIdList, bhUserOpeCmd);
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
  return templates;
})();
