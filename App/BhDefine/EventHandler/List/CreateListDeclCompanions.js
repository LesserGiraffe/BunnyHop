let listDeclName = String(bhThis.getSymbolName());
let dervIdIdVar = 'dervIdVar';

let listDeclToGetExp = {
  'NumListDecl'   : 'idNumArrayGetExp',
  'StrListDecl'   : 'idStrArrayGetExp',
  'BoolListDecl'  : 'idBoolArrayGetExp',
  'ColorListDecl' : 'idColorArrayGetExp',
  'SoundListDecl' : 'idSoundArrayGetExp'
};

let listDeclToSliceExp = {
  'NumListDecl'   : 'idNumArraySliceExp',
  'StrListDecl'   : 'idStrArraySliceExp',
  'BoolListDecl'  : 'idBoolArraySliceExp',
  'ColorListDecl' : 'idColorArraySliceExp',
  'SoundListDecl' : 'idSoundArraySliceExp'
};

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
  'StrListDecl'   : 'idStrArrayMaxMinExp'
};

let listDeclToSortExp = {
  'NumListDecl'   : 'idNumArraySortExp',
  'StrListDecl'   : 'idStrArraySortExp'
};

let listDeclToReverseExp = {
  'NumListDecl'   : 'idNumArrayReverseExp',
  'StrListDecl'   : 'idStrArrayReverseExp',
  'BoolListDecl'  : 'idBoolArrayReverseExp',
  'ColorListDecl' : 'idColorArrayReverseExp',
  'SoundListDecl' : 'idSoundArrayReverseExp'
};

let listDeclToPlaySoundListStat = {
  'SoundListDecl' : 'idPlaySoundListStat'
}

function genAnyArrayControlNode(arrayCtrlNodeId, listCnctrName, argNameToSymbolMap) {
  if (!arrayCtrlNodeId) {
    return null;
  }
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
  let templates = [
    bhUtil.buildDerivative(bhThis, dervIdIdVar, bhUserOpe),
    genAnyArrayControlNode(listDeclToPlaySoundListStat[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArrayToStrExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayLengthExp', 'Arg0'),
    genAnyArrayControlNode('idAnyArrayPushStat', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode(listDeclToGetExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArraySetStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayInsertStat', 'Arg0', {'Arg2' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayAppendStat', 'Arg0', {'Arg2' : listDeclToExpVoid}),
    genAnyArrayControlNode('idAnyArraySpliceStat', 'Arg0'),
    genAnyArrayControlNode(listDeclToMinMaxExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArrayIndexOfExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode('idAnyArrayIncludesExp', 'Arg0', {'Arg1' : listDeclToLiteral}),
    genAnyArrayControlNode(listDeclToSortExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode(listDeclToReverseExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode(listDeclToSliceExp[listDeclName], 'Arg0'),
    genAnyArrayControlNode('idAnyArrayAssignStat', 'LeftVar', {'RightExp' : listDeclToExpVoid})
  ];
  return templates.filter(node => node !== null);
})();
