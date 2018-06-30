	const _jString = Java.type("java.lang.String");
	const _jExecutors = Java.type('java.util.concurrent.Executors');
	const _jThread = Java.type('java.lang.Thread');
	const _jLock = Java.type('java.util.concurrent.locks.ReentrantLock');
	const _jByteOrder = Java.type('java.nio.ByteOrder');
	const _jByteArray = Java.type('byte[]');
	const _jFloatArray = Java.type('float[]');
	const _jFile = Java.type('java.io.File');
	const _jFiles = Java.type('java.nio.file.Files');
	const _jPaths = Java.type('java.nio.file.Paths');
	const _jAtomicLong = Java.type('java.util.concurrent.atomic.AtomicLong');
	const _jProcBuilder = Java.type('java.lang.ProcessBuilder');
	const _jBufferedInputStream = Java.type('java.io.BufferedInputStream');
	const _jStandardOpenOption = Java.type('java.nio.file.StandardOpenOption');

	const _eventHandlers = {};
	const _executor = _jExecutors.newFixedThreadPool(16);
	const _anyObj = {};

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

	function _unlock(lockObj) {
		lockObj.unlock();
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
					catch(e) { _println("ERR: _fireEvent " + e); }
				}
			);
		}
	}

	function _strToNum(strVal) {
		const num = Number(strVal);
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
		catch (e) { _println("ERR: _sleep " + e); }
	}

	function _scan(str) {
		bhInout.println(str);
		let input = bhInout.scan();
		return (input === null) ? "" : input;
	}

	const _getSerialNo = (function () {
		const counter = new _jAtomicLong();
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
			const rb = stream.read();
			if (rb === -1)
				break;
			readByte.push(rb);
		}
		readByte = Java.to(readByte, "byte[]");
		return new _jString(readByte);
	}

	function _waitProcEnd(process, errMsg, getStdinStr) {

		let retStr = null;
		try {
			const is = process.getInputStream();
			if (getStdinStr)
				retStr = readStream(is);
			else
				while (is.read() !== -1);
			process.waitFor();
		}
		catch (e) {
			_println(errMsg + e);
		}
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

	const _jAudioFormat = Java.type('javax.sound.sampled.AudioFormat');
	const _jDataLine = Java.type('javax.sound.sampled.DataLine');
	const _jSourceDataLine = Java.type('javax.sound.sampled.SourceDataLine');
	const _jAudioSystem = Java.type('javax.sound.sampled.AudioSystem');

	const isBigEndian = _jByteOrder.nativeOrder() === _jByteOrder.BIG_ENDIAN;
	const bytePerSample = 2;
	const samplingRate = 44100;		//44.1KHz
	const amplitude = 16384/2;
	const wave1Hz = new _jFloatArray(samplingRate);
	const _nilSound = _createSound(0, 0);


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
			const sample = Math.floor(amp * wave1Hz[samplePos]);
			if (isBigEndian) {
				waveBuf[i+1] = sample;
				waveBuf[i] = (sample >> 8);
			}
			else {
				waveBuf[i] = sample;
				waveBuf[i+1] = (sample >> 8);
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
			const sound = soundList[soundList.length - 1];
			const soundLen = Math.floor(sound.duration * samplingRate) * bytePerSample;

			if (soundLen > waveBufRemains) {
				sound.duration -= waveBufRemains / bytePerSample / samplingRate;
				const bufEnd = bufBegin + waveBufRemains;
				samplePos = _genOneFreqWave(waveBuf, bufBegin, bufEnd, sound.hz, sound.amp * vol, samplePos);
				bufBegin = bufEnd;
				waveBufRemains = 0;
			}
			else {
				soundList.pop();
				const bufEnd = bufBegin + soundLen;
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

		const soundListCopy = [];
		if (!reverse) {
			for (let i = soundList.length - 1; i >= 0; --i)
				soundListCopy.push(_createSound(soundList[i].hz, soundList[i].duration));
		}
		else {
			for (let i = 0; i < soundList.length; ++i)
				soundListCopy.push(_createSound(soundList[i].hz, soundList[i].duration));
		}

		const waveBuf = new _jByteArray(samplingRate * bytePerSample);
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
			let dLineInfo = new _jDataLine.Info(_jSourceDataLine.class, format);
			line = _jAudioSystem.getLine(dLineInfo);
			line.open(format, waveBuf.length);
			line.start();
			let samplePos = 0;
			while (soundListCopy.length !== 0) {
				const ret = _genWave(waveBuf, soundListCopy, 1.0, samplePos);
				samplePos = ret.samplePos;
				line.write(waveBuf, 0, ret.waveLen);
			}
		}
		catch (e) { _println("ERR: _playMelodies " + e); }
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
			const dLineInfo = new _jDataLine.Info(_jSourceDataLine.class, ais.getFormat());
			line = _jAudioSystem.getLine(dLineInfo);
			line.open();
			line.start();
			const waveBuf = new _jByteArray(line.getBufferSize());
			let byteRead = -1;

			while((byteRead = ais.read(waveBuf)) !== -1) {
				line.write(waveBuf, 0, byteRead);
			}
		}
		catch (e) { _println("ERR: _playWavFile " + e); }
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

		word = word.replaceAll('\"', '');
		let talkCmd = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhSay.sh').toAbsolutePath().toString();
		const procBuilder = new _jProcBuilder(['sh', talkCmd, '"' + word + '"']);
		try {
			const process =  procBuilder.start();
			_waitProcEnd(process, 'ERR: _say ', false);
		}
		catch (e) {
			_println('ERR: _say ' + e);
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
				_println('ERR: _createColorFromName invalid colorName ' + colorName);
		}
		return null;
	}

	function _compareColors(colorA, colorB, eq) {

		const equality = (colorA.red === colorB.red) && (colorA.green === colorB.green) && (colorA.blue === colorB.blue);
		if (eq === 'eq')
			return equality;
		else if (eq === 'neq')
			return !equality;
		else
			_println("ERR: _compareColors invalid eq " + eq);

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

	const _nilColor = new _Color(0, 0, 0);

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
		return '高さ: ' + this.hz + '[Hz],  長さ: ' + Math.round(this.duration * 1000.0) + '[ms]';
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

		if (val === _anyObj)
			return '';
		return val._toStr();
	}

