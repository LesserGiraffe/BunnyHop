(function() {
  let newNodeSection = bhParentConnector.getConnectedNode().findSymbolInDescendants('*');
  let newNodeSectionName = null;
  if (newNodeSection !== null)
    newNodeSectionName = String(newNodeSection.getSymbolName());

  let nextMelodyExp = bhThis.findSymbolInDescendants('*', 'Arg1', '*');

  // 音挿入
  if (String(newNodeSectionName) === 'SoundExpSctn') {
  
    // 新メロディノード作成
    let posOnWS = bhMsgService.getPosOnWs(bhThis);
    let newMelodyExp = bhCommon.addNewNodeToWS(
      'idMelodyExp', bhThis.getWorkspace(), posOnWS, bhUserOpe);
    bhNodeHandler.exchangeNodes(nextMelodyExp, newMelodyExp, bhUserOpe);
    
    // 新ノードの末尾に旧ノードを追加
    let newMelodyExpNext = newMelodyExp.findSymbolInDescendants('*', 'Arg1', '*');
    bhNodeHandler.exchangeNodes(newMelodyExpNext, nextMelodyExp, bhUserOpe);
    bhNodeHandler.deleteNode(newMelodyExpNext, bhUserOpe);
    
    // 旧音ノードを新メロディノードに繋ぎ直し
    let soundExpVoid = newMelodyExp.findSymbolInDescendants('*', 'Arg0', '*');
    bhNodeHandler.exchangeNodes(soundExpVoid, bhReplacedOldNode, bhUserOpe);
    bhNodeHandler.deleteNode(soundExpVoid, bhUserOpe);
  }
  // 音削除
  else if (String(bhParentConnector.getConnectedNode().getSymbolName()) === 'SoundLiteralVoid') {
    if (String(nextMelodyExp.getSymbolName()) === 'MelodyExp') {
      bhNodeHandler.replaceChild(bhThis, nextMelodyExp, bhUserOpe);
      bhNodeHandler.deleteNode(bhThis, bhUserOpe);
    }
  }
})();
