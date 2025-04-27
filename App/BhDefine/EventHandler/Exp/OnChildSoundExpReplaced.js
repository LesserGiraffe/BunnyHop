(function() {
  let newNodeSection = bhParentConnector.getConnectedNode().findDescendantOf('*');
  let newNodeSectionName = null;
  if (newNodeSection !== null)
    newNodeSectionName = String(newNodeSection.getSymbolName());

  let nextMelodyExp = bhThis.findDescendantOf('*', 'Arg1', '*');

  // 音挿入
  if (String(newNodeSectionName) === 'SoundExpSctn') {
  
    // 新メロディノード作成
    let posOnWS = bhThis.getViewProxy().getPosOnWorkspace()
    let newMelodyExp = bhCommon.addNewNodeToWS(
      'idMelodyExp', bhThis.getWorkspace(), posOnWS, bhUserOpe);
    bhNodePlacer.exchangeNodes(nextMelodyExp, newMelodyExp, bhUserOpe);
    
    // 新ノードの末尾に旧ノードを追加
    let newMelodyExpNext = newMelodyExp.findDescendantOf('*', 'Arg1', '*');
    bhNodePlacer.exchangeNodes(newMelodyExpNext, nextMelodyExp, bhUserOpe);
    bhNodePlacer.deleteNode(newMelodyExpNext, bhUserOpe);
    
    // 旧音ノードを新メロディノードに繋ぎ直し
    let soundExpVoid = newMelodyExp.findDescendantOf('*', 'Arg0', '*');
    bhNodePlacer.exchangeNodes(soundExpVoid, bhReplacedOldNode, bhUserOpe);
    bhNodePlacer.deleteNode(soundExpVoid, bhUserOpe);
  }
  // 音削除
  else if (String(bhParentConnector.getConnectedNode().getSymbolName()) === 'SoundLiteralVoid') {
    if (String(nextMelodyExp.getSymbolName()) === 'MelodyExp') {
      bhNodePlacer.replaceChild(bhThis, nextMelodyExp, bhUserOpe);
      bhNodePlacer.deleteNode(bhThis, bhUserOpe);
    }
  }
})();
