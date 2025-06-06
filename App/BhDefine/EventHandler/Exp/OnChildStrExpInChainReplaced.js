(function() {
  let nextStrChainLinkExp = bhThis.findDescendantOf('*', 'Arg1', '*');

  // 文字列挿入
  if (bhCommon.isPrimitiveTypeExp(bhReplacedNewNode)) {

    // 新文字列リンクノード作成
    let posOnWS = bhCommon.getPosOnWorkspace(bhThis) ?? {x: 0, y: 0};
    let newStrChainLinkExp = bhCommon.addNewNodeToWS(
      bhThis.getId(), bhThis.getWorkspace(), posOnWS, bhUserOpe);
    bhNodePlacer.exchangeNodes(nextStrChainLinkExp, newStrChainLinkExp, bhUserOpe);

    // 新ノードの末尾に旧ノードを追加
    let newStrChainLinkExpNext = newStrChainLinkExp.findDescendantOf('*', 'Arg1', '*');
    bhNodePlacer.exchangeNodes(newStrChainLinkExpNext, nextStrChainLinkExp, bhUserOpe);
    bhNodePlacer.deleteNode(newStrChainLinkExpNext, bhUserOpe);
    
    // bhReplacedOldNode が AnyExpVoid の場合この時点で削除されている.
    if (!bhReplacedOldNode.isDeleted()) {
      // 旧文字列ノードを新文字列リンクノードに繋ぎ直し
      let anyExpVoid = newStrChainLinkExp.findDescendantOf('*', 'Arg0', '*');
      bhNodePlacer.exchangeNodes(anyExpVoid, bhReplacedOldNode, bhUserOpe);
      bhNodePlacer.deleteNode(anyExpVoid, bhUserOpe);
    }
  }
  // 文字列取り外し
  else if (String(bhReplacedNewNode.getSymbolName()) === 'AnyExpVoid') {
    if (String(nextStrChainLinkExp.getSymbolName()) === 'StrChainLinkExp') {
      bhNodePlacer.moveToWs(bhThis.getWorkspace(), nextStrChainLinkExp, 0, 0, bhUserOpe);
      bhNodePlacer.replaceChild(bhThis, nextStrChainLinkExp, bhUserOpe);
      bhNodePlacer.deleteNode(bhThis, bhUserOpe);
    }
  }
})();
