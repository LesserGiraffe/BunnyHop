
  let _MAX_SPEED = 10;
  let _MIN_SPEED = 1;

  function _moveForward(speed, time) {
    bhScriptHelper.simulator.moveForwardRaspiCar(speed, time);
  }

  function _moveBackward(speed, time) {
    bhScriptHelper.simulator.moveBackwardRaspiCar(speed, time);
  }

  function _turnRight(speed, time) {
    bhScriptHelper.simulator.turnRightRaspiCar(speed, time);
  }

  function _turnLeft(speed, time) {
    bhScriptHelper.simulator.turnLeftRaspiCar(speed, time);
  }

  function _stopRaspiCar() {
    bhScriptHelper.simulator.stopRaspiCar();
  }

  function _measureDistance() {
    return bhScriptHelper.simulator.measureDistance();
  }

  function _sayOnWindows(word) {

    word = word.replace(/"/g, '');
    let execPath = bhScriptHelper.util.getExecPath();
    let wavFilePath = _jPaths.get(execPath, 'Actions', 'open_jtalk', _getSerialNo() + '.wav').toAbsolutePath();
    let sayCmdPath = _jPaths.get(execPath, 'Actions', 'bhSay.cmd').toAbsolutePath();
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

    if (bhScriptHelper.util.platform.isWindows())
      _sayOnWindows.call(this, word);
    else if (bhScriptHelper.util.platform.isLinux())
      _sayOnLinux.call(this, word);
  }

  function _detectColor() {
    let rgb = bhScriptHelper.simulator.detectColor();
    return new _Color(rgb[0], rgb[1], rgb[2]);
  }

  function _lightEye(eyeSel, color) {
    let eye;
    switch (eyeSel) {
      case 'both':
        bhScriptHelper.simulator.setBothEyesColor(color.red, color.green, color.blue);
        break;
      case 'right':
        bhScriptHelper.simulator.setRightEyeColor(color.red, color.green, color.blue);
        break;
      case 'left':
        bhScriptHelper.simulator.setLeftEyeColor(color.red, color.green, color.blue);
        break;
      default:
        throw _newBhProgramExceptioin.call(this, 'invalid eye choice');
    }
  }

