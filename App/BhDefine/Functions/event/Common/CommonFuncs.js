(function() {

  let NodeMvcBuilder = net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
  let DerivativeBuilder = net.seapanda.bunnyhop.modelprocessor.DerivativeBuilder;
  let DerivationId = net.seapanda.bunnyhop.model.node.attribute.DerivationId;
  let bhCommon = {
    'bhNodeFactory': bhNodeFactory,
    'bhNodeHandler': bhNodeHandler
  };

  // 入れ替わってワークスペースに移ったノードを末尾に再接続する
  function appendRemovedNode(newNode, oldNode, isSpecifiedDirectly, bhUserOpe) {
    let outerEnd = newNode.findOuterNode(-1);
    if (outerEnd.canBeReplacedWith(oldNode)
        && !isSpecifiedDirectly
        && !newNode.equals(outerEnd)) { // return などの外部ノードを持たないノードは newNode == outerEnd となる.
      bhNodeHandler.replaceChild(outerEnd, oldNode, bhUserOpe);
      bhNodeHandler.deleteNode(outerEnd, bhUserOpe);
    }
  }

  /**
   * 引数で指定したノードを作成し, ワークスペースに追加する
   * @param bhNodeId 作成するノードのID
   * @param pos 追加時のワークスペース上の位置
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   * @return 新規作成したノード
   */
  function addNewNodeToWS(bhNodeId, workspace, pos, bhUserOpe) {
    let newNode = createBhNode(bhNodeId, bhUserOpe)    
    NodeMvcBuilder.build(newNode);
    bhNodeHandler.moveToWs(workspace, newNode, pos.x, pos.y, bhUserOpe);
    return newNode;
  }

  /**
   * 子孫ノードを入れ替える. 古いノードは削除される.
   * @param rootNode このノードの子孫ノードを入れ替える
   * @param descendantPath 入れ替えられる子孫ノードの rootNode からのパス
   * @param newNode 入れ替える新しいノード
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   */
  function replaceDescendant(rootNode, descendantPath, newNode, bhUserOpe) {
    let oldNode = rootNode.findSymbolInDescendants(descendantPath);
    bhNodeHandler.replaceChild(oldNode, newNode, bhUserOpe);
    bhNodeHandler.deleteNode(oldNode, bhUserOpe);
  }

  /**
   * from の descendantPath の位置にあるノードを to の descendantPath の位置にあるノードに移す.
   * to の descendantPath の位置にあったノードは削除される.
   * @param to from の descendantPathの位置にあるノードをこのノードの descendantPath の位置に移す
   * @param from このノードの path の位置のノードを to の位置の path の位置に移す
   * @param descendantPath 移し元および移し先のノードのパス
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   */
  function moveDescendant(from, to, descendantPath, bhUserOpe) {
    let childToBeMoved = from.findSymbolInDescendants(descendantPath);
    let oldNode = to.findSymbolInDescendants(descendantPath);
    bhNodeHandler.replaceChild(oldNode, childToBeMoved, bhUserOpe);
    bhNodeHandler.deleteNode(oldNode, bhUserOpe);
  }

  /**
   * ステートノードを入れ替える.
   * @param oldStat 入れ替えられる古いステートノード
   * @param newStat 入れ替える新しいステートノード
   * @param userOpe undo/redo用コマンドオブジェクト
   */
  function replaceStatWithNewStat(oldStat, newStat, bhUserOpe) {
    let nextStatOfOldStat = oldStat.findSymbolInDescendants('*', 'NextStat', '*');
    let nextStatOfNewStat = newStat.findSymbolInDescendants('*', 'NextStat', '*');
    bhNodeHandler.exchangeNodes(nextStatOfOldStat, nextStatOfNewStat, bhUserOpe);
    bhNodeHandler.exchangeNodes(oldStat, newStat, bhUserOpe);
  }
  
  /**
   * node の外部ノードが cut か delete の対象でない場合, 適切な位置に再接続する
   * @param node このノードの外部ノードが cut か delete の対象でない場合, それを繋ぎ換える
   * @param cadidates cut か delete の対象ノードのリスト
   * @param userOpe undo/redo用コマンドオブジェクト
   */
  function reconnectOuter(node, candidates, bhMsgService, userOpe) {
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
      bhNodeHandler.moveToWs(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpe);
    }
    else {
      let posOnWS = bhMsgService.getPosOnWs(nodeToReconnect);
      bhNodeHandler.moveToWs(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpe);
      bhNodeHandler.exchangeNodes(nodeToReconnect, nodeToReplace, userOpe);
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
   * 派生ノードを作成する
   * @param node このノードの派生ノードを作成する
   * @param derivationId この ID に対応する派生ノードを作成する ID (文字列)
   * @param userOpe undo/redo用コマンドオブジェクト
   * @return node の派生ノード
   */
  function buildDerivative(node, derivationId, userOpe) {
    return DerivativeBuilder.build(node, DerivationId.of(derivationId), userOpe);
  }
  
  /**
   * BhNode を新規作成する
   * @param nodeID 作成するノードのID
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   */
  function createBhNode(bhNodeId, bhUserOpe) {
    return bhNodeFactory.create(BhNodeId.of(bhNodeId), bhUserOpe);
  }

  /**
   * コネクタのデフォルトノードを変更して, 接続されているノードを変更後のデフォルトノードにする.
   * @prarm connector デフォルトノードを変更するコネクタ
   * @param defulatNodeID connector に設定するデフォルトノードの ID
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   */
  function changeDefaultNode(connector, defaultNodeId, bhUserOpe) {
    connector.setDefaultNodeId(BhNodeId.of(defaultNodeId));
    connector.getConnectedNode().remove(bhUserOpe);
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
  bhCommon['buildDerivative'] = buildDerivative;
  bhCommon['createBhNode'] = createBhNode;
  bhCommon['changeDefaultNode'] = changeDefaultNode;
  bhCommon['toJsArray'] = toJsArray;  
  return bhCommon;
})();
