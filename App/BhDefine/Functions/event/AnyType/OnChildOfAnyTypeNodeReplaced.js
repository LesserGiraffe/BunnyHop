(function() {
  
  let childNodeName = String(bhThis.getSymbolName());

  function createStaticTypeNodeFromAnyTypeNode() {

    let newNodeSection = bhReplacedNewNode.findSymbolInDescendants('*');
    let newNodeSectionName = '';
    if (newNodeSection !== null)
      newNodeSectionName = String(newNodeSection.getSymbolName());

    // 新ノード作成
    let staticTypeNodeID = bhCommon.getStaticTypeNodeID(childNodeName, newNodeSectionName);
    if (staticTypeNodeID === null)
      return null;

    let posOnWS = bhMsgService.getPosOnWs(bhThis);
    let staticTypeNode = bhCommon.addNewNodeToWS(
      staticTypeNodeID,
      bhThis.getWorkspace(),
      posOnWS,
      bhNodeHandler,
      bhNodeTemplates,
      bhUserOpeCmd);

    // 入れ替えられた子ノードのパス取得
    let replacedNodePath = bhThis.getRelativeSymbolNamePath(bhReplacedNewNode);
    let destPath = [];
    for (let i = 0; i < replacedNodePath.length; ++i) {
      if (i === replacedNodePath.length - 2)
        destPath.push(String(replacedNodePath[i]));
      else
        destPath.push('*');
    }
    // 入れ替えられた子ノードの移動
    bhCommon.replaceDescendant(staticTypeNode, destPath, bhReplacedNewNode, bhNodeHandler, bhUserOpeCmd);
    return staticTypeNode;
  }

  let staticTypeCnctrClassList = ['NumClass', 'StrClass', 'BoolClass', 'SoundClass', 'ColorClass'];
  let cnctrClass = String(bhParentConnector.getClaz());
  if (staticTypeCnctrClassList.indexOf(cnctrClass) !== -1)
    return;

  let staticTypeNode = createStaticTypeNodeFromAnyTypeNode();
  if (staticTypeNode === null)
    return;

  // 子ノードの移動
  let paths = bhCommon.getPathOfAnyTypeChildToBeMoved(childNodeName);
  if (paths !== null) {
    paths.forEach(
      function (path) {
        bhCommon.moveDescendant(bhThis, staticTypeNode, path, bhNodeHandler, bhUserOpeCmd);
      });
  }

  // any-type ノードの入れ替え
  if (String(bhThis.findSymbolInDescendants('*').getSymbolName()) === 'StatSctn')
    bhCommon.replaceStatWithNewStat(bhThis, staticTypeNode, bhNodeHandler, bhUserOpeCmd);
  else
    bhNodeHandler.exchangeNodes(bhThis, staticTypeNode, bhUserOpeCmd);

  bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
})();

