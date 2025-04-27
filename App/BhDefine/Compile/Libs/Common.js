let _jString = java.lang.String;
let _jExecutors = java.util.concurrent.Executors;
let _jThread = java.lang.Thread;
let _jLock = java.util.concurrent.locks.ReentrantLock;
let _jByteOrder = java.nio.ByteOrder;
let _jByteType = java.lang.Byte.TYPE;
let _jFloatType = java.lang.Float.TYPE;
let _jFile = java.io.File;
let _jFiles = java.nio.file.Files;
let _jPaths = java.nio.file.Paths;
let _jAtomicLong = java.util.concurrent.atomic.AtomicLong;
let _jProcBuilder = java.lang.ProcessBuilder;
let _jBufferedInputStream = java.io.BufferedInputStream;
let _jStandardOpenOption = java.nio.file.StandardOpenOption;
let _jReflectArray = java.lang.reflect.Array;
let _jClass = java.lang.Class;
let _jCyclicBarrier = java.util.concurrent.CyclicBarrier;
let _jStringBuilder = java.lang.StringBuilder;
let _jTimeUnit = java.util.concurrent.TimeUnit;
let _jTimeoutException = java.util.concurrent.TimeoutException;
let _eventHandlers = {};
let _executor = _jExecutors.newFixedThreadPool(16);
let _nil = new _Nil();
let _idxCallStack = 0;
let _idxCurrentNodeInstId = 1;
let _idxErrorMsgs = 2;
let _threadContext = _createThreadContext();
let _syncTimer = (function() {
  let nilTimer = bhScriptHelper.factory.newSyncTimer(0, true);
  return {
    nil: nilTimer,
    minCount: nilTimer.MIN_COUNT,
    maxCount: nilTimer.MAX_COUNT
  };
})();
// 倍精度浮動小数点数で表現可能な値の中で long 型の最大値を超えない最大の値.
let _nearestLongMax = Number(0x7FFFFFFFFFFFFC00n);
let _nearestLongMin = -_nearestLongMax;
let _maxArraySize = 0xFFFF_FFFF;
let _programStartingTime = 0;


function _Nil() {}

/** スレッド固有のデータを作成する. */
function _createThreadContext() {
  // アクセス高速のため配列に格納する.
  let threadContext = [
    [],   // call stack
    null, // current node instance id,
    []    // error messages
  ];
  // Java の long 型は JavaScript で使うと double 型に変換される. 
  // Java のスレッド ID は long 型 だが, Number.MAX_SAFE_INTEGER の範囲に入る保証がないので,
  // スレッド ID は Java のメソッド内部で取り扱う.
  bhScriptHelper.thread.setThreadData(threadContext);
  return threadContext;
}

function _removeThreadContext(dest) {
  let threadContext = _getThreadContext();
  if (dest && threadContext) {
    dest['_callStack'] = threadContext[_idxCallStack];
    dest['_currentNodeInstId'] = threadContext[_idxCurrentNodeInstId];
    dest['_errorMsgs'] = threadContext[_idxErrorMsgs];
  }
  bhScriptHelper.thread.removeThreadData();
}

function _getThreadContext() {
  return bhScriptHelper.thread.getThreadData();
}

function _genLockObj(fair) {
  return new _jLock(fair);
}

function _tryLock(lockObj) {
  return lockObj.tryLock();
}

function _lock(lockObj) {
  lockObj.lock();
}

function _unlock(lockObj) {
  lockObj.unlock();
}

function _fullWidthToHalf(str) {
  return str
    .replace(/０/g, '0').replace(/１/g, '1')
    .replace(/２/g, '2').replace(/３/g, '3')
    .replace(/４/g, '4').replace(/５/g, '5')
    .replace(/６/g, '6').replace(/７/g, '7')
    .replace(/８/g, '8').replace(/９/g, '9')
    .replace(/Ａ/g, 'A').replace(/ａ/g, 'a')
    .replace(/Ｂ/g, 'B').replace(/ｂ/g, 'b')
    .replace(/Ｃ/g, 'C').replace(/ｃ/g, 'c')
    .replace(/Ｄ/g, 'D').replace(/ｄ/g, 'd')
    .replace(/Ｅ/g, 'E').replace(/ｅ/g, 'e')
    .replace(/Ｆ/g, 'F').replace(/ｆ/g, 'f')
    .replace(/Ｇ/g, 'G').replace(/ｇ/g, 'g')
    .replace(/Ｈ/g, 'H').replace(/ｈ/g, 'h')
    .replace(/Ｉ/g, 'I').replace(/ｉ/g, 'i')
    .replace(/Ｊ/g, 'J').replace(/ｊ/g, 'j')
    .replace(/Ｋ/g, 'K').replace(/ｋ/g, 'k')
    .replace(/Ｌ/g, 'L').replace(/ｌ/g, 'l')
    .replace(/Ｍ/g, 'M').replace(/ｍ/g, 'm')
    .replace(/Ｎ/g, 'N').replace(/ｎ/g, 'n')
    .replace(/Ｏ/g, 'O').replace(/ｏ/g, 'o')
    .replace(/Ｐ/g, 'P').replace(/ｐ/g, 'p')
    .replace(/Ｑ/g, 'Q').replace(/ｑ/g, 'q')
    .replace(/Ｒ/g, 'R').replace(/ｒ/g, 'r')
    .replace(/Ｓ/g, 'S').replace(/ｓ/g, 's')
    .replace(/Ｔ/g, 'T').replace(/ｔ/g, 't')
    .replace(/Ｕ/g, 'U').replace(/ｕ/g, 'u')
    .replace(/Ｖ/g, 'V').replace(/ｖ/g, 'v')
    .replace(/Ｗ/g, 'W').replace(/ｗ/g, 'w')
    .replace(/Ｘ/g, 'X').replace(/ｘ/g, 'x')
    .replace(/Ｙ/g, 'Y').replace(/ｙ/g, 'y')
    .replace(/Ｚ/g, 'Z').replace(/ｚ/g, 'z')
    .replace(/．/g, '.');
}

//イベント名とイベントハンドラを登録
function _addEvent(eventHandler, event) {
  if (_eventHandlers[event] === void 0)
    _eventHandlers[event] = [];
  _eventHandlers[event].push(eventHandler);
}

//イベントに関連付けられたベントハンドラの名前を返す
function _getEventHandlerNames(event) {
  let nameList = [];
  let handlers = _eventHandlers[event];
  if (handlers !== void 0) {
    for (let handler of handlers) {
      nameList.push(handler.name);
    }
  }
  return nameList;
}

function _truncateStr(str, len, suffix) {
  suffix = suffix || '';
  return (str.length > len) ? (str.substring(0, len) + suffix) : str;
}

function _isIntInRange(num, min, max) {
  return Number.isInteger(num) && (min <= num) && (num <= max);
}

function _isInRange(num, min, max) {
  return (min <= num) && (num <= max);
}

function _strToNum(strVal) {
  if (strVal === 'Infinity')
    return Number.POSITIVE_INFINITY;
  else if (strVal === '-Infinity')
    return Number.NEGATIVE_INFINITY;
  else if (strVal === 'NaN')
    return Number.NaN;
  
  if (strVal !== '') {
    let num = Number(strVal);
    if (Number.isFinite(num))
      return num;
  }
  strVal = _truncateStr(strVal, 256, '...');
  throw _newBhProgramException(_textDb.errMsg.cannotConvertStrToNum(strVal));
}

function _randomInt(min, max) {
  if (min > max) {
    [min, max] = [max, min];
  }
  let diff = Math.floor(max) - Math.ceil(min);
  // diff == NaN の場合を考慮する
  if (diff <= Number.MAX_SAFE_INTEGER) {
    return Math.floor(Math.random() * (diff + 1)) + min;
  }
  throw _newBhProgramException(_textDb.errMsg.invalidRandRange(min, max, Number.MAX_SAFE_INTEGER));
}

function _clamp(v, min, max) {
  if (v < min)
    return min;
  else if (v > max)
    return max;

  return v;
}

function _println(arg) {
  bhScriptHelper.io.println(_str(arg));
}

function _sleep(sec) {
  let max = Math.floor(_nearestLongMax / 1024);
  if (!_isInRange(sec, 0, max)) {
    throw _newBhProgramException(_textDb.errMsg.invalidSleepTime(0, max, sec));
  }
  
  let millis = Math.round(sec * 1000);
  try {
    _jThread.sleep(millis);
  } catch (e) {
    _addExceptionMsg('_sleep()');
    throw e;
  }
}

function _scan(str) {
  bhScriptHelper.io.println(str);
  let input = bhScriptHelper.io.scanln();
  return (input === null) ? "" : String(input);
}

function _startTimer() {
  return bhScriptHelper.util.timer.start();
}

function _getTimerVal() {
  return bhScriptHelper.util.timer.getMillis() / 1000; // sec
}

let _getSerialNo = (function () {
  let counter = new _jAtomicLong();
  return function() {
    return counter.getAndIncrement();
  }
})();

//==================================================================
//              比較
//==================================================================
_Nil.prototype._eq = function(val) {
  return val instanceof _Nil;
}

Number.prototype._eq = function(val) {
  if (typeof(val) === 'number')
    return this.valueOf() === val;
  if (val instanceof Number)
    return this.valueOf() === val.valueOf();

  return false;
}

Boolean.prototype._eq = function(val) {
  if (typeof(val) === 'boolean')
    return this.valueOf() === val;
  if (val instanceof Boolean)
    return this.valueOf() === val.valueOf();

  return false;
}

String.prototype._eq = function(val) {
  if (typeof(val) === 'string')
    return this.valueOf() === val;
  if (val instanceof String)
    return this.valueOf() === val.valueOf();

  return false;
}

_Sound.prototype._eq = function(val) {
  if (val instanceof _Sound)
    return (this.hz === val.hz) && (this.duration === val.duration) && (this.amp === val.amp);
  
  return false;
}

_Color.prototype._eq = function(val) {
  if (val instanceof _Color)
    return (this.red === val.red) && (this.green === val.green) && (this.blue === val.blue);
  
  return false;
}

function _anyEq(a, b) {
  return a._eq(b);
}

function _anyNeq(a, b) {
  return !_anyEq(a, b);
}

//==================================================================
//              プロセス待ち
//==================================================================
function readStream(stream) {
  let readByte = [];
  while(true) {
    let rb = stream.read();
    if (rb === -1)
      break;

    readByte.push(rb);
  }
  let byteArray = _jReflectArray.newInstance(_jByteType, readByte.length);
  for (let i = 0; i < readByte.length; ++i)
    byteArray[i] = bhScriptHelper.util.toByte(readByte[i]);

  return new _jString(byteArray);
}

function _waitProcEnd(process, getStdinStr, checkExitVal) {
  let retStr = null;
  try {
    let is = process.getInputStream();
    if (getStdinStr) {
      retStr = readStream(is);
    } else {
      while (is.read() !== -1);
        process.waitFor();
    }
    if (checkExitVal && (process.exitValue() !== 0))
      throw _newBhProgramException('abnormal process end');
  } finally {
    process.getErrorStream().close();
    process.getInputStream().close();
    process.getOutputStream().close();
  }
  return retStr;
}

//==================================================================
//              例外処理
//==================================================================
function _addExceptionMsg(msg) {
  _getThreadContext()[_idxErrorMsgs].push(String(msg));
}

function _newBhProgramException(msg) {
  let context = _getThreadContext();
  let currentNodeInstId = context[_idxCurrentNodeInstId];
  let callStack = (currentNodeInstId !== null)
      ? context[_idxCallStack].concat(currentNodeInstId) // 'concat' returns a new array.
      : context[_idxCallStack];
  return bhScriptHelper.factory.newBhProgramException(callStack, msg);
}

//==================================================================
//              配列操作
//==================================================================

function _checkAryIdx(index, len, margin) {
  let idx = Math.trunc(index);
  let max = len - 1 + margin;
  let min = -(len + margin);
  if (!_isInRange(idx, min, max)) {
    if (len === 0) {
      throw _newBhProgramException(_textDb.errMsg.listIsEmpty());
    } else {
      throw _newBhProgramException(_textDb.errMsg.invalidListIdx(index, min, max));
    }
  }
  return (idx < 0) ? (idx - min) : idx;
}

function _checkArySize(aryLen, increment) {
  if (!_isInRange(aryLen + increment, 0, _maxArraySize)) {
    throw _newBhProgramException(
      _textDb.errMsg.invalidArraySize(aryLen, increment, _maxArraySize));
  }
}

function _checkNumAryElems(num) {
  let count = Math.trunc(num);
  if (!_isInRange(count, 0, _maxArraySize)) {
    throw _newBhProgramException(_textDb.errMsg.invalidNumElems(num, 0, _maxArraySize));
  }
  return count;
}

function _aryToStr(ary, listName) {
  if (ary.length === 0)
    return listName + ' = 空リスト';

  let halfOfMaxElems = 256;
  let strBuilder = new _jStringBuilder();
  if (ary.length > halfOfMaxElems * 2) {
    for (let i = 0; i < halfOfMaxElems; ++i) {
      strBuilder
      .append(listName)
      .append(_jString.format('[%.0f] = %s\n', i, ary[i]._str()));
    }
    strBuilder.append('  ...  \n');
    for (let i = ary.length - halfOfMaxElems; i < ary.length; ++i) {
      strBuilder
      .append(listName)
      .append(_jString.format('[%.0f] = %s\n', i, ary[i]._str()));
    }
  } else {
    for (let i = 0; i < ary.length; ++i) {
      strBuilder
      .append(listName)
      .append(_jString.format('[%.0f] = %s\n', i, ary[i]._str()));
    }
  }
  return String(strBuilder);
}

function _aryPush(ary, val, num) {
  num = _checkNumAryElems(num);
  _checkArySize(ary.length, num);
  for (let i = 0; i < num; ++i) {
    ary[ary.length] = val;
  }
}

function _aryPop(ary) {
  ary.pop();
}

function _aryInsert(ary, idx, val, num) {
  idx = _checkAryIdx(idx, ary.length, 1);
  num = _checkNumAryElems(num);
  _checkArySize(ary.length, num);

  // 疎配列にアクセスするとメモリリークを起こすので, 疎配列にせずに高速に挿入する.
  let len = ary.length;
  let numToShift = len - idx;
  let numToPush = Math.max(num - numToShift, 0);
  for (let i = 0; i < numToPush; ++i) {
    ary[ary.length] = val;
  }
  let numToSpillOut = Math.min(numToShift, num);
  for (let i = len - numToSpillOut; i < len; ++i) {
    ary[ary.length] = ary[i];
  }
  let numToCopy = numToShift - numToSpillOut;
  for (let i = idx + numToCopy - 1; i >= idx; --i) {
    ary[i + num] = ary[i];
  }
  len = idx + num - numToPush;
  for (let i = idx; i < len; ++i) {
    ary[i] = val;
  }
}

function _aryRemove(ary, idx, num) {
  if (ary.length === 0) {
    return;
  }
  idx = _checkAryIdx(idx, ary.length, 0);
  num = _checkNumAryElems(num);
  ary.splice(idx, num);
}

function _aryExtract(ary, idx, num) {
  if (ary.length === 0) {
    return;
  }
  idx = _checkAryIdx(idx, ary.length, 0);
  num = _checkNumAryElems(num);
  let len = Math.min(ary.length - idx, num);
  for (let i = 0; i < len; ++i) {
    ary[i] = ary[i + idx];
  }
  ary.splice(len, ary.length);
}

function _aryClear(ary) {
  ary.splice(0, ary.length);
}

function _aryAddAll(aryA, idx, aryB) {
  idx = _checkAryIdx(idx, aryA.length, 1);
  _checkArySize(aryA.length, aryB.length);
  let tails = aryA.slice(idx, aryA.length);
  aryA.length = idx;
  Array.prototype.push.apply(aryA, aryB);
  Array.prototype.push.apply(aryA, tails);
}

function _aryGet(ary, idx) {
  idx = _checkAryIdx(idx, ary.length, 0);
  return ary[idx];
}

function _aryGetLast(ary) {
  let idx = _checkAryIdx(ary.length - 1, ary.length, 0);
  return ary[idx];
}

function _arySet(ary, idx, val) {
  idx = _checkAryIdx(idx, ary.length, 0);
  ary[idx] = val;
}

function _aryLen(ary) {
  return ary.length;
}

function _aryReverse(ary) {
  ary.reverse();
}

function _aryFirstIndexOf(ary, elem) {
  for (let i = 0; i < ary.length; ++i)
    if (ary[i]._eq(elem))
      return i;

  return Number.NaN;
}

function _aryLastIndexOf(ary, elem) {
  for (let i = ary.length - 1; i >= 0; --i)
    if (ary[i]._eq(elem))
      return i;

  return Number.NaN;
}

function _aryIncludes(ary, elem) {
  return _aryFirstIndexOf(ary, elem) >= 0;
}

function _aryEq(aryA, aryB) {
  if (aryA === aryB)
    return true;

  if (aryA.length !== aryB.length)
    return false;

  for (let i = 0; i < aryA.length; ++i)
    if (!aryA[i]._eq(aryB[i]))
      return false;

  return true;
}

function _aryNeq(aryA, aryB) {
  return !_aryEq(aryA, aryB);
}

function _delDuplicates(ary) {
  let set = [];
  for (let i = 0; i < ary.length; ++i)
    if (!_aryIncludes(set, ary[i]))
      set.push(ary[i]);

  return set;
}

//==================================================================
//              Set 操作
//==================================================================

// aryA is a subset of aryB
function _isSubset(aryA, aryB) {
  if (aryA === aryB)
    return true;

  let setA = _delDuplicates(aryA);
  let setB = _delDuplicates(aryB);
  if (setA.length > setB.length)
    return false;

  for (let i = 0; i < setA.length; ++i)
    if (!_aryIncludes(setB, aryA[i]))
      return false;

  return true;
}

function _isProperSubset(aryA, aryB) {
  if (aryA === aryB)
    return false;

  let setA = _delDuplicates(aryA);
  let setB = _delDuplicates(aryB);
  if (setA.length >= setB.length)
    return false;

  for (let i = 0; i < setA.length; ++i)
    if (!_aryIncludes(setB, aryA[i]))
      return false;

  return true;
}

function _isSuperset(aryA, aryB) {
  return _isSubset(aryB, aryA);
}

function _isProperSuperset(aryA, aryB) {
  return _isProperSubset(aryB, aryA);
}

function _setEq(aryA, aryB) {
  if (aryA === aryB)
    return true;

  let setA = _delDuplicates(aryA);
  let setB = _delDuplicates(aryB);
  if (setA.length !== setB.length)
    return false;

  for (let i = 0; i < setA.length; ++i)
    if (!_aryIncludes(setB, aryA[i]))
      return false;

  return true;
}

function _setNeq(aryA, aryB) {
  return !_setEq(aryA, aryB);
}

//==================================================================
//              音再生
//==================================================================
let _jAudioFormat = javax.sound.sampled.AudioFormat;
let _jDataLine = javax.sound.sampled.DataLine;
let _jAudioSystem = javax.sound.sampled.AudioSystem;
let isBigEndian = _jByteOrder.nativeOrder() === _jByteOrder.BIG_ENDIAN;
let bytePerSample = 2;
let samplingRate = 44100;    //44.1KHz
let amplitude = 16384/2;
let wave1Hz = _jReflectArray.newInstance(_jFloatType, samplingRate);
let _nilSound = _createSound(0, 0);

(function _initMusicPlayer() {
  for (let i = 0; i < wave1Hz.length; ++i)
    wave1Hz[i] = Math.cos(2.0 * Math.PI * i / samplingRate);
})();

/**
 * 単一波長の波形を生成する
 * @param waveBuf 波形データの格納先
 * @param hz 生成する波長
 * @param amp 振幅
 * @param bufBegin 波形を格納する最初のインデックス (inclusive)
 * @param bufEnd bufBeginからbufEndの1つ前まで波形を格納する (exclusive)
 * @param samplePos 基準波形の最初の取得位置
 * @return 基準波形の次の取得位置
 */
function _genMonoFreqWave(waveBuf, bufBegin, bufEnd, hz, amp, samplePos) {
  hz = Math.round(hz);
  if (hz < 0 || hz > (samplingRate / 2))
    hz = 0;
  if (samplePos > (wave1Hz.length - 1))
    samplePos = 0;
  for (let i = bufBegin; i < bufEnd; i += bytePerSample) {
    let sample = Math.floor(amp * wave1Hz[samplePos]);
    if (isBigEndian) {
      waveBuf[i+1] = bhScriptHelper.util.toByte(sample);
      waveBuf[i] = bhScriptHelper.util.toByte((sample >> 8));
    } else {
      waveBuf[i] = bhScriptHelper.util.toByte(sample);
      waveBuf[i+1] = bhScriptHelper.util.toByte((sample >> 8));
    }
    samplePos += hz;
    if (samplePos > (wave1Hz.length - 1))
      samplePos -= wave1Hz.length - 1;
  }
  return samplePos;
}

/**
 * 音リストからバッファに収まる分の波形を生成する. 音リストの末尾から順に波形を生成する.
 * @param waveBuf 波形データの格納先
 * @param soundList 波形を生成する音のリスト
 * @param vol 音量
 * @param samplePos 基準波形の最初の取得位置
 * @return 波形データの長さと基準波形の次の取得位置
 */
function _genWave(waveBuf, soundList, vol, samplePos) {
  let bufBegin = 0;
  let waveBufRemains = waveBuf.length;
  while (soundList.length !== 0 && waveBufRemains !== 0) {
    let sound = soundList[soundList.length - 1];
    let soundLen = Math.floor(sound.duration * samplingRate) * bytePerSample;
    if (soundLen > waveBufRemains) {
      sound.duration -= waveBufRemains / bytePerSample / samplingRate;
      let bufEnd = bufBegin + waveBufRemains;
      samplePos = _genMonoFreqWave(waveBuf, bufBegin, bufEnd, sound.hz, sound.amp * vol, samplePos);
      bufBegin = bufEnd;
      waveBufRemains = 0;
    } else {
      soundList.pop();
      let bufEnd = bufBegin + soundLen;
      samplePos = _genMonoFreqWave(waveBuf, bufBegin, bufEnd, sound.hz, sound.amp * vol, samplePos);
      bufBegin = bufEnd;
      waveBufRemains -= soundLen;
    }
  }
  return {
    waveLen : bufBegin,
    samplePos : samplePos
  };
}

/**
 * メロディ再生
 * @param soundList 再生する音のリスト
 * @param reverse 音リストの末尾から順に再生する場合true
 * */
function _playMelodies(soundList, reverse) {
  let soundListCopy = [];
  if (!reverse) {
    for (let i = soundList.length - 1; i >= 0; --i)
      soundListCopy.push(_createSound(soundList[i].hz, soundList[i].duration));
  } else {
    for (let i = 0; i < soundList.length; ++i)
      soundListCopy.push(_createSound(soundList[i].hz, soundList[i].duration));
  }
  let waveBuf = _jReflectArray.newInstance(_jByteType, samplingRate * bytePerSample);
  let format = new _jAudioFormat(
    _jAudioFormat.Encoding.PCM_SIGNED,
    samplingRate,
    8 * bytePerSample,
    1,
    bytePerSample,
    samplingRate,
    isBigEndian);
  let line = null;
  try {
    let dLineInfo = new _jDataLine.Info(_jClass.forName('javax.sound.sampled.SourceDataLine'), format);
    line = _jAudioSystem.getLine(dLineInfo);
    line.open(format, waveBuf.length);
    line.start();
    let samplePos = 0;
    while (soundListCopy.length !== 0) {
      let ret = _genWave(waveBuf, soundListCopy, 1.0, samplePos);
      samplePos = ret.samplePos;
      line.write(waveBuf, 0, ret.waveLen);
    }
  } catch (e) {
    _addExceptionMsg('_playMelodies()');
    throw e;
  } finally {
    if (line !== null) {
      line.drain();
      line.close();
    }
  }
}

/**
 * wavファイル再生
 * @param path 再生したいファイルのパス (java.nio.file.Path オブジェクト)
 */
function _playWavFile(path) {
  let line = null;
  let bis = null;
  let ais = null;
  try {
    bis = new _jBufferedInputStream(_jFiles.newInputStream(path, _jStandardOpenOption.READ));
    ais = _jAudioSystem.getAudioInputStream(bis);
    let dLineInfo = new _jDataLine.Info(_jClass.forName('javax.sound.sampled.SourceDataLine'), ais.getFormat());
    line = _jAudioSystem.getLine(dLineInfo);
    line.open();
    line.start();
    let waveBuf = _jReflectArray.newInstance(_jByteType, line.getBufferSize());
    let byteRead = -1;
    while((byteRead = ais.read(waveBuf)) !== -1) {
      line.write(waveBuf, 0, byteRead);
    }
  } catch (e) {
    _addExceptionMsg('_playWavFile()');
    throw e;
  } finally {
    if (line !== null) {
      line.drain();
      line.stop();
      line.close();
    }
    if (ais !== null)
      ais.close();
    if (bis !== null)
      bis.close();
  }
}

// 音クラス
function _Sound(hz, duration, amp) {
  this.hz = hz;
  this.duration = duration;
  this.amp = amp;
}

function _createSound(hz, duration) {
  return new _Sound(hz, duration, amplitude);
}

function _pushSound(sound, soundList) {
  soundList.push(sound);
  return soundList;
}

function _sayOnLinux(word) {
  word = word.replace(/"/g, '');
  word = bhScriptHelper.util.substringByBytes(word, 990, "UTF-8");
  let path = _jPaths.get(bhScriptHelper.util.getExecPath(), 'Actions', 'bhSay.sh');
  let talkCmd = String(path.toAbsolutePath().toString());
  let procBuilder = new _jProcBuilder('sh', talkCmd, '"' + word + '"');
  try {
    let process = procBuilder.start();
    _waitProcEnd(process, false, true);
  } catch (e) {
    _addExceptionMsg('_sayOnLinux()');
    throw e;
  }
}

// 色クラス
function _Color(red, green, blue) {
  this.red = red;
  this.green = green;
  this.blue = blue;
}

function _createColorFromName(colorName) {
  colorName = colorName.toLowerCase();
  switch (colorName) {
    case 'red':
      return new _Color(255, 0, 0);
    case 'green':
      return new _Color(0, 255, 0);
    case 'blue':
      return new _Color(0, 0, 255);
    case 'cyan':
      return new _Color(0, 255, 255);
    case 'magenta':
      return new _Color(255, 0, 255);
    case 'yellow':
      return new _Color(255, 255, 0);
    case 'white':
      return new _Color(255, 255, 255);
    case 'black':
      return new _Color(0,0,0);
    default:
      throw _newBhProgramException('_createColorFromName invalid colorName ' + colorName);
  }
}

function _addColor(left, right) {
  return new _Color(
    _clamp(left.red + right.red, 0, 255),
    _clamp(left.green + right.green, 0, 255),
    _clamp(left.blue + right.blue, 0, 255));
}

function _subColor(left, right) {
  return new _Color(
    _clamp(left.red - right.red, 0, 255),
    _clamp(left.green - right.green, 0, 255),
    _clamp(left.blue - right.blue, 0, 255));
}

let _nilColor = new _Color(0, 0, 0);
//==================================================================
//              文字列化
//==================================================================
_Nil.prototype._str = function() {return '';}

Number.prototype._str = function () {
  if (Number.isInteger(Number(this))) {
    return String(BigInt(this));
  }
  return this.toString();
}

BigInt.prototype._str = function () {
  return this.toString();
}

Boolean.prototype._str = function () {
  if (this == true)
    return _textDb.literal.bool.true;

  return _textDb.literal.bool.false;
}

String.prototype._str = function () {
  return this.toString();
}

_Sound.prototype._str = function () {
  return '高さ: ' + this.hz + '[ヘルツ],  長さ: ' + Math.round(this.duration * 1000.0) + '[ミリ秒]';
}

_Color.prototype._str = function () {
  if (this.red === 255 && this.green === 0 && this.blue === 0)
    return '赤';
  else if (this.red === 0 && this.green === 255 && this.blue === 0)
    return '緑';
  else if (this.red === 0 && this.green === 0 && this.blue === 255)
    return '青';
  else if (this.red === 0 && this.green === 255 && this.blue === 255)
    return '水色';
  else if (this.red === 255 && this.green === 0 && this.blue === 255)
    return 'むらさき';
  else if (this.red === 255 && this.green === 255 && this.blue === 0)
    return 'きいろ';
  else if (this.red === 255 && this.green === 255 && this.blue === 255)
    return '白';
  else if (this.red === 0 && this.green === 0 && this.blue === 0)
    return '黒';

  return '(red, green, blue) = (' + this.red + ', ' + this.green + ', ' + this.blue + ')'
}

function _str(val) {
  let toStr = val._str || val.toString;
  return toStr.call(val);
}

function _strcat(valA, valB) {
  return valA._str() + valB._str();
}

//==================================================================
//              同期/排他
//==================================================================
function _genSyncTimer(count, autoReset) {
  if (!_isIntInRange(count, _syncTimer.minCount, _syncTimer.maxCount))
    throw _newBhProgramException(
      _textDb.errMsg.invalidSyncTimerInitVal(_syncTimer.minCount, _syncTimer.maxCount, count));

  return bhScriptHelper.factory.newSyncTimer(count, autoReset);
}

function _syncTimerCountdown(timer) {
  if (timer === _syncTimer.nil)
    return;
  timer.countdown();
}

/**
 * カウントダウンしてタイマーが 0 になるのを待つ
 * @param timer 同期タイマーオブジェクト
 * @param timeout タイムアウト値 (sec).
 */
function _syncTimerCountdownAndAwait(timer, timeout) {
  if (timer === _syncTimer.nil) {
    return;
  }
  if (timeout === (void 0) || timeout === null) {
    timer.countdownAndAwait();
    return;
  }

  let max = Math.floor(_nearestLongMax / 1024);
  if (!_isInRange(timeout, 0, max)) {
    throw _newBhProgramException(_textDb.errMsg.invalidSyncTimerWaitTime(0, max, timeout));
  }
  let millis = Math.round(timeout * 1000);
  try {
    timer.countdownAndAwaitInterruptibly(millis, _jTimeUnit.MILLISECONDS);
  } catch (e) {
    if (!(e.javaException instanceof _jTimeoutException)) {
      _addExceptionMsg('_syncTimerCountdownAndAwait()');
      throw e;
    }
  }
}

/**
 * タイマーが 0 になるのを待つ
 * @param timer 同期タイマーオブジェクト
 * @param timeout タイムアウト値 (sec).
 */
function _syncTimerAwait(timer, timeout) {
  if (timer === _syncTimer.nil) {
    return;
  }
  if (timeout === (void 0) || timeout === null) {
    timer.await();
    return;
  }

  let max = Math.floor(_nearestLongMax / 1024);
  if (!_isInRange(timeout, 0, max)) {
    throw _newBhProgramException(_textDb.errMsg.invalidSyncTimerWaitTime(0, max, timeout));
  }
  let millis = Math.round(timeout * 1000);
  try {
    timer.awaitInterruptibly(millis, _jTimeUnit.MILLISECONDS);
  } catch (e) {
    if (!(e.javaException instanceof _jTimeoutException)) {
      _addExceptionMsg('_syncTimerAwait()');
      throw e;
    }
  }
}

function _resetSyncTimer(timer, count) {
  if (timer === _syncTimer.nil)
    return;

  if (!_isIntInRange(count, _syncTimer.minCount, _syncTimer.maxCount))
    throw _newBhProgramException(
      _textDb.errMsg.invalidSyncTimerResetVal(_syncTimer.minCount, _syncTimer.maxCount, count));
  
  timer.reset(count);
}

function _getSyncTimerCount(timer) {
  if (timer === _syncTimer.nil)
    return 0;

  return timer.getCount();
}

let _MAX_SPEED = 9;
let _MIN_SPEED = 0;

function _clampSpeedAndTime(speed, time) {
  if (Number.isNaN(speed)) {
    throw new _newBhProgramException(_textDb.errMsg.invalidMoveSpeed(speed));
  }
  if (Number.isNaN(time)) {
    throw new _newBhProgramException(_textDb.errMsg.invalidMoveTime(time));
  }
  time = _clamp(time, 0, Number.MAX_VALUE);
  speed = _clamp(speed, _MIN_SPEED, _MAX_SPEED);
  return [speed, time];
}
