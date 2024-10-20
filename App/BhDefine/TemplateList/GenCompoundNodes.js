(function() {

  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;

  function registerNodeTemplate(bhNodeId, bhNode) {
    bhNodeTemplates.registerNodeTemplate(BhNodeId.create(bhNodeId), bhNode);
  }

  function genBhNode(bhNodeId, bhUserOpeCmd) {
    return bhNodeTemplates.genBhNode(BhNodeId.create(bhNodeId), bhUserOpeCmd);
  }

  function connect(parentNode, childNode, cnctrPath) {
    let cnctr = parentNode.findSymbolInDescendants(cnctrPath);
    cnctr.connectNode(childNode, bhUserOpeCmd);
  }

  //移動ノードの初期速度を変更して再登録
  function setInitialMoveSpeed() {
    let newMoveStat = genBhNode('idMoveStat', bhUserOpeCmd);
    let moveSpeed = newMoveStat.findSymbolInDescendants('*', 'Arg0', '*', '*', 'Literal', '*');
    let moveTime = newMoveStat.findSymbolInDescendants('*', 'Arg1', '*', '*', 'Literal', '*');
    moveSpeed.setText('2');
    moveTime.setText('1');
    registerNodeTemplate('idMoveStat', newMoveStat);
  }

  //音ノードの初期値を変更して再登録
  function setInitialSoundParams() {

    let newFreqSoundLiteral = genBhNode('idFreqSoundLiteral', bhUserOpeCmd);
    let duration = newFreqSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    let freq = newFreqSoundLiteral.findSymbolInDescendants('*', 'Frequency', '*', '*', 'Literal', '*');
    duration.setText('1');
    freq.setText('500');
    registerNodeTemplate('idFreqSoundLiteral', newFreqSoundLiteral);

    let newScaleSoundLiteral = genBhNode('idScaleSoundLiteral', bhUserOpeCmd);
    duration = newScaleSoundLiteral.findSymbolInDescendants('*', 'Duration', '*', '*', 'Literal', '*');
    duration.setText('1');
    registerNodeTemplate('idScaleSoundLiteral', newScaleSoundLiteral);
  }

  //リストと変数名を変更して再登録
  function setVarAndListName() {

    let renameInfoList = [
      {node: 'idNumVarDecl',          cnctrName: 'VarName',             varName: '数値変数'},
      {node: 'idStrVarDecl',          cnctrName: 'VarName',             varName: '文字列変数'},
      {node: 'idBoolVarDecl',         cnctrName: 'VarName',             varName: '論理変数'},
      {node: 'idColorVarDecl',        cnctrName: 'VarName',             varName: '色変数'},
      {node: 'idSoundVarDecl',        cnctrName: 'VarName',             varName: '音変数'},
      {node: 'idNumListDecl',         cnctrName: 'ListName',            varName: '数値リスト'},
      {node: 'idStrListDecl',         cnctrName: 'ListName',            varName: '文字列リスト'},
      {node: 'idBoolListDecl',        cnctrName: 'ListName',            varName: '論理リスト'},
      {node: 'idColorListDecl',       cnctrName: 'ListName',            varName: '色リスト'},
      {node: 'idSoundListDecl',       cnctrName: 'ListName',            varName: '音リスト'},
      {node: 'idCriticalSectionDecl', cnctrName: 'CriticalSectionName', varName: '区間名'},
      {node: 'idSyncTimerDecl',       cnctrName: 'SyncTimerName',       varName: 'タイマー名'}
    ];

    for (let i = 0; i < renameInfoList.length; ++i) {
      let renameInfo = renameInfoList[i];
      let varDecl = genBhNode(renameInfo.node, bhUserOpeCmd);
      let varName = varDecl.findSymbolInDescendants('*', renameInfo.cnctrName, '*');
      varName.setText(renameInfo.varName);
      registerNodeTemplate(renameInfo.node, varDecl);
    }
  }

  setInitialMoveSpeed();
  setVarAndListName();
  setInitialSoundParams();
})();


