(function() {
  let parent = bhThis.findParentNode();
  if (parent === null) {
    return [];
  }
  let symbolIdToDefaultName = {
    'idNumVarDecl':          '数値変数',
    'idStrVarDecl':          '文字列変数',
    'idBoolVarDecl':         '論理変数',
    'idColorVarDecl':        '色変数',
    'idSoundVarDecl':        '音変数',
    'idNumListDecl':         '数値リスト',
    'idStrListDecl':         '文字列リスト',
    'idBoolListDecl':        '論理リスト',
    'idColorListDecl':       '色リスト',
    'idSoundListDecl':       '音リスト',
    'idCriticalSectionDecl': '区間名',
    'idSyncTimerDecl':       'タイマー名',
    'idCommentPart':         '説明'
  };
  let parentSymbolId = String(parent.getId());
  let name = symbolIdToDefaultName[parentSymbolId];
  if (name) {
    return [
      [name, name]
    ];
  }
  return [];
})();
