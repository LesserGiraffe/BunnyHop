let _textDb = {
  errMsg: {
    invalidSyncTimerInitVal:
      (min, max, val) => `ふせいな しょきち (= ${_str(val)}) が してされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerResetVal:
      (min, max, val) => `ふせいなリセットち (= ${_str(val)}) が していされました. (ゆうこうな あたい: ${_str(min)} ~ ${_str(max)})`,
    invalidSyncTimerWaitTime:
      (min, max, val) => `ふせいな待ち時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidSemaphorePermits:
      (min, max, val) => `ふせいなチケットの数 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    tryToReleaseTooManySemaphorePermits:
      (cur, add, max) => `チケットボックスに入れようとしたチケットが多すぎます.  (げんざいの数 : ${_str(cur)},  入れようとした数 : ${_str(add)},  入る数 : ${_str(max)})`,
    invalidSemaphoreWaitTime:
      (min, max, val) => `ふせいな待ち時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidRandRange:
      (min, max, max_width) => `ふせいな らんすうの はんい (= ${_str(min)} ~ ${_str(max)}) が していされました.\n上げん - 下げん ≦ ${_str(max_width)} を みたさなければなりません.`,
    invalidSleepTime:
      (min, max, val) => `ふせいな待ち時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidMoveSpeed:
      (val) => `ふせいな速さ (= ${_str(val)}) が していされました.`,
    invalidMoveTime:
      (val, min, max) => `ふせいな時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidNumElems:
      (val, min, max) => `ふせいな こすう (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidListSize:
      (len, num, max) => `リストの ようその数が せいじょうな はんい (0 ~ ${_str(max)} こ) を こえます.  (${_str(len)} + ${_str(num)} = ${_str(len + num)} こ)`,
    invalidListIdx:
      (val, min, max) => `ふせいなインデックス (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    listIsEmpty:
      () => `リストが空です.`,
    invalidSoundLength:
      (val, min, max) => `ふせいな音の長さ (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidSoundPitch:
      (val, min, max) => `ふせいな音の高さ (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    invalidSoundVolume:
      (val, min, max) => `ふせいな音の大きさ (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    failedToCtrlHw:
      (cmd) => `ハードウェアのコントロールに しっぱいしました.  (command = ${cmd})`,
    invalidClampRange:
      (min, max) => `ふせいな はんい (= ${_str(min)} ~ ${_str(max)}) が していされました.\n下げん ≦ 上げん でなければなりません.`,
    failedToGetBaselineColor:
      () => 'カラーセンサーの きじゅんちの しゅとくに しっぱいしました.',
    filePathIncludesIllegalChars:
      (path) => `ファイル名には えい数字, 「_」, 「/」 いがいの半角文字は使えません.  (${_str(path)})`,
    filePathStartsWithSlash:
      (path) => `「/」から始まるファイル名は使えません.  (${_str(path)})`,
    invalidFilePathSize:
      (path, val, max) => `ファイル名は UTF-8 で 1 ~ ${_str(max)} バイトでなければなりません.  (${_str(path)},  ${_str(val)} バイト)`,
    fileNotFound:
      (path) => `ファイルが見つかりませんでした.  (${_str(path)})`,
    invalidRecordTime:
      (val, min, max) => `ふせいな ろくおん時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
    audioInputDeviceNotFound:
      () => '音声入力デバイスが見つかりませんでした',
    audioOutputDeviceNotFound:
      () => '音声出力デバイスが見つかりませんでした',
    invalidMeasurementTime:
      (val, min, max) => `ふせいな そくてい時間 (= ${_str(val)}) が していされました.  (ゆうこうな あたい : ${_str(min)} ~ ${_str(max)})`,
  },
  literal: {
    bool: {
      true: '真',
      false: '偽 (ぎ)'
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
    namelessList: 'むめいリスト',
    empty: '[ 空リスト ]'
  }
};
