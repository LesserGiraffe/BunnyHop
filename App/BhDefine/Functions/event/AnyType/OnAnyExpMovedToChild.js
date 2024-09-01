(function() {

  let cnctr = bhThis.findSymbolInAncestors('*', 1, false);
  let staticTypeNodeID = bhCommon.getStaticTypeNodeID(
    String(bhThis.getSymbolName()),
    String(cnctr.getClaz()));

  if (staticTypeNodeID !== null) {
    let posOnWS = bhMsgService.getPosOnWs(bhThis);
    let staticTypeNode = bhCommon.addNewNodeToWS(
      staticTypeNodeID,
      bhThis.getWorkspace(),
      posOnWS,
      bhNodeHandler,
      bhNodeTemplates,
      bhUserOpeCmd);

    // 子ノードの移動
    let paths = bhCommon.getPathOfAnyTypeChildToBeMoved(String(bhThis.getSymbolName()));
    if (paths !== null) {
      paths.forEach(
        function (path) {
          bhCommon.moveDescendant(bhThis, staticTypeNode, path, bhNodeHandler, bhUserOpeCmd);
        });
    }

    bhNodeHandler.exchangeNodes(bhThis, staticTypeNode, bhUserOpeCmd);
    bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
  }
})();
