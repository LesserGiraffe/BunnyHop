	let _jThread = Java.type('java.lang.Thread');
	let _jExecutors = Java.type('java.util.concurrent.Executors');
	let _jLock = Java.type('java.util.concurrent.locks.ReentrantLock');
	let _jByteOrder = Java.type('java.nio.ByteOrder');
	let _jByteArray = Java.type('byte[]');
	let _jFloatArray = Java.type('float[]');

	let _eventHandlers = {};
	let _executor = _jExecutors.newFixedThreadPool(16);

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

	function _boolToStr(boolVal) {
		return (boolVal === true) ? '真' : '偽';
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

	function _println(str) {
		inout.println(str);
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
		inout.println(str);
		let input = inout.scan();
		return (input === null) ? "" : input;
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

	function _arySet(ary, idx, val) {
		idx = Math.floor(idx);
		if (0 <= idx && idx < ary.length)
			ary[idx] = val;
	}

	//==================================================================
	//							音再生
	//==================================================================

	let _jAudioFormat = Java.type('javax.sound.sampled.AudioFormat');
	let _jDataLine = Java.type('javax.sound.sampled.DataLine');
	let _jSourceDataLine = Java.type('javax.sound.sampled.SourceDataLine');
	let _jAudioSystem = Java.type('javax.sound.sampled.AudioSystem');

	const isBigEndian = _jByteOrder.nativeOrder() === _jByteOrder.BIG_ENDIAN;
	const bytePerSample = 2;
	const samplingRate = 44100;		//44.1KHz
	let amp = 16384/2;
	const wave1Hz = new _jFloatArray(samplingRate);
	const waveBuf = new _jByteArray(samplingRate * bytePerSample);

	(function _initMusicPlayer() {
		for (let i = 0; i < wave1Hz.length; ++i)
			wave1Hz[i] = Math.cos(2.0 * Math.PI * i / samplingRate);
	})();

	//波形生成
	function _genWave(waveBuf, hz, amp) {

		let samplePos = 0;
		hz = Math.round(hz);
		if (hz < 0 || hz > (samplingRate / 2))
			hz = 0;

		for (let i = 0; i < waveBuf.length; i += bytePerSample) {
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
			if (samplePos >= wave1Hz.length)
				samplePos -= wave1Hz.length;
		}
	}

	//メロディ再生
	function _playMelodies(melodyList, reverse) {

		if (reverse)
			melodyList = melodyList.reverse();

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

			melodyList.forEach(
				function (sound) {
					if (sound.duration !== 0) {
						_genWave(waveBuf, sound.hz, amp);
						let integer = Math.floor(sound.duration);
						let fractional = sound.duration - integer;
						for (let i = 0; i < integer; ++i)
							line.write(waveBuf, 0, waveBuf.length);
						line.write(waveBuf, 0, Math.floor(waveBuf.length * fractional));
					}
					sound = sound.next;
				});
		}
		catch (e) { _println("ERR: _playMelodies " + e); }
		finally {
			if (line !== null) {
				line.drain();
				line.close();
			}
		}
	}

	// 音クラス
	function _Sound(hz, duration) {
		this.hz = hz;
		this.duration = duration;
		this.next = null;
	}

	function _createSound(hz, duration) {
		return new _Sound(hz, duration);
	}

	function _pushSound(sound, soundList) {
		soundList.push(sound);
		return soundList;
	}

