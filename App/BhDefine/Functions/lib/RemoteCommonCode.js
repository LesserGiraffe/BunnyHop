
  let _MOVE_CMD = {
    MOVE_FORWARD : '1',
    MOVE_BACKWARD : '2',
    TURN_RIGHT : '3',
    TURN_LEFT : '4'
  };

  function _moveAny(speed, time, cmd) {

    let _MAX_SPEED = 10;
    let _MIN_SPEED = 1;
    speed = (speed - _MIN_SPEED) / (_MAX_SPEED - _MIN_SPEED);
    speed = Math.min(Math.max(0.0, speed), 1.0);
    time *= 1000;
    time = Math.floor(time);
    let path = _jPaths.get(bhScriptHelper.util.getExecPath(), 'Actions', 'bhMove');
    let moveCmd = String(path.toAbsolutePath().toString());
    let procBuilder = new _jProcBuilder(moveCmd, String(cmd), String(time), String(speed));
    let success = false;
    try {
      let process = procBuilder.start();
      _waitProcEnd(process, false, true);
      success = true;
    }
    finally {
      if (!success)
        _addExceptionMsg('_moveAny()  ' + cmd);
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
    let path = _jPaths.get(bhScriptHelper.util.getExecPath(), 'Actions', 'bhSpiRead');
    let spiCmd = String(path.toAbsolutePath().toString());
    let procBuilder = new _jProcBuilder(spiCmd, '20', '5');
    let distanceList;
    let success = false;
    try {
      let process =  procBuilder.start();
      distanceList = _waitProcEnd(process, true, true);
      success = true;
    }
    finally {
      if (!success)
        _addExceptionMsg('_measureDistance()');
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

    word = word.replace(/\r?\n/g, ' ');
    if (word === '')
      return;

    _sayOnLinux(word);
  }

  // 色センサ値を取得
  function _getColor() {

    let exposureTime = 50;  //ms
    let retryTimes = 10;
    let path = _jPaths.get(bhScriptHelper.util.getExecPath(), 'Actions', 'bhColorDetection');
    let cmd = String(path.toAbsolutePath().toString());
    let procBuilder = new _jProcBuilder(cmd, String(exposureTime), String(retryTimes));
    let colorList = null;
    let success = false;
    
    try {
      let process =  procBuilder.start();
      colorList = _waitProcEnd(process, true, true);
      success = true;
    }
    finally {
      if (!success)
        _addExceptionMsg('_detectColor()');
    }

    colorList = colorList.split(",");
    return new _Color(Number(colorList[0]), Number(colorList[1]), Number(colorList[2]));
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
    let blackThresh = 0.5;
    let rateOfDetectableChange = 2;

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

  function _ioWrite(port, val) {
    
    let procBuilder = new _jProcBuilder('gpio', '-g', 'write', String(port), String(val));
    let success = false;
    try {
      let process =  procBuilder.start();
      _waitProcEnd(process, false, false);
      success = true;
    }
    finally {
      if (!success)
        _addExceptionMsg('_ioWrite()');
    }
  }

  function _changeIoMode(port, mode) {
    
    let procBuilder = new _jProcBuilder('gpio', '-g', 'mode', String(port), String(mode));
    let success = false;
    try {
      let process =  procBuilder.start();
      _waitProcEnd(process, false, false);
      success = true;
    }
    finally {
      if (!success)
        _addExceptionMsg('_changeIoMode()');
    }
  }

  (function _initEyeLight() {

    let ioList = ['21', '20', '16', '13', '19', '26'];
    ioList.forEach(function (port) {_changeIoMode(port, 'out');});
    ioList.forEach(function (port) {_ioWrite(port, '0');});
  })();

  function _lightEye(eyeSel, color) {

    let rightRed = '13';
    let rightGreen = '26';
    let rightBlue = '19';
    let leftRed = '21';
    let leftGreen = '16';
    let leftBlue = '20';

    let hiList = [];
    let loList = [];
    if (eyeSel === 'both' || eyeSel === 'right') {
      if (color.red === 0)
        loList.push(rightRed);
      else
        hiList.push(rightRed);

      if (color.green === 0)
        loList.push(rightGreen);
      else
        hiList.push(rightGreen);

      if(color.blue === 0)
        loList.push(rightBlue);
      else
        hiList.push(rightBlue);
    }
    if (eyeSel === 'both' || eyeSel === 'left') {
      if (color.red === 0)
        loList.push(leftRed);
      else
        hiList.push(leftRed);

      if (color.green === 0)
        loList.push(leftGreen);
      else
        hiList.push(leftGreen);

      if(color.blue === 0)
        loList.push(leftBlue);
      else
        hiList.push(leftBlue);
    }

    hiList.forEach(function (port) {_ioWrite(port, '1');});
    loList.forEach(function (port) {_ioWrite(port, '0');});
  }
