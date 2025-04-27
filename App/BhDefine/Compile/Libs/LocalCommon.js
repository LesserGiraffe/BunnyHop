function _moveForward(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.moveForwardRaspiCar(speed, time);
}

function _moveBackward(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.moveBackwardRaspiCar(speed, time);
}

function _turnRight(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.turnRightRaspiCar(speed, time);
}

function _turnLeft(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
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
  word = bhScriptHelper.util.substringByBytes(word, 990, "Shift_JIS");
  let execPath = bhScriptHelper.util.getExecPath();
  let wavFilePath = _jPaths.get(execPath, 'Actions', 'open_jtalk', _getSerialNo() + '.wav').toAbsolutePath();
  let sayCmdPath = _jPaths.get(execPath, 'Actions', 'bhSay.cmd').toAbsolutePath();
  let procBuilder = new _jProcBuilder(sayCmdPath.toString(), '"' + word + '"', wavFilePath.toString());
  try {
    let process = procBuilder.start();
    _waitProcEnd(process, false, true);
    _playWavFile(wavFilePath);
  } catch (e) {
    _addExceptionMsg('_sayOnWindows()');
    throw e;
  }

  procBuilder = new _jProcBuilder('cmd', '/C', 'del', '/F', wavFilePath.toString());
  try {
    let process = procBuilder.start();
    _waitProcEnd(process, false, true);
  } catch (e) {
    _addExceptionMsg('_sayOnWindows() del');
    throw e;
  }
}

function _say(word) {
  word = word.replace(/\r?\n/g, ' ');
  if (word === '')
    return;
  if (bhScriptHelper.util.platform.isWindows())
    _sayOnWindows(word);
  else if (bhScriptHelper.util.platform.isLinux())
    _sayOnLinux(word);
}

function _detectColor() {
  let rgb = bhScriptHelper.simulator.detectColor();
  return new _Color(rgb[0], rgb[1], rgb[2]);
}

function _lightEye(eyeSel, color) {
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
      throw _newBhProgramException('invalid eye choice');
  }
}
