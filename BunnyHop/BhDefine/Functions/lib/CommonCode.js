	let _jThread = Java.type('java.lang.Thread');
	let _jExecutors = Java.type('java.util.concurrent.Executors');
	let _jLock = Java.type('java.util.concurrent.locks.ReentrantLock');
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
					catch(e) {}
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
		catch (e) {}
	}

	function _scan(str) {
		inout.println(str);
		let input = inout.scan();
		return (input === null) ? "" : input;
	}

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

