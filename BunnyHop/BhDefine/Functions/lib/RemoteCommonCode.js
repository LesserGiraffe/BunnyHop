
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

	// 色センサ値を取得
	function _getColor() {

		const exposureTime = 50;	//ms
		const retryTimes = 10;
		let cmd = _jPaths.get(bhUtil.EXEC_PATH, 'Actions', 'bhColorDetection').toAbsolutePath().toString();
		const procBuilder = new _jProcBuilder([cmd, String(exposureTime), String(retryTimes)]);
		let colorList = null;

		try {
			const process =  procBuilder.start();
			colorList = _waitProcEnd(process, 'ERR: _detectColor ', true);
			if (process.exitValue() !== 0)
				throw "(color snsor error)";
		}
		catch (e) {
			_println('ERR: _detectColor ' + e);
			return new _Color(0, 0, 0);
		}

		colorList = colorList.split(",");
		return new _Color(Number(colorList[0]), Number(colorList[1]), Number(colorList[2]));

	}

	const _baseLineColor = _getColor();

	function calcColorFeature(color) {

		const calibrated = new _Color(
			Math.max(color.red - _baseLineColor.red, 1),
			Math.max(color.green - _baseLineColor.green, 1),
			Math.max(color.blue - _baseLineColor.blue, 1));

		let ave = (calibrated.red + calibrated.green + calibrated.blue) / 3;
		let variance = (calibrated.red * calibrated.red + calibrated.green * calibrated.green + calibrated.blue * calibrated.blue) / 3.0 - ave * ave;
		let coefOfVar = Math.sqrt(variance) / ave;

		const max = Math.max(Math.max(calibrated.red, calibrated.green), calibrated.blue);
		const min = Math.min(Math.min(calibrated.red, calibrated.green), calibrated.blue);
		let hue;
		if (max === min)
			hue = 0;
		else if (max === calibrated.red)
			hue = 60 * (calibrated.green - calibrated.blue) / (max - min);
		else if (max === calibrated.green)
			hue = 60 * (calibrated.blue - calibrated.red) / (max - min) + 120;
		else
			hue = 60 * (calibrated.red - calibrated.green) / (max - min) + 240;

		let saturation = (max - min) / max;

		if (hue < 0)
			hue += 360;

		return {hue:hue, coefOfVar:coefOfVar};
	}

	// 色センサ値を取得し8色に分類する.
	function _detectColor() {

		const color = _getColor();
		const blackThresh = 0.5;
		const rateOfDetectableChange = 2;

		if (color.red <= _baseLineColor.red * blackThresh &&
			color.green <= _baseLineColor.green * blackThresh &&
			color.blue <= _baseLineColor.blue * blackThresh) {
			return new _Color(0, 0, 0);
		}

		if (color.red < _baseLineColor.red * rateOfDetectableChange &&
			color.green < _baseLineColor.green * rateOfDetectableChange &&
			color.blue < _baseLineColor.blue * rateOfDetectableChange) {
			return new _Color(255, 255, 255);
		}

		const feature = calcColorFeature(color);
		const whiteThresh = 0.31;
		const redYellowThresh = 12;
		const yellowGreenThresh = 85;
		const greenCyanThresh = 170;
		const cyanBlueThresh = 219;
		const blueMagentaThresh = 245;
		const magentaRedThresh = 348;

		if (feature.coefOfVar <= whiteThresh)
			return new _Color(255, 255, 255);

		if (feature.hue < redYellowThresh || magentaRedThresh <= feature.hue)
			return new _Color(255, 0, 0);

		if (feature.hue < yellowGreenThresh)
			return new _Color(255, 255, 0);

		if (feature.hue < greenCyanThresh)
			return new _Color(0, 255, 0);

		if (feature.hue < cyanBlueThresh)
			return new _Color(0, 255, 255);

		if (feature.hue < blueMagentaThresh)
			return new _Color(0, 0, 255);

		return new _Color(255, 0, 255);
	}











