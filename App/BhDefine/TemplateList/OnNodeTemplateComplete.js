(function() {

  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;

  function overwriteNodeTemplate(bhNodeId, bhNode) {
    bhNodeTemplates.overwriteNodeTemplate(BhNodeId.of(bhNodeId), bhNode);
  }

  function genBhNode(bhNodeId) {
    return bhNodeTemplates.genBhNode(BhNodeId.of(bhNodeId), bhUserOpe);
  }

  function connect(parentNode, childNode, cnctrPath) {
    let cnctr = parentNode.findSymbolInDescendants(cnctrPath);
    cnctr.connectNode(childNode, bhUserOpe);
  }

  // 移動ノードの初期速度を変更して再登録
  function setInitialMoveSpeed() {
    let newMoveStat = genBhNode('idMoveStat');
    let moveSpeed = newMoveStat.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
    let moveTime = newMoveStat.findSymbolInDescendants('*', 'Arg1', '*', '*', 'Literal', '*');
    moveSpeed.setText('2');
    moveTime.setText('1');
    overwriteNodeTemplate('idMoveStat', newMoveStat);
  }

  // 音ノードの初期値を変更して再登録
  function setInitialSoundParams() {
    let newFreqSoundLiteral = genBhNode('idFreqSoundLiteral');
    let duration = newFreqSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    let freq = newFreqSoundLiteral.findSymbolInDescendants('*', 'Frequency', '*', '*', 'Literal', '*');
    duration.setText('1');
    freq.setText('500');
    overwriteNodeTemplate('idFreqSoundLiteral', newFreqSoundLiteral);

    let newScaleSoundLiteral = genBhNode('idScaleSoundLiteral');
    duration = newScaleSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    duration.setText('1');
    overwriteNodeTemplate('idScaleSoundLiteral', newScaleSoundLiteral);
  }

  // 文字入力ノードの出力メッセージを変更して再登録
  function setScanMsg() {
    let scanExp = genBhNode('idScanExp');
    let msg = scanExp.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
    msg.setText('入力待ちです');
    overwriteNodeTemplate('idScanExp', scanExp);
  }

  setInitialMoveSpeed();
  setInitialSoundParams();
  setScanMsg();
})();
