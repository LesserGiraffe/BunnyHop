(function() {

  let nodeToSuffixPos = {
    'BinaryBoolExp':            ['*', '*', 'BinaryBoolOpe'],
    'BinaryColorExp':           ['*', '*', 'BinaryColorOpe'],
    'FourArithExp':             ['*', '*', 'FourArithOpe'],
    'MaxMinExp':                ['*', '*', 'MaxMinOpe'],
    'NumRoundExp':              ['*', '*', 'NumRoundOpe'],
    'CheckNumTypeExp':          ['*', '*', 'Function'],
    "NumArrayMaxMinExp":        ['*', '*', 'MaxMinOpe'],
    "StrArrayMaxMinExp":        ['*', '*', 'MaxMinOpe'],
    "MeasureSoundPressureExp":  ['*', '*', 'MeasureOpe'],
  };

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

  let nodeToOptionAndTextId = {
    'MoveStat':           [ ['*', '*', 'MoveOpe'],  ['node', 'move-ops'] ],
    'AnyArraySpliceStat': [ ['*', '*', 'Function'], ['node', 'list-splice-ops'] ],
    'AnyCompExp':         [ ['*', '*', 'CompOpe'],  ['node', 'math-comp-ops'] ],
    'NumCompExp':         [ ['*', '*', 'CompOpe'],  ['node', 'math-comp-ops'] ],
    'StrCompExp':         [ ['*', '*', 'CompOpe'],  ['node', 'math-comp-ops'] ]
  };

  function getAliasId() {
    let suffixPos = nodeToSuffixPos[bhThis.getSymbolName()];
    let part = bhThis.getSymbolName();
    if (suffixPos) {
      part += '${' + bhThis.findDescendantOf(suffixPos).getText() + '}';
    }
    return ['node', 'alias', String(part)]; // String() 必要
  }

  let alias = String(bhTextDb.get(getAliasId()));

  /**
   * エイリアスの ${name} を文字列 (s) で置き換える.
   * s: bhThis から namePos で指定される子孫ノードが持つテキスト.
   */
  function replaceName(namePos) {
    let name = String(bhThis.findDescendantOf(namePos).getText());
    return alias.replace('${name}', name);
  }

  /**
   * エイリアスの ${option} を文字列 (s) で置き換える.
   * s: ID (x) で参照される Text Database の文字列.
   * x: 文字列 (t) を textId に追加した ID.
   * t: bhThis から optionPos で指定される子孫ノードが持つテキスト.
   */
  function replaceOption(optionPos, textId) {
    let opt = bhThis.findDescendantOf(optionPos).getText();
    textId.push(opt);
    return alias.replace('${option}', bhTextDb.get(textId));
  }

  let namePos = nodeToNamePos[bhThis.getSymbolName()];
  if (namePos) {
    return replaceName(namePos);
  }

  let [option, textId] = nodeToOptionAndTextId[bhThis.getSymbolName()] || [null, null];
  if (option) {
    return replaceOption(option, textId)
  }

  return alias;
})();
