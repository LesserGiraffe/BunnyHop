(function() {
  let nextStrChainLinkExp = bhThis.findSymbolInDescendants('*', 'Arg1', '*');

  // 文字列挿入
  if (bhCommon.isStaticTypeExp(bhReplacedNewNode)) {

    // 新文字列リンクノード作成
    let posOnWS = bhMsgService.getPosOnWs(bhThis);
    let newStrChainLinkExp = bhCommon.addNewNodeToWS(
      bhThis.getId(), bhThis.getWorkspace(), posOnWS, bhUserOpe);
    bhNodeHandler.exchangeNodes(nextStrChainLinkExp, newStrChainLinkExp, bhUserOpe);

    // 新ノードの末尾に旧ノードを追加
    let newStrChainLinkExpNext = newStrChainLinkExp.findSymbolInDescendants('*', 'Arg1', '*');
    bhNodeHandler.exchangeNodes(newStrChainLinkExpNext, nextStrChainLinkExp, bhUserOpe);
    bhNodeHandler.deleteNode(newStrChainLinkExpNext, bhUserOpe);
    
    // bhReplacedOldNode が AnyExpVoid の場合この時点で削除されている.
    if (!bhReplacedOldNode.isDeleted()) {
      // 旧文字列ノードを新文字列リンクノードに繋ぎ直し
      let anyExpVoid = newStrChainLinkExp.findSymbolInDescendants('*', 'Arg0', '*');
      bhNodeHandler.exchangeNodes(anyExpVoid, bhReplacedOldNode, bhUserOpe);
      bhNodeHandler.deleteNode(anyExpVoid, bhUserOpe);
    }
  }
  // 文字列取り外し
  else if (String(bhReplacedNewNode.getSymbolName()) === 'AnyExpVoid') {
    if (String(nextStrChainLinkExp.getSymbolName()) === 'StrChainLinkExp') {
      bhNodeHandler.replaceChild(bhThis, nextStrChainLinkExp, bhUserOpe);
      bhNodeHandler.deleteNode(bhThis, bhUserOpe);
    }
  }
})();
