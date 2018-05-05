
	const _MAX_SPEED = 10;
	const _MIN_SPEED = 1;

	function _moveForward(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間「前進」した")
	}

	function _moveBackward(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間「後退」した")
	}

	function _turnRight(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間「右旋回」した")
	}

	function _turnLeft(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間「左旋回」した")
	}

	function _measureDistance() {

		let dist = _scan('距離を入力してください (標準入力に半角で)');
		dist = Number(dist);
		if (!isFinite(dist))
			dist = 0;

		_println("距離 = " + dist);
		return dist;
	}

	function _sayOnWindows(word) {

		word = word.replaceAll('\"', '');
		const wavFilePath = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'open_jtalk', _getSerialNo() + '.wav').toAbsolutePath();
		const sayCmdPath = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhSay.cmd').toAbsolutePath();
		let procBuilder = new _jProcBuilder([sayCmdPath.toString(), '"' + word + '"', wavFilePath.toString()]);
		try {
			const process = procBuilder.start();
			_waitProcEnd(process, 'ERR: _say ', false);
		}
		catch (e) {
			_println('ERR: _say ' + e);
		}

		_playWavFile(wavFilePath);

		procBuilder = new _jProcBuilder(['cmd', '/C', 'del', '/F', wavFilePath.toString()]);
		try {
			const process = procBuilder.start();
			_waitProcEnd(process, 'ERR: say del ', false);
		}
		catch (e) {
			_println('ERR: say del ' + e);
		}

	}

	function _say(word) {

		if (bhUtil.PLATFORM.isWindows())
			_sayOnWindows(word);
		else if (bhUtil.PLATFORM.isLinux())
			_sayOnLinux(word);
	}

