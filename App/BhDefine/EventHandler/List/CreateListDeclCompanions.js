let listDeclName = String(bhThis.getSymbolName());
let dervIdIdVar = 'dervIdVar';

let listDeclToGetExp = {
  'NumListDecl'   : 'idNumListGetExp',
  'StrListDecl'   : 'idStrListGetExp',
  'BoolListDecl'  : 'idBoolListGetExp',
  'ColorListDecl' : 'idColorListGetExp',
  'SoundListDecl' : 'idSoundListGetExp'
};

let listDeclToSliceExp = {
  'NumListDecl'   : 'idNumListSliceExp',
  'StrListDecl'   : 'idStrListSliceExp',
  'BoolListDecl'  : 'idBoolListSliceExp',
  'ColorListDecl' : 'idColorListSliceExp',
  'SoundListDecl' : 'idSoundListSliceExp'
};

let listDeclToLiteral = {
  'NumListDecl'   : 'idNumLiteralExp',
  'StrListDecl'   : 'idStrLiteralExp',
  'BoolListDecl'  : 'idBoolLiteralExp',
  'ColorListDecl' : 'idColorLiteralExp',
  'SoundListDecl' : 'idSimpleFreqSoundLiteralExp'
};

let listDeclToExpVoid = {
  'NumListDecl'   : 'idNumListExpVoid',
  'StrListDecl'   : 'idStrListExpVoid',
  'BoolListDecl'  : 'idBoolListExpVoid',
  'ColorListDecl' : 'idColorListExpVoid',
  'SoundListDecl' : 'idSoundListExpVoid'
};

let listDeclToMinMaxExp = {
  'NumListDecl'   : 'idNumListMaxMinExp',
  'StrListDecl'   : 'idStrListMaxMinExp'
};

let listDeclToSortExp = {
  'NumListDecl'   : 'idNumListSortExp',
  'StrListDecl'   : 'idStrListSortExp'
};

let listDeclToReverseExp = {
  'NumListDecl'   : 'idNumListReverseExp',
  'StrListDecl'   : 'idStrListReverseExp',
  'BoolListDecl'  : 'idBoolListReverseExp',
  'ColorListDecl' : 'idColorListReverseExp',
  'SoundListDecl' : 'idSoundListReverseExp'
};

let listDeclToPlaySoundListStat = {
  'SoundListDecl' : 'idPlaySoundListStat'
}

function genAnyListControlNode(listCtrlNodeId, listCnctrName, argNameToSymbolMap) {
  if (!listCtrlNodeId) {
    return null;
  }
  let listCtrlNode = bhUtil.createBhNode(listCtrlNodeId, bhUserOpe);
  let arg = listCtrlNode.findDescendantOf('*', listCnctrName, '*');
  let newNode = bhUtil.buildDerivative(bhThis, dervIdIdVar, bhUserOpe);
  arg.replace(newNode, bhUserOpe);

  if (argNameToSymbolMap === undefined) {
    return listCtrlNode;
  }
  for (let argName in argNameToSymbolMap) {
    cnctr = listCtrlNode.findDescendantOf('*', argName);
    let defaultNodeId = argNameToSymbolMap[argName][listDeclName];
    bhUtil.changeDefaultNode(cnctr, defaultNodeId, bhUserOpe);
  }
  return listCtrlNode;
}

(function() {
  let templates = [
    bhUtil.buildDerivative(bhThis, dervIdIdVar, bhUserOpe),
    genAnyListControlNode(listDeclToPlaySoundListStat[listDeclName], 'Arg0'),
    genAnyListControlNode('idAnyListToStrExp', 'Arg0'),
    genAnyListControlNode('idAnyListLengthExp', 'Arg0'),
    genAnyListControlNode('idAnyListPushStat', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyListControlNode(listDeclToGetExp[listDeclName], 'Arg0'),
    genAnyListControlNode('idAnyListSetStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyListControlNode('idAnyListInsertStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyListControlNode('idAnyListInsertAllStat', 'Arg0', {'Arg2' : listDeclToExpVoid}),
    genAnyListControlNode('idAnyListSpliceStat', 'Arg0'),
    genAnyListControlNode(listDeclToMinMaxExp[listDeclName], 'Arg0'),
    genAnyListControlNode('idAnyListIndexOfExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyListControlNode('idAnyListIncludesExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyListControlNode(listDeclToSortExp[listDeclName], 'Arg0'),
    genAnyListControlNode(listDeclToReverseExp[listDeclName], 'Arg0'),
    genAnyListControlNode(listDeclToSliceExp[listDeclName], 'Arg0'),
    genAnyListControlNode('idAnyListAssignStat', 'LeftVar', {'RightExp' : listDeclToExpVoid})
  ];
  return templates.filter(node => node !== null);
})();
