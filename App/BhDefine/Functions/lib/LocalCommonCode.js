
	let _MAX_SPEED = 10;
	let _MIN_SPEED = 1;

	function _moveForward(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間 「前進」 した");
		throw _newBhProgramExceptioin.call(this, "sokudo ihan");
	}

	function _moveBackward(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間 「後退」 した");
	}

	function _turnRight(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間 「右旋回」 した");
	}

	function _turnLeft(speed, time) {
		speed = Math.min(Math.max(_MIN_SPEED, speed), _MAX_SPEED);
		_println("速度 " + speed + " で " + time + " 秒間 「左旋回」 した");
	}

	function _measureDistance() {

		let dist = null;
		while (true) {
			let distStr = _scan('\n距離を入力してください (標準入力に数字で)');
			distStr = _fullWidthToHalf(distStr);
			if (distStr.match(/^[+-]?(\d*\.\d+|\d+\.?\d*)([eE][+-]?\d+)?$/)) {
				dist = Number(distStr);
			}
			else {
				_println('不正な入力です (' + distStr + ')');
				continue;
			}
			break;
		}

		_println('入力された距離 = ' + dist);
		return dist;
	}

	function _sayOnWindows(word) {

		word = word.replace(/"/g, '');
		let wavFilePath = _jPaths.get(bhScriptHelper.getExecPath(), 'Actions', 'open_jtalk', _getSerialNo() + '.wav').toAbsolutePath();
		let sayCmdPath = _jPaths.get(bhScriptHelper.getExecPath(), 'Actions', 'bhSay.cmd').toAbsolutePath();
		let procBuilder = new _jProcBuilder(sayCmdPath.toString(), '"' + word + '"', wavFilePath.toString());
		let success = false;
		try {
			let process = procBuilder.start();
			_waitProcEnd(process, false, true);
			_playWavFile.call(this, wavFilePath);
			success = true;
		}
		finally {
			if (!success)
				_addExceptionMsg.call(this, '_sayOnWindows()');
		}

		procBuilder = new _jProcBuilder('cmd', '/C', 'del', '/F', wavFilePath.toString());
		success = false;
		try {
			let process = procBuilder.start();
			_waitProcEnd(process, false, true);
			success = true;
		}
		finally {
			if (!success)
				_addExceptionMsg.call(this, '_sayOnWindows() del');
		}
	}

	function _say(word) {

		word = word.replace(/\r?\n/g, ' ');
		if (word === '')
			return;

		if (bhScriptHelper.PLATFORM.isWindows())
			_sayOnWindows.call(this, word);
		else if (bhScriptHelper.PLATFORM.isLinux())
			_sayOnLinux.call(this, word);
	}

	function _detectColor() {

		let color = (function () {
			let colorID = null;
			while (true) {
				let colorIdStr = _scan(
					'\n色を入力してください (標準入力に数字で)\n' +
					'    0: 赤,  ' +'1: 緑,  ' +'2: 青,  ' + '3: 水色,  ' + '4: 紫,  ' + '5: きいろ,  ' + '6: 白,  ' + '7: 黒');

				colorIdStr = _fullWidthToHalf(colorIdStr);
				if (colorIdStr.match(/^\d$/))
					colorID = Number(colorIdStr);
				else
					colorID = null;

				switch (colorID) {
					case 0:
						return new _Color(255, 0, 0);
					case 1:
						return new _Color(0, 255, 0);
					case 2:
						return new _Color(0, 0, 255);
					case 3:
						return new _Color(0, 255, 255);
					case 4:
						return new _Color(255, 0, 255);
					case 5:
						return new _Color(255, 255, 0);
					case 6:
						return new _Color(255, 255, 255);
					case 7:
						return new _Color(0,0,0);
					default:
						_println('色検出 不正な入力です (' + colorIdStr + ')');
				}
			}
		})();
		_println('入力された色 = ' + _toStr(color));
		return color;
	}

	function _lightEye(eyeSel, color) {

		let eye;
		switch (eyeSel) {
			case 'both':
				eye = '両目';
				break;
			case 'right':
				eye = '右目';
				break;
			case 'left':
				eye = '左目';
				break;
			default:
				_println('_lightEye  不正な入力です (' + colorIdStr + ')');
				return;
		}

		_println(eye + 'が 「' +  _toStr(color) + '」 になった');
	}

