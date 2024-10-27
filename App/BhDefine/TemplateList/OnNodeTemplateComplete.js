(function() {

  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;

  function registerNodeTemplate(bhNodeId, bhNode) {
    bhNodeTemplates.registerNodeTemplate(BhNodeId.create(bhNodeId), bhNode);
  }

  function genBhNode(bhNodeId) {
    return bhNodeTemplates.genBhNode(BhNodeId.create(bhNodeId), bhUserOpeCmd);
  }

  function connect(parentNode, childNode, cnctrPath) {
    let cnctr = parentNode.findSymbolInDescendants(cnctrPath);
    cnctr.connectNode(childNode, bhUserOpeCmd);
  }

  // 移動ノードの初期速度を変更して再登録
  function setInitialMoveSpeed() {
    let newMoveStat = genBhNode('idMoveStat');
    let moveSpeed = newMoveStat.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
    let moveTime = newMoveStat.findSymbolInDescendants('*', 'Arg1', '*', '*', 'Literal', '*');
    moveSpeed.setText('2');
    moveTime.setText('1');
    registerNodeTemplate('idMoveStat', newMoveStat);
  }

  // 音ノードの初期値を変更して再登録
  function setInitialSoundParams() {
    let newFreqSoundLiteral = genBhNode('idFreqSoundLiteral');
    let duration = newFreqSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    let freq = newFreqSoundLiteral.findSymbolInDescendants('*', 'Frequency', '*', '*', 'Literal', '*');
    duration.setText('1');
    freq.setText('500');
    registerNodeTemplate('idFreqSoundLiteral', newFreqSoundLiteral);

    let newScaleSoundLiteral = genBhNode('idScaleSoundLiteral');
    duration = newScaleSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    duration.setText('1');
    registerNodeTemplate('idScaleSoundLiteral', newScaleSoundLiteral);
  }

  // 文字入力ノードの出力メッセージを変更して再登録
  function setScanMsg() {
    let scanExp = genBhNode('idScanExp');
    let msg = scanExp.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
    msg.setText('入力待ちです');
    registerNodeTemplate('idScanExp', scanExp);
  }

  setInitialMoveSpeed();
  setInitialSoundParams();
  setScanMsg();
})();
