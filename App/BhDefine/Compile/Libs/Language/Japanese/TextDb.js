let _textDb = {
  errMsg: {
    invalidSyncTimerInitVal:
      (min, max, val) => `不正な初期値 (= ${_str(val)}) が設定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerResetVal:
      (min, max, val) => `不正なリセット値 (= ${_str(val)}) が指定されました. (有効な値: ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerWaitTime:
      (min, max, val) => `不正な待ち時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    cannotConvertStrToNum:
      (str) => `数値に変換できない文字列です.  (${_str(str)})`,
    invalidRandRange:
      (min, max, max_width) => `不正な乱数の範囲 (= ${_str(min)} ~ ${_str(max)}) が指定されました.\n上限 - 下限 ≦ ${_str(max_width)} を満たさなければなりません.`,
    invalidSleepTime:
      (min, max, val) => `不正な待ち時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidMoveSpeed:
      (val) => `不正な速さ (= ${_str(val)}) が指定されました.`,
    invalidMoveTime:
      (val, min, max) => `不正な時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidNumElems:
      (val, min, max) => `不正な個数 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidArraySize:
      (len, num, max) => `リストの要素数が正常な範囲 (0 ~ ${_str(max)} 個) を超えます.  (${_str(len)} + ${_str(num)} = ${_str(len + num)} 個)`,
    invalidListIdx:
      (val, min, max) => `不正なインデックス (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    listIsEmpty:
      () => `リストが空です.`,
    invalidDurationOfSound:
      (val, min, max) => `不正な音の長さ (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidPitchOfSound:
      (val, min, max) => `不正な音の高さ (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    failedToCtrlHw:
      (cmd) => `ハードウェアの制御に失敗しました.  (command = ${cmd})`
  },
  literal: {
    bool: {
      true: '真',
      false: '偽'
    },
    color: {
      red: '赤',
      green: '緑',
      blue: '青',
      cyan: '水色',
      purple: 'むらさき',
      yellow: 'きいろ',
      white: '白',
      black: '黒'
    },
    sound:
      (hz, duration) => `高さ: ${hz} [ヘルツ],  長さ: ${duration} [秒]`
  },
  list: {
    empty: '空リスト'
  }
};
