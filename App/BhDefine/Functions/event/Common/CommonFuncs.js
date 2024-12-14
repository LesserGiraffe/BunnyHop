(function() {

  let NodeMvcBuilder = net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
  let BhNodeId = net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
  let DerivativeBuilder = net.seapanda.bunnyhop.model.traverse.DerivativeBuilder;
  let DerivationId = net.seapanda.bunnyhop.model.node.attribute.DerivationId;
  let bhCommon = {
    'bhNodeFactory': bhNodeFactory,
    'bhNodePlacer': bhNodePlacer
  };

  // 入れ替わってワークスペースに移ったノードを末尾に再接続する
  function appendRemovedNode(newNode, oldNode, isSpecifiedDirectly, bhUserOpe) {
    let outerEnd = newNode.findOuterNode(-1);
    if (outerEnd.canBeReplacedWith(oldNode)
        && !isSpecifiedDirectly
        && !newNode.equals(outerEnd)) { // return などの外部ノードを持たないノードは newNode == outerEnd となる.
      bhNodePlacer.replaceChild(outerEnd, oldNode, bhUserOpe);
      bhNodePlacer.deleteNode(outerEnd, bhUserOpe);
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
    bhNodePlacer.moveToWs(workspace, newNode, pos.x, pos.y, bhUserOpe);
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
    bhNodePlacer.replaceChild(oldNode, newNode, bhUserOpe);
    bhNodePlacer.deleteNode(oldNode, bhUserOpe);
  }

  /**
   * from から見て descendantPath の位置にあるノードを to から見て descendantPath の位置にあるノードに移す.
   * to から見て descendantPath の位置にあったノードは削除される.
   * @param to from の descendantPath の位置にあるノードをこのノードの descendantPath の位置に移す
   * @param from このノードの descendantPath の位置のノードを to の位置の descendantPath の位置に移す
   * @param descendantPath 移し元および移し先のノードのパス
   * @param bhUserOpe undo/redo用コマンドオブジェクト
   */
  function moveDescendant(from, to, descendantPath, bhUserOpe) {
    let childToBeMoved = from.findSymbolInDescendants(descendantPath);
    let oldNode = to.findSymbolInDescendants(descendantPath);
    bhNodePlacer.replaceChild(oldNode, childToBeMoved, bhUserOpe);
    bhNodePlacer.deleteNode(oldNode, bhUserOpe);
  }
  
  /**
   * startNode とその先祖ノード全体を A とする.
   * B = { a ∈ A | a の親ノードが ignoredList の含まれていない }
   * としたとき, B の中で親子関係が最も startNode に近いノードを b とする.
   * 
   * ignoredList の中に node が含まれていない場合 b と入れ替える.
   * startNode が null の場合や b が存在しない場合は node をワークスペースに移動する.
   * 
   * node が 'VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat' のいずれかであった場合は何もしない.
   * node === b であった場合も何もしない.
   * 
   * b が 'VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat' のいずれかであった場合削除される.
   */
  function reconnect(node, startNode, ignoredList, bhCmdProxy, userOpe) {
    if (node === null) {
      return;
    }
    // 繋ぎ換える必要がないノード
    let notToReconnect = ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'];
    // Java のリストに対して, Array.prototype.includes は定義されていないので, 代わりに indexOf を使う.
    // indexOf は Java のリストに対しても呼び出すことができる.
    if (notToReconnect.indexOf(String(node.getSymbolName())) >= 0
        || ignoredList.indexOf(node) >= 0) {
      return;
    }

    let toBeReaplced = findNodeWhoseParentIsNotInList(startNode, ignoredList);    
    if (node === toBeReaplced) {
      return;
    } else if (toBeReaplced === null) {
      // 接続先が無い場合は, ワークスペースへ
      let posOnWS = bhCmdProxy.getPosOnWs(node);
      bhNodePlacer.moveToWs(node.getWorkspace(), node, posOnWS.x, posOnWS.y, userOpe);
    } else {
      let posOnWS = bhCmdProxy.getPosOnWs(node);
      bhNodePlacer.moveToWs(node.getWorkspace(), node, posOnWS.x, posOnWS.y, userOpe);
      bhNodePlacer.exchangeNodes(node, toBeReaplced, userOpe);
      if (notToReconnect.indexOf(String(toBeReaplced.getSymbolName())) >= 0) {
        bhNodePlacer.deleteNode(toBeReaplced, userOpe)
      }
    }
  }

  function findNodeWhoseParentIsNotInList(node, list) {
    if (node === null) {
      return null;
    }
    let parent = node.findParentNode();
    if (parent === null) {
      return null;
    }
    if (list.indexOf(parent) >= 0) {
      return findNodeWhoseParentIsNotInList(parent, list);
    }
    return node;
  }
  
  /**
   * static-type の式である場合 true を返す.
   */
  function isPrimitiveTypeExp(node) {
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

  /**
   * node から外部子ノードをたどり, 最初に見つかった非選択状態のノードを返す.
   * 見つからなかった場合は null を返す.
   */
  function findOuterNotSelected(node) {
    let outerNode = node.findOuterNode(1);
    if (outerNode === null) {
      return null;
    }
    if (!outerNode.isSelected()) {
      return outerNode;
    }
    return findOuterNotSelected(outerNode);
  }

  bhCommon['appendRemovedNode'] = appendRemovedNode;
  bhCommon['addNewNodeToWS'] = addNewNodeToWS;
  bhCommon['replaceDescendant'] = replaceDescendant;
  bhCommon['moveDescendant'] = moveDescendant;
  bhCommon['isPrimitiveTypeExp'] = isPrimitiveTypeExp;
  bhCommon['reconnect'] = reconnect;
  bhCommon['buildDerivative'] = buildDerivative;
  bhCommon['createBhNode'] = createBhNode;
  bhCommon['changeDefaultNode'] = changeDefaultNode;
  bhCommon['findOuterNotSelected'] = findOuterNotSelected;
  return bhCommon;
})();
