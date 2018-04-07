
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
		let moveCmd = execPath + '/Actions' + '/bhMove';
		const procBuilder = new java.lang.ProcessBuilder([moveCmd, cmd, String(time), String(speed)]);
		let process = null;
		try {
			process = procBuilder.start();
			process.waitFor();
		}
		catch (e) {
			_println('ERR: _move ' + cmd + ' ' + e);
		}
		finally {
			if (process !== null) {
				process.getErrorStream().close();
				process.getInputStream().close();
				process.getOutputStream().close();
			}
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

	function readStream(stream) {
		let readByte = [];
		while(true) {
			const rb = stream.read();
			if (rb === -1)
				break;
			readByte.push(rb);
		}
		readByte = Java.to(readByte, "byte[]");
		const string = Java.type("java.lang.String");
		return new string(readByte);
	}

	function _measureDistance() {

		let spiCmd = execPath + '/Actions' + '/bhSpiRead';
		const procBuilder = new java.lang.ProcessBuilder([spiCmd, '20', '5']);
		let distanceList;
		try {
			const process =  procBuilder.start();
			process.waitFor();
			distanceList = readStream(process.getInputStream());
			process.getErrorStream().close();
			process.getInputStream().close();
			process.getOutputStream().close();
		}
		catch (e) {
			_println('ERR: _measureDistance ' + e);
		}
		distanceList = distanceList.split(",")
						.map(function (elem) { return Number(elem); })
						.sort(function(a,b){ return (a < b ? -1 : 1); });

		let distance = Number(distanceList[0]);
		if (!isFinite(distance))
			return 0;
		if (distance >= 450)
			distance = 84581 * Math.pow(distance, -1.224);
		else
			distance = 585242 * Math.pow(distance, -1.54);
		return distance;
	}

	function _say(word) {

		let talkCmd = execPath + '/Actions' + '/bhTalk.sh';
		const procBuilder = new java.lang.ProcessBuilder(['sh', talkCmd, word]);
		try {
			const process =  procBuilder.start();
			process.waitFor();
			process.getErrorStream().close();
			process.getInputStream().close();
			process.getOutputStream().close();
		}
		catch (e) {
			_println('ERR: _say ' + e);
		}
	}


