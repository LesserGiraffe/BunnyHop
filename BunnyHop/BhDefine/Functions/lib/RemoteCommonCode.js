
	const _MOVE_CMD = {
		MOVE_FORWARD : '1',
		MOVE_BACKWARD : '2',
		TURN_RIGHT : '3',
		TURN_LEFT : '4'
	};

	function _moveAny(speed, time, cmd) {

		const _MAX_SPEED = 10;
		const _MIN_SPEED = 1;
		speed = (speed - _MIN_SPEED) / (_MAX_SPEED - _MIN_SPEED);
		speed = Math.min(Math.max(0.0, speed), 1.0);
		time *= 1000;
		time = Math.floor(time);
		let moveCmd = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhMove').toAbsolutePath().toString();
		const procBuilder = new _jProcBuilder([moveCmd, cmd, String(time), String(speed)]);
		try {
			const process = procBuilder.start();
			_waitProcEnd(process, 'ERR: _move ' + cmd + ' ', false);
		}
		catch (e) {
			_println('ERR: _move ' + cmd + ' ' + e);
		}
	}

	function _moveForward(speed, time) {
		_moveAny(speed, time, _MOVE_CMD.MOVE_FORWARD);
	}

	function _moveBackward(speed, time) {
		_moveAny(speed, time, _MOVE_CMD.MOVE_BACKWARD);
	}

	function _turnRight(speed, time) {
		_moveAny(speed, time, _MOVE_CMD.TURN_RIGHT);
	}

	function _turnLeft(speed, time) {
		_moveAny(speed, time, _MOVE_CMD.TURN_LEFT);
	}

	function _measureDistance() {

		let spiCmd = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhSpiRead').toAbsolutePath().toString();
		const procBuilder = new _jProcBuilder([spiCmd, '20', '5']);
		let distanceList;
		try {
			const process =  procBuilder.start();
			distanceList = _waitProcEnd(process, 'ERR: _measureDistance ', true);
		}
		catch (e) {
			_println('ERR: _measureDistance ' + e);
			return 0;
		}
		distanceList = distanceList.split(",")
						.map(function (elem) { return Number(elem); })
						.sort(function(a,b){ return (a < b ? -1 : 1); });

		let distance = distanceList[0];
		if (!isFinite(distance))
			return 0;
		if (distance >= 450)
			distance = 84581 * Math.pow(distance, -1.224);
		else
			distance = 585242 * Math.pow(distance, -1.54);

		return distance;
	}

	function _say(word) {
		_sayOnLinux(word);
	}


