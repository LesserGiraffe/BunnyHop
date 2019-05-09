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
	let _jSystem = java.lang.System;
	let _jCyclicBarrier = java.util.concurrent.CyclicBarrier;
	let _jStringBuilder = java.lang.StringBuilder;
	let _jIllegalMonitorStateException = java.lang.IllegalMonitorStateException;

	let _eventHandlers = {};
	let _executor = _jExecutors.newFixedThreadPool(16);
	let _programStartingTime = _currentTimeMillis();
	let _noWaitBarrier = _genReusableBarrier(1);
	let _anyObj = {
		_toStr : function() {return '';}
	};

	function _genCallObj() {
		return {_outArgs:[]};
	}

	function _copyArgs(dest, args, beginIdx) {
		dest.length = 0;
		for (let i = beginIdx; i < args.length; ++i)
			dest.push(args[i]);
	}

	function _genLockObj() {
		return new _jLock();
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
	
	function _genReusableBarrier(parties) {
		return new _jCyclicBarrier(parties);
	}
	
	function _awit(barrier) {
		try {
			barrier.await();
		}
		catch(e) {throw e}
	}
	
	function _getNumberWaiting(barrier) {
		return barrier.getNumberWaiting();
	}
	
	function _listToStr(list, listName) {
	
		if (list.length === 0)
			return listName + ' = 空リスト';
	
		let strBuilder = new _jStringBuilder();
		for (let i = 0; i < list.length; ++i) {
			strBuilder
			.append(listName)
			.append(_jString.format('[%.0f] = %s\n', i, list[i]._toStr()));
		}
		return String(strBuilder);
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

	//イベントに応じたイベントハンドラを呼ぶ
	function _fireEvent(event) {
		if (_eventHandlers[event] !== void 0) {
			_eventHandlers[event].forEach(
				function(handler) {
					try {_executor['submit(java.util.concurrent.Callable)'](function() {handler();});}
					catch(e) { throw ('_fireEvent ' + e); }
				}
			);
		}
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

	function _strToNum(strVal) {
		let num = Number(strVal);
		if (isFinite(num))
			return num;
		return 0;
	}

	function _randomInt(min, max) {
		min = Math.ceil(min);
		max = Math.ceil(max);
		return Math.floor(Math.random() * (max - min + 1)) + min;
	}

	function _clamp(v, min, max) {

		if (v < min)
			return min;
		else if (v > max)
			return max;
		return v;
	}

	function _println(arg) {

		if (arg === _anyObj)
			arg = '';

		bhInout.println(arg._toStr());
	}

	function _sleep(sec) {

		if (sec <= 0)
			return;
		try {
			_jThread.sleep(Math.round(sec * 1000));
		}
		catch (e) { throw ('_sleep ' + e); }
	}

	function _scan(str) {
		bhInout.println(str);
		let input = bhInout.scan();
		return (input === null) ? "" : String(input);
	}

	function _currentTimeMillis() {
		return _jSystem.currentTimeMillis();
	}

	function _getTimeSinceProgramStarted() {
		return _currentTimeMillis() - _programStartingTime;
	}

	let _getSerialNo = (function () {
		let counter = new _jAtomicLong();
		return function() {
			return counter.getAndIncrement();
		}
	})();

	//==================================================================
	//							プロセス待ち
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
			byteArray[i] = bhUtil.toByte(readByte[i]);

		return new _jString(byteArray);
	}

	function _waitProcEnd(process, getStdinStr, checkExitVal) {

		let retStr = null;
		try {
			let is = process.getInputStream();
			if (getStdinStr)
				retStr = readStream(is);
			else
				while (is.read() !== -1);
			
			process.waitFor();
			if (checkExitVal && (process.exitValue() !== 0))
				throw '(abnormal process end)';
		}
		catch (e) { throw e; }
		finally {
			process.getErrorStream().close();
			process.getInputStream().close();
			process.getOutputStream().close();
		}
		return retStr;
	}

	//==================================================================
	//							配列操作
	//==================================================================
	function _aryPush(ary, val) {
		ary.push(val);
	}

	function _aryPop(ary) {
		ary.pop();
	}

	function _aryInsert(ary, idx, val) {
		idx = Math.floor(idx);
		if (0 <= idx && idx <= ary.length)
			ary.splice(idx, 0, val);
	}

	function _aryRemove(ary, idx) {
		idx = Math.floor(idx);
		if (0 <= idx && idx < ary.length)
			ary.splice(idx, 1);
	}

	function _aryClear(ary) {
		ary.length = 0;
	}

	function _aryAddAll(aryA, aryB) {
		Array.prototype.push.apply(aryA, aryB);
	}

	function _aryGet(ary, idx, dflt) {
		idx = Math.floor(idx);
		if (0 <= idx && idx < ary.length)
			return ary[idx];
		return dflt;
	}

	function _aryGetLast(ary, dflt) {
		if (ary.length >= 1)
			return ary[ary.length - 1];
		return dflt;
	}

	function _arySet(ary, idx, val) {
		idx = Math.floor(idx);
		if (0 <= idx && idx < ary.length)
			ary[idx] = val;
	}

	//==================================================================
	//							音再生
	//==================================================================

	let _jAudioFormat = javax.sound.sampled.AudioFormat;
	let _jDataLine = javax.sound.sampled.DataLine;
	let _jAudioSystem = javax.sound.sampled.AudioSystem;

	let isBigEndian = _jByteOrder.nativeOrder() === _jByteOrder.BIG_ENDIAN;
	let bytePerSample = 2;
	let samplingRate = 44100;		//44.1KHz
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
	 * */
	function _genOneFreqWave(waveBuf, bufBegin, bufEnd, hz, amp, samplePos) {

		hz = Math.round(hz);
		if (hz < 0 || hz > (samplingRate / 2))
			hz = 0;

		if (samplePos > (wave1Hz.length - 1))
			samplePos = 0;

		for (let i = bufBegin; i < bufEnd; i += bytePerSample) {
			let sample = Math.floor(amp * wave1Hz[samplePos]);
			if (isBigEndian) {
				waveBuf[i+1] = bhUtil.toByte(sample);
				waveBuf[i] = bhUtil.toByte((sample >> 8));
			}
			else {
				waveBuf[i] = bhUtil.toByte(sample);
				waveBuf[i+1] = bhUtil.toByte((sample >> 8));
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
	 * */
	function _genWave(waveBuf, soundList, vol, samplePos) {

		let bufBegin = 0;
		let waveBufRemains = waveBuf.length;

		while (soundList.length !== 0 && waveBufRemains !== 0) {
			let sound = soundList[soundList.length - 1];
			let soundLen = Math.floor(sound.duration * samplingRate) * bytePerSample;

			if (soundLen > waveBufRemains) {
				sound.duration -= waveBufRemains / bytePerSample / samplingRate;
				let bufEnd = bufBegin + waveBufRemains;
				samplePos = _genOneFreqWave(waveBuf, bufBegin, bufEnd, sound.hz, sound.amp * vol, samplePos);
				bufBegin = bufEnd;
				waveBufRemains = 0;
			}
			else {
				soundList.pop();
				let bufEnd = bufBegin + soundLen;
				samplePos = _genOneFreqWave(waveBuf, bufBegin, bufEnd, sound.hz, sound.amp * vol, samplePos);
				bufBegin = bufEnd;
				waveBufRemains -= soundLen;
			}
		}

		return { waveLen : bufBegin,
				  samplePos : samplePos};
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
		}
		else {
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
		}
		catch (e) { throw ('_playMelodies ' + e); }
		finally {
			if (line !== null) {
				line.drain();
				line.close();
			}
		}
	}

	/**
	 * wavファイル再生
	 * @param path 再生したいファイルのパス (java.nio.file.Path オブジェクト)
	 * */
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
		}
		catch (e) { throw ('_playWavFile ' + e); }
		finally {
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
		let talkCmd = String(_jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhSay.sh').toAbsolutePath().toString());
		let procBuilder = new _jProcBuilder('sh', talkCmd, '"' + word + '"');
		try {
			let process =  procBuilder.start();
			_waitProcEnd(process, false, true);
		}
		catch (e) { throw ('_sayOnLinux ' + e); }
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
				throw ('_createColorFromName invalid colorName ' + colorName);
		}
		return null;
	}

	function _compareColors(colorA, colorB, eq) {

		let equality = (colorA.red === colorB.red) && (colorA.green === colorB.green) && (colorA.blue === colorB.blue);
		if (eq === 'eq')
			return equality;
		else if (eq === 'neq')
			return !equality;
		else
			throw ('_compareColors invalid eq ' + eq);

		return null;
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
	//							文字列化
	//==================================================================
	Number.prototype._toStr = function () {
		return this.toString();
	}

	Boolean.prototype._toStr = function () {
		if (this == true)
			return '真';
		return '偽';
	}

	String.prototype._toStr = function () {
		return this.toString();
	}

	_Sound.prototype._toStr = function () {
		return '高さ: ' + this.hz + '[ヘルツ],  長さ: ' + Math.round(this.duration * 1000.0) + '[ミリ秒]';
	}

	_Color.prototype._toStr = function () {
		if (this.red === 255 && this.green === 0 && this.blue === 0)
			return '赤';
		else if (this.red === 0 && this.green === 255 && this.blue === 0)
			return '緑';
		else if (this.red === 0 && this.green === 0 && this.blue === 255)
			return '青';
		else if (this.red === 0 && this.green === 255 && this.blue === 255)
			return '水色';
		else if (this.red === 255 && this.green === 0 && this.blue === 255)
			return '紫';
		else if (this.red === 255 && this.green === 255 && this.blue === 0)
			return 'きいろ';
		else if (this.red === 255 && this.green === 255 && this.blue === 255)
			return '白';
		else if (this.red === 0 && this.green === 0 && this.blue === 0)
			return '黒';

		return '(red, green, blue) = (' + this.red + ', ' + this.green + ', ' + this.blue + ')'
	}

	function _toStr(val) {
		return val._toStr();
	}

	function _strcat(valA, valB) {
		return valA._toStr() + valB._toStr();
	}
