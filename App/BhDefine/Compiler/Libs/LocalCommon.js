function _moveForward(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.sendCmd('move', 'fwd', String(speed), String(time));
}

function _moveBackward(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.sendCmd('move', 'bwd', String(speed), String(time));
}

function _turnRight(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.sendCmd('move', 'cw', String(speed), String(time));
}

function _turnLeft(speed, time) {
  [speed, time] = _clampSpeedAndTime(speed, time);
  bhScriptHelper.simulator.sendCmd('move', 'ccw', String(speed), String(time));
}

function _stopRaspiCar() {
  bhScriptHelper.simulator.sendCmd('move', 'stop');
}

function _measureDistance() {
  let resp = bhScriptHelper.simulator.sendCmd('measure-distance');
  return Number(resp[0]) * 100;
}

function _detectColor() {
  let resp = bhScriptHelper.simulator.sendCmd('detect-color');
  return new _Color(Number(resp[0]), Number(resp[1]), Number(resp[2]));
}

function _lightEye(eyeSel, color) {
  // 黒の場合はシミュレーションモデルの目のデフォルトの色を指定する.
  if (color.red === 0 && color.green === 0 && color.blue === 0) {
    color = new _Color(-1, -1, -1);
  }

  bhScriptHelper.simulator.sendCmd(
    'light-eye', eyeSel, String(color.red), String(color.green), String(color.blue));
}
