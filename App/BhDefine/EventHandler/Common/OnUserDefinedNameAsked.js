(function() {
  let nodeToNamePos = {
    'VoidFuncDef':    ['*', '*', 'FuncName', '*'],
    'VoidFuncCall':   ['*', 'FuncName', '*'],
    'MutexBlockDecl': ['*', 'MutexBlockName', '*'],
    'MutexBlockStat': ['*', 'MutexBlockName', '*'],
    'SyncTimerDecl':  ['*', 'SyncTimerName', '*'],
    'SyncTimerVar':   ['*', 'SyncTimerName', '*'],
    'SemaphoreDecl':  ['*', 'SemaphoreName', '*'],
    'SemaphoreVar':   ['*', 'SemaphoreName', '*'],

    'BoolList':       ['*', 'ListName', '*'],
    'BoolListArg':    ['*', 'ListName', '*'],
    'BoolListDecl':   ['*', 'ListName', '*'],
    'BoolOutArg':     ['*', 'VarName', '*'],
    'BoolVar':        ['*', 'VarName', '*'],
    'BoolArg':        ['*', 'VarName', '*'],
    'BoolVarDecl':    ['*', 'VarName', '*'],

    'ColorList':      ['*', 'ListName', '*'],
    'ColorListArg':   ['*', 'ListName', '*'],
    'ColorListDecl':  ['*', 'ListName', '*'],
    'ColorOutArg':    ['*', 'VarName', '*'],
    'ColorVar':       ['*', 'VarName', '*'],
    'ColorArg':       ['*', 'VarName', '*'],
    'ColorVarDecl':   ['*', 'VarName', '*'],

    'NumList':        ['*', 'ListName', '*'],
    'NumListArg':     ['*', 'ListName', '*'],
    'NumListDecl':    ['*', 'ListName', '*'],
    'NumOutArg':      ['*', 'VarName', '*'],
    'NumVar':         ['*', 'VarName', '*'],
    'NumArg':         ['*', 'VarName', '*'],
    'NumVarDecl':     ['*', 'VarName', '*'],

    'SoundList':      ['*', 'ListName', '*'],
    'SoundListArg':   ['*', 'ListName', '*'],
    'SoundListDecl':  ['*', 'ListName', '*'],
    'SoundOutArg':    ['*', 'VarName', '*'],
    'SoundVar':       ['*', 'VarName', '*'],
    'SoundArg':       ['*', 'VarName', '*'],
    'SoundVarDecl':   ['*', 'VarName', '*'],

    'StrList':        ['*', 'ListName', '*'],
    'StrListArg':     ['*', 'ListName', '*'],
    'StrListDecl':    ['*', 'ListName', '*'],
    'StrOutArg':      ['*', 'VarName', '*'],
    'StrVar':         ['*', 'VarName', '*'],
    'StrArg':         ['*', 'VarName', '*'],
    'StrVarDecl':     ['*', 'VarName', '*']
  };

  let namePos = nodeToNamePos[bhThis.getSymbolName()];
  if (namePos) {
    return String(bhThis.findDescendantOf(namePos).getText());
  }
  return null;
})();
