let _textDb = {
  errMsg: {
    invalidSyncTimerInitVal:
      (min, max, val) => `不正な初期値 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerResetVal:
      (min, max, val) => `不正なリセット値 (= ${_str(val)}) が指定されました. (有効な値: ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerWaitTime:
      (min, max, val) => `不正な待ち時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidSemaphorePermits:
      (min, max, val) => `不正なチケットの枚数 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    tryToReleaseTooManySemaphorePermits:
      (cur, add, max) => `チケットボックスに入れようとしたチケットが多すぎます.  (現在の枚数 : ${_str(cur)},  追加しようとした枚数 : ${_str(add)},  保持できる枚数 : ${_str(max)})`,
    invalidSemaphoreWaitTime:
        (min, max, val) => `不正な待ち時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
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
    invalidSoundLength:
      (val, min, max) => `不正な音の長さ (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidSoundPitch:
      (val, min, max) => `不正な音の高さ (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    invalidSoundVolume:
      (val, min, max) => `不正な音の大きさ (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    failedToCtrlHw:
      (cmd) => `ハードウェアの制御に失敗しました.  (command = ${cmd})`,
    invalidClampRange:
      (min, max) => `不正な範囲 (= ${_str(min)} ~ ${_str(max)}) が指定されました.\n下限 ≦ 上限 でなければなりません.`,
    failedToGetBaselineColor:
      () => 'カラーセンサーの基準値の取得に失敗しました.',
    filePathIncludesIllegalChars:
      (path) => `ファイル名には英数字, 「_」, 「/」 以外の半角文字は使えません.  (${_str(path)})`,
    filePathStartsWithSlash:
      (path) => `「/」から始まるファイル名は使えません.  (${_str(path)})`,
    invalidFilePathSize:
      (path, val, max) => `ファイル名は UTF-8 で 1 ~ ${_str(max)} バイトでなければなりません.  (${_str(path)},  ${_str(val)} バイト)`,
    fileNotFound:
      (path) => `ファイルが見つかりませんでした.  (${_str(path)})`,
    invalidRecordTime:
      (val, min, max) => `不正な録音時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
    audioInputDeviceNotFound:
      () => '音声入力デバイスが見つかりませんでした',
    audioOutputDeviceNotFound:
      () => '音声出力デバイスが見つかりませんでした',
    invalidMeasurementTime:
      (val, min, max) => `不正な測定時間 (= ${_str(val)}) が指定されました.  (有効な値 : ${_str(min)} ~ ${_str(max)})`,
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
      (volume, hz, duration) => `音 (大きさ: ${volume} %,  高さ: ${hz} ヘルツ,  長さ: ${duration} 秒)`
  },
  list: {
    namelessList: '無名リスト',
    empty: '[ 空リスト ]'
  }
};
