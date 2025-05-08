let _MOVE_CMD = {
  FORWARD: 'fwd',
  BACKWARD: 'bwd',
  CLOCKWISE: 'cw',
  COUNTERCLOCKWISE: 'ccw',
  STOP: 'stop'
};

function _isCmdSuccessful(cmdResult) {
  return String(cmdResult[0].toLowerCase()) === 'true';
}

function _moveAny(speed, time, cmd) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  speed = 0.0544 * speed + 0.1456;
  try {
    let command = ['move', cmd, String(speed), String(time)];
    for (let i = 0; i < 3; ++i) {
      let res = bhScriptHelper.hw.sendCmd(command);
      if (_isCmdSuccessful(res)) {
        return;
      }
    }
    throw _newBhProgramEexception(_textDb.errMsg.failedToCtrlHw(command.join()));
  } catch (e) {
    throw e;
  }
}

function _moveForward(speed, time) {
  _moveAny(speed, time, _MOVE_CMD.FORWARD);
}

function _moveBackward(speed, time) {
  _moveAny(speed, time, _MOVE_CMD.BACKWARD);
}

function _turnRight(speed, time) {
  _moveAny(speed, time, _MOVE_CMD.CLOCKWISE);
}

function _turnLeft(speed, time) {
  _moveAny(speed, time, _MOVE_CMD.COUNTERCLOCKWISE);
}

function _stopRaspiCar() {
  try {
    let command = ['move', _MOVE_CMD.STOP];
    for (let i = 0; i < 3; ++i) {
      let res = bhScriptHelper.hw.sendCmd(command);
      if (_isCmdSuccessful(res)) {
        return;
      }
    }
    throw _newBhProgramEexception(_textDb.errMsg.failedToCtrlHw(command.join()));
  } catch (e) {
    throw e;
  }
}

function _measureDistance() {
  try {
    let command = 'measure-distance';
    for (let i = 0; i < 3; ++i) {
      let res = bhScriptHelper.hw.sendCmd(command);
      if (_isCmdSuccessful(res)) {
        return Number(String(res[2])) / 10000;
      }
    }
    throw _newBhProgramEexception(_textDb.errMsg.failedToCtrlHw(command.join()));
  } catch (e) {
    throw e;
  }
}

function _say(word) {
  word = word.replace(/\r?\n/g, ' ');
  if (word === '')
    return;
  _sayOnLinux(word);
}

// 色センサ値を取得
function _getColor() {
  try {
    let exposureTime = 0.05; // sec
    let command = ['detect-color', exposureTime];
    for (let i = 0; i < 10; ++i) {
      let res = bhScriptHelper.hw.sendCmd(command);
      if (_isCmdSuccessful(res)) {
        return new _Color(Number(String(res[2])), Number(String(res[3])), Number(String(res[4])));
      }
    }
    throw _newBhProgramEexception(_textDb.errMsg.failedToCtrlHw(command.join()));
  } catch (e) {
    throw e;
  }
}

let _baseLineColor = _getColor();

function calcColorFeature(color) {
  let calibrated = new _Color(
    Math.max(color.red - _baseLineColor.red, 1),
    Math.max(color.green - _baseLineColor.green, 1),
    Math.max(color.blue - _baseLineColor.blue, 1));
  let ave = (calibrated.red + calibrated.green + calibrated.blue) / 3;
  let variance = (calibrated.red * calibrated.red + calibrated.green * calibrated.green + calibrated.blue * calibrated.blue) / 3.0 - ave * ave;
  let coefOfVar = Math.sqrt(variance) / ave;
  let max = Math.max(Math.max(calibrated.red, calibrated.green), calibrated.blue);
  let min = Math.min(Math.min(calibrated.red, calibrated.green), calibrated.blue);
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
  let color = _getColor();
  let blackThresh = 2;
  if (color.red <= _baseLineColor.red * blackThresh &&
    color.green <= _baseLineColor.green * blackThresh &&
    color.blue <= _baseLineColor.blue * blackThresh) {
    return new _Color(0, 0, 0);
  }
  let feature = calcColorFeature(color);
  let whiteThresh = 0.31;
  let redYellowThresh = 12;
  let yellowGreenThresh = 85;
  let greenCyanThresh = 170;
  let cyanBlueThresh = 219;
  let blueMagentaThresh = 245;
  let magentaRedThresh = 348;
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

function _lightEye(eyeSel, color) {
  try {
    let command = ['light-eye', eyeSel];
    command.push(color.red === 0 ? '0' : '1');
    command.push(color.green === 0 ? '0' : '1');
    command.push(color.blue === 0 ? '0' : '1');
    let res = bhScriptHelper.hw.sendCmd(command);
    if (_isCmdSuccessful(res)) {
      return;
    }
    throw _newBhProgramEexception(_textDb.errMsg.failedToCtrlHw(command.join()));
  } catch (e) {
    throw e;
  }
}
