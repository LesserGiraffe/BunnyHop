(function() {
  let nodeToNamePos = {
    'VoidFuncDef':     ['*', '*', 'FuncName', '*'],
    'VoidFuncCall':    ['*', 'FuncName', '*'],
    'MutexBlockDecl':  ['*', 'MutexBlockName', '*'],
    'MutexBlockStat':  ['*', 'MutexBlockName', '*'],
    'SyncTimerDecl':   ['*', 'SyncTimerName', '*'],
    'SyncTimerVar':    ['*', 'SyncTimerName', '*'],
    'SemaphoreDecl':   ['*', 'SemaphoreName', '*'],
    'SemaphoreVar':    ['*', 'SemaphoreName', '*'],

    'BoolListDecl':    ['*', 'ListName', '*'],
    'BoolList':        ['*', 'ListName', '*'],
    'BoolListArg':     ['*', 'ListName', '*'],
    'BoolListOutArg':  ['*', 'ListName', '*'],
    'BoolVarDecl':     ['*', 'VarName', '*'],
    'BoolVar':         ['*', 'VarName', '*'],
    'BoolArg':         ['*', 'VarName', '*'],
    'BoolOutArg':      ['*', 'VarName', '*'],

    'ColorListDecl':   ['*', 'ListName', '*'],
    'ColorList':       ['*', 'ListName', '*'],
    'ColorListArg':    ['*', 'ListName', '*'],
    'ColorListOutArg': ['*', 'ListName', '*'],
    'ColorVarDecl':    ['*', 'VarName', '*'],
    'ColorVar':        ['*', 'VarName', '*'],
    'ColorArg':        ['*', 'VarName', '*'],
    'ColorOutArg':     ['*', 'VarName', '*'],

    'NumListDecl':     ['*', 'ListName', '*'],
    'NumList':         ['*', 'ListName', '*'],
    'NumListArg':      ['*', 'ListName', '*'],
    'NumListOutArg':   ['*', 'ListName', '*'],
    'NumVarDecl':      ['*', 'VarName', '*'],
    'NumVar':          ['*', 'VarName', '*'],
    'NumArg':          ['*', 'VarName', '*'],
    'NumOutArg':       ['*', 'VarName', '*'],

    'SoundListDecl':   ['*', 'ListName', '*'],
    'SoundList':       ['*', 'ListName', '*'],
    'SoundListArg':    ['*', 'ListName', '*'],
    'SoundListOutArg': ['*', 'ListName', '*'],
    'SoundVarDecl':    ['*', 'VarName', '*'],
    'SoundVar':        ['*', 'VarName', '*'],
    'SoundArg':        ['*', 'VarName', '*'],
    'SoundOutArg':     ['*', 'VarName', '*'],

    'StrListDecl':     ['*', 'ListName', '*'],
    'StrList':         ['*', 'ListName', '*'],
    'StrListArg':      ['*', 'ListName', '*'],
    'StrListOutArg':   ['*', 'ListName', '*'],
    'StrVarDecl':      ['*', 'VarName', '*'],
    'StrVar':          ['*', 'VarName', '*'],
    'StrArg':          ['*', 'VarName', '*'],
    'StrOutArg':       ['*', 'VarName', '*']
  };

  let namePos = nodeToNamePos[bhThis.getSymbolName()];
  if (namePos) {
    return String(bhThis.findDescendantOf(namePos).getText());
  }
  return null;
})();
