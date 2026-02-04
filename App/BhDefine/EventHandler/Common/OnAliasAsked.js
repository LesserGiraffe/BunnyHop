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

let nodeToOptionAndTextId = {
  'MoveStat':           [ ['*', '*', 'MoveOpe'],     ['node', 'move-opts'] ],
  'AnyArraySpliceStat': [ ['*', '*', 'Function'],    ['node', 'list-splice-opts'] ],
  'AnyCompExp':         [ ['*', '*', 'CompOpe'],     ['node', 'math-comp-opts'] ],
  'NumCompExp':         [ ['*', '*', 'CompOpe'],     ['node', 'math-comp-opts'] ],
  'StrCompExp':         [ ['*', '*', 'CompOpe'],     ['node', 'math-comp-opts'] ]
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

(function() {
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
