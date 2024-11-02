(function() {

  let NodeMvcBuilder = net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
  let ImitationBuilder = net.seapanda.bunnyhop.modelprocessor.ImitationBuilder;
  let ImitationId = net.seapanda.bunnyhop.model.node.attribute.ImitationId;
  let bhCommon = {};

  // 入れ替わってWSに移ったノードを末尾に再接続する
  function appendRemovedNode(newNode, oldNode, isSpecifiedDirectly, bhNodeHandler, bhUserOpeCmd) {
    let outerEnd = newNode.findOuterNode(-1);
    if (outerEnd.canBeReplacedWith(oldNode)
        && !isSpecifiedDirectly
        && !newNode.equals(outerEnd)) { // return などの外部ノードを持たないノードは newNode == outerEnd となる.
      bhNodeHandler.replaceChild(outerEnd, oldNode, bhUserOpeCmd);
      bhNodeHandler.deleteNode(outerEnd, bhUserOpeCmd);
    }
  }

  /**
   * 引数で指定したノードを作成し, ワークスペースに追加する
   * @param bhNodeId 作成するノードのID
   * @param pos 追加時のワークスペース上の位置
   * @param bhNodeHandler ノード操作用オブジェクト
   * @param bhNodeTemplates ノードテンプレート管理オブジェクト
   * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
   * @return 新規作成したノード
   * */
  function addNewNodeToWS(bhNodeId, workspace, pos, bhNodeHandler, bhNodeTemplates, bhUserOpeCmd) {
    let newNode = genBhNode(bhNodeId, bhNodeTemplates, bhUserOpeCmd)    
    NodeMvcBuilder.build(newNode);
    bhNodeHandler.addRootNode(workspace, newNode, pos.x, pos.y, bhUserOpeCmd);
    return newNode;
  }

  /**
   * 子孫ノードを入れ替える. 古いノードは削除される.
   * @param rootNode このノードの子孫ノードを入れ替える
   * @param descendantPath 入れ替えられる子孫ノードの rootNode からのパス
   * @param newNode 入れ替える新しいノード
   * @param bhNodeHandler ノード操作用オブジェクト
   * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
   * */
  function replaceDescendant(rootNode, descendantPath, newNode, bhNodeHandler, bhUserOpeCmd) {
    let oldNode = rootNode.findSymbolInDescendants(descendantPath);
    bhNodeHandler.replaceChild(oldNode, newNode, bhUserOpeCmd);
    bhNodeHandler.deleteNode(oldNode, bhUserOpeCmd);
  }

  /**
   * from の descendantPath の位置にあるノードを to の descendantPath の位置にあるノードに移す.
   * to の descendantPath の位置にあったノードは削除される.
   * @param to from の descendantPathの位置にあるノードをこのノードの descendantPath の位置に移す
   * @param from このノードの path の位置のノードを to の位置の path の位置に移す
   * @param descendantPath 移し元および移し先のノードのパス
   * @param bhNodeHandler ノード操作用オブジェクト
   * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
   * */
  function moveDescendant(from, to, descendantPath, bhNodeHandler, bhUserOpeCmd) {
    let childToBeMoved = from.findSymbolInDescendants(descendantPath);
    let oldNode = to.findSymbolInDescendants(descendantPath);
    bhNodeHandler.replaceChild(oldNode, childToBeMoved, bhUserOpeCmd);
    bhNodeHandler.deleteNode(oldNode, bhUserOpeCmd);
  }

  /**
   * ステートノードを入れ替える.
   * @param oldStat 入れ替えられる古いステートノード
   * @param newStat 入れ替える新しいステートノード
   * @param bhNodeHandler ノード操作用オブジェクト
   * @param userOpeCmd undo/redo用コマンドオブジェクト
   * */
  function replaceStatWithNewStat(oldStat, newStat, bhNodeHandler, bhUserOpeCmd) {
    let nextStatOfOldStat = oldStat.findSymbolInDescendants('*', 'NextStat', '*');
    let nextStatOfNewStat = newStat.findSymbolInDescendants('*', 'NextStat', '*');
    bhNodeHandler.exchangeNodes(nextStatOfOldStat, nextStatOfNewStat, bhUserOpeCmd);
    bhNodeHandler.exchangeNodes(oldStat, newStat, bhUserOpeCmd);
  }
  
  /**
   * node の外部ノードが cut か delete の対象でない場合, 適切な位置に再接続する
   * @param node このノードの外部ノードが cut か delete の対象でない場合, それを繋ぎ換える
   * @param cadidates cut か delete の対象ノードのリスト
   * @param userOpeCmd undo/redo用コマンドオブジェクト
   */
  function reconnectOuter(node, candidates, bhMsgService, bhNodeHandler, userOpeCmd) {
    // 移動させる外部ノードを探す
    let nodeToReconnect = node.findOuterNode(1);
    if (nodeToReconnect === null)
      return;
    
    // 繋ぎ換える必要がないノード
    let outersNotToReconnect = ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'];
    let isOuterNotToReconnect = outersNotToReconnect.some(nodeName => nodeName === String(nodeToReconnect.getSymbolName()));
    if (isOuterNotToReconnect)
      return;

    candidates = toJsArray(candidates);
    if (candidates.includes(nodeToReconnect))
      return;
    
    let nodeToReplace = findNodeToBeReplaced(nodeToReconnect, node, candidates);
    
    // 接続先が無い場合は, ワークスペースへ
    if (nodeToReplace === null) {
      let posOnWS = bhMsgService.getPosOnWs(nodeToReconnect);
      bhNodeHandler.moveToWs(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpeCmd);
    }
    else {
      let posOnWS = bhMsgService.getPosOnWs(nodeToReconnect);
      bhNodeHandler.moveToWs(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpeCmd);
      bhNodeHandler.exchangeNodes(nodeToReconnect, nodeToReplace, userOpeCmd);
    }
    return;
  }

  /**
   * 外部ノードの接続先ノードを探す
   */
  function findNodeToBeReplaced(nodeToReconnect, nodeToCheckReplaceability, candidates) {
    let parent = nodeToCheckReplaceability.findParentNode();
    if (parent === null)
      return null;

    if (candidates.includes(parent))
      return findNodeToBeReplaced(nodeToReconnect, parent, candidates);

    return nodeToCheckReplaceability;
  }
  
  /**
   * static-type の式である場合 true を返す.
   */
  function isStaticTypeExp(node) {
    let section = node.findSymbolInDescendants('*');
    let sectionName = null;
    if (section !== null)
      sectionName = String(section.getSymbolName());
    
    return sectionName === 'NumExpSctn' ||
        sectionName === 'StrExpSctn'    ||
        sectionName === 'BoolExpSctn'   ||
        sectionName === 'SoundExpSctn'  ||
        sectionName === 'ColorExpSctn';
  }
  
  /**
   * イミテーションノードを作成する
   * @param node イミテーションノードを作成するオリジナルノード
   * @param imitID 作成するイミテーションノードの ID (文字列)
   * @param userOpeCmd undo/redo用コマンドオブジェクト
   * @return node のイミテーションノード
   */
  function buildImitation(node, imitID, userOpeCmd) {
    return ImitationBuilder.build(node, ImitationId.of(imitID), false, userOpeCmd);
  }
  
  /**
   * BhNode を新規作成する
   * @param nodeID 作成するノードのID
   * @param bhNodeTemplates ノードテンプレート管理オブジェクト
   * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
   */
  function genBhNode(bhNodeId, bhNodeTemplates, bhUserOpeCmd) {
    return bhNodeTemplates.genBhNode(BhNodeId.of(bhNodeId), bhUserOpeCmd);
  }

  /**
   * コネクタのデフォルトノードを変更して, 接続されているノードを変更後のデフォルトノードにする.
   * @prarm connector デフォルトノードを変更するコネクタ
   * @param defulatNodeID connector に設定するデフォルトノードの ID
   * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
   */
  function changeDefaultNode(connector, defaultNodeId, bhUserOpeCmd) {
    connector.setDefaultNodeId(BhNodeId.of(defaultNodeId));
    connector.remove(bhUserOpeCmd);
  }

  /** Java の List を JavaScript の配列に変換する. */
  function toJsArray(javaArray) {
    let jsArray = [];
    for (let elem of javaArray) {
      jsArray.push(elem);
    }
    return jsArray;
  }

  bhCommon['appendRemovedNode'] = appendRemovedNode;
  bhCommon['addNewNodeToWS'] = addNewNodeToWS;
  bhCommon['replaceDescendant'] = replaceDescendant;
  bhCommon['replaceStatWithNewStat'] = replaceStatWithNewStat;
  bhCommon['moveDescendant'] = moveDescendant;
  bhCommon['isStaticTypeExp'] = isStaticTypeExp;
  bhCommon['reconnectOuter'] = reconnectOuter;
  bhCommon['buildImitation'] = buildImitation;
  bhCommon['genBhNode'] = genBhNode;
  bhCommon['changeDefaultNode'] = changeDefaultNode;
  bhCommon['toJsArray'] = toJsArray;  
  return bhCommon;
})();
