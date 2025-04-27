(function() {
  let parent = bhThis.findParentNode();
  if (parent === null) {
    return;
  }
  let symbolIdToDefaultName = {
    'idNumVarDecl': bhTextDb.get('node', 'default-identifier-name', 'num-var'),
    'idStrVarDecl': bhTextDb.get('node', 'default-identifier-name', 'str-var'),
    'idBoolVarDecl': bhTextDb.get('node', 'default-identifier-name', 'bool-var'),
    'idColorVarDecl': bhTextDb.get('node', 'default-identifier-name', 'color-var'),
    'idSoundVarDecl': bhTextDb.get('node', 'default-identifier-name', 'sound-var'),
    'idNumListDecl': bhTextDb.get('node', 'default-identifier-name', 'num-list'),
    'idStrListDecl': bhTextDb.get('node', 'default-identifier-name', 'str-list'),
    'idBoolListDecl': bhTextDb.get('node', 'default-identifier-name', 'bool-list'),
    'idColorListDecl': bhTextDb.get('node', 'default-identifier-name', 'color-list'),
    'idSoundListDecl': bhTextDb.get('node', 'default-identifier-name', 'sound-list'),
    'idMutexBlockDecl': bhTextDb.get('node', 'default-identifier-name', 'mutex-block'),
    'idSyncTimerDecl': bhTextDb.get('node', 'default-identifier-name', 'sync-timer'),
    'idVoidFuncDef': bhTextDb.get('node', 'default-identifier-name', 'void-func-def'),
  };
  let parentSymbolId = String(parent.getId());
  let name = symbolIdToDefaultName[parentSymbolId];
  if (name) {
    bhThis.setText(name);
  }
})();
