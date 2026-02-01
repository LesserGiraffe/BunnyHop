(function() {

  let BhNodeId = net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
  let DerivativeBuilder = net.seapanda.bunnyhop.node.model.derivative.DerivativeBuilder;
  let DerivationId = net.seapanda.bunnyhop.node.model.parameter.DerivationId;
  let MvcType = net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
  let bhUtility = {
    'bhNodeFactory': bhNodeFactory,
    'bhNodePlacer': bhNodePlacer
  };

  /**
   * nodeA の外部末尾ノードのシンボル名が symbolName であった場合, nodeA の外部末尾ノードと nodeB を入れ替える.
   * nodeA の外部末尾ノードは入れ替え後, 削除される.
   */
  function connectToOuterEnd(nodeA, nodeB, symbolName, bhUserOpe) {
    let outerEnd = nodeA.findOuterNode(-1);
    if (outerEnd !== null
        && outerEnd !== nodeA // return などの外部ノードを持たないノードは nodeA == outerEnd となる.
        && String(outerEnd.getSymbolName()) === symbolName) {
      bhNodePlacer.replaceChild(outerEnd, nodeB, bhUserOpe);
      bhNodePlacer.deleteNode(outerEnd, bhUserOpe);
    }
  }

  /**
   * 引数で指定したノードを作成し, ワークスペースに追加する
   * @param bhNodeId 作成するノードのID
   * @param workspace ノードを追加するワークスペース
   * @param pos 追加時のワークスペース上の位置
   * @param bhUserOpe undo / redo 用コマンドオブジェクト
   * @return 新規作成したノード
   */
  function addNewNodeToWS(bhNodeId, workspace, pos, bhUserOpe) {
    let newNode = createBhNode(bhNodeId, bhUserOpe)    
    bhNodeFactory.setMvc(newNode, MvcType.DEFAULT);
    bhNodePlacer.moveToWs(workspace, newNode, pos.x, pos.y, bhUserOpe);
    return newNode;
  }

  /**
   * 子孫ノードを入れ替える. 古いノードは削除される.
   * @param rootNode このノードの子孫ノードを入れ替える
   * @param descendantPath 入れ替えられる子孫ノードの rootNode からのパス
   * @param newNode 入れ替える新しいノード
   * @param bhUserOpe undo / redo 用コマンドオブジェクト
   */
  function replaceDescendant(rootNode, descendantPath, newNode, bhUserOpe) {
    let oldNode = rootNode.findDescendantOf(descendantPath);
    bhNodePlacer.replaceChild(oldNode, newNode, bhUserOpe);
    bhNodePlacer.deleteNode(oldNode, bhUserOpe);
  }

  /**
   * from から見て descendantPath の位置にあるノードを to から見て descendantPath の位置にあるノードに移す.
   * to から見て descendantPath の位置にあったノードは削除される.
   * @param to from の descendantPath の位置にあるノードをこのノードの descendantPath の位置に移す
   * @param from このノードの descendantPath の位置のノードを to の位置の descendantPath の位置に移す
   * @param descendantPath 移し元および移し先のノードのパス
   * @param bhUserOpe undo / redo 用コマンドオブジェクト
   */
  function moveDescendant(from, to, descendantPath, bhUserOpe) {
    let childToBeMoved = from.findDescendantOf(descendantPath);
    let oldNode = to.findDescendantOf(descendantPath);
    bhNodePlacer.replaceChild(oldNode, childToBeMoved, bhUserOpe);
    bhNodePlacer.deleteNode(oldNode, bhUserOpe);
  }
  
  /**
   * ancestorSearchStart とその先祖ノード全体を A とする.
   * B = { a ∈ A | a の親ノードが nodesToExclude  に含まれていない }
   * としたとき, B の中で親子関係が最も ancestorSearchStart に近いノードを b とする.
   *
   * node をワークスペースに移す.
   * b が存在してかつ, b と node が入れ替え可能な場合 b と node を入れ替える.
   * 入れ替え後, b のシンボル名が symbolsToExclude に含まれている場合, b を削除する.
   *
   * ただし, 以下の条件のいずれかを満たす場合この関数は何もしない.
   *   - node が null である
   *   - node が nodesToExclude に含まれている
   *   - node のシンボル名が symbolsToExclude に含まれている
   *   - node = b である
   *
   * @param node ワークスペースに移動後, 可能なら b と入れ替えるノード (nullable)
   * @param ancestorSearchStart このノードまたはその先祖ノードから b を探す  (nullable)
   * @param nodesToExclude 移動の対象にならないノードのリスト
   * @param symbolsToExclude node のシンボル名がこのリストに含まれている場合, この関数は何もしない.
   *                         b と node を入れ替えた後, b のシンボル名がこのリストに含まれている場合 b を削除する.
   * @param userOpe undo / redo 用コマンドオブジェクト
   */
  function moveNodeToAncestor(
      node, ancestorSearchStart, nodesToExclude, symbolsToExclude, userOpe) {
    // Java のリストに対して, Array.prototype.includes は定義されていないので, 代わりに indexOf を使う.
    // indexOf は Java のリストに対しても呼び出すことができる.
    if (node === null
        || nodesToExclude.indexOf(node) >= 0
        || symbolsToExclude.indexOf(String(node.getSymbolName())) >= 0) {
      return;
    }
    let toBeReplaced = findNodeWhoseParentIsNotInList(ancestorSearchStart, nodesToExclude);
    if (node === toBeReplaced) {
      return;
    }
    // node をワークスペースに移動
    let posOnWS = getPosOnWorkspace(node) ?? {x: 0, y: 0};
    bhNodePlacer.moveToWs(node.getWorkspace(), node, posOnWS.x, posOnWS.y, userOpe);
    // node と toBeReplaced が入れ替え可能かチェック
    if (toBeReplaced === null || !canConnect(toBeReplaced.getParentConnector(), node)) {
      return;
    }
    // node と toBeReplaced を入れ替え
    bhNodePlacer.exchangeNodes(node, toBeReplaced, userOpe);
    if (symbolsToExclude.indexOf(String(toBeReplaced.getSymbolName())) >= 0) {
      bhNodePlacer.deleteNode(toBeReplaced, userOpe)
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
  
  /** cnctr に node が接続可能か調べる. */
  function canConnect(cnctr, node) {
    if (cnctr === null || node === null) {
      return false;
    }
    return cnctr.canConnect(node);
  }

  /** node のワークスペース上の位置を返す. node がビューを持たない場合は null  */
  function getPosOnWorkspace(node) {
    return node.getView().map(view => view.getPositionManager().getPosOnWorkspace()).orElse(null);
  }

  /** プリミティブ型の式である場合 true を返す. */
  function isPrimitiveTypeExp(node) {
    let section = node.findDescendantOf('*');
    let sectionName = null;
    if (section !== null)
      sectionName = String(section.getSymbolName());

    return sectionName === 'NumExp'
        || sectionName === 'StrExp'
        || sectionName === 'BoolExp'
        || sectionName === 'SoundExp'
        || sectionName === 'ColorExp';
  }

  /** リスト型の式である場合 true を返す. */
  function isListTypeExp(node) {
    let section = node.findDescendantOf('*');
    let sectionName = null;
    if (section !== null)
      sectionName = String(section.getSymbolName());

    return sectionName === 'NumListExp'
        || sectionName === 'StrListExp'
        || sectionName === 'BoolListExp'
        || sectionName === 'SoundListExp'
        || sectionName === 'ColorListExp';
  }

  /**
   * 派生ノードを作成する
   * @param node このノードの派生ノードを作成する
   * @param derivationId この ID に対応する派生ノードを作成する ID (文字列)
   * @param userOpe undo / redo 用コマンドオブジェクト
   * @return node の派生ノード
   */
  function buildDerivative(node, derivationId, userOpe) {
    return DerivativeBuilder.build(node, DerivationId.of(derivationId), userOpe);
  }
  
  /**
   * BhNode を新規作成する
   * @param nodeId 作成するノードのID
   * @param bhUserOpe undo / redo 用コマンドオブジェクト
   */
  function createBhNode(nodeId, bhUserOpe) {
    return bhNodeFactory.create(BhNodeId.of(nodeId), bhUserOpe);
  }

  /**
   * defaultNodeId で指定したノードをデフォルトノードとして作成して, コネクタに接続する.
   *
   * <p>デフォルトノード指定されたノードをコネクタに接続すると, そのコネクタのデフォルトノードはそのノードの ID になる.
   * @param connector デフォルトノードを変更するコネクタ
   * @param defaultNodeId connector に設定するデフォルトノードの ID
   * @param bhUserOpe undo / redo 用コマンドオブジェクト
   */
  function changeDefaultNode(connector, defaultNodeId, bhUserOpe) {
    let defaultNode = bhNodeFactory.create(BhNodeId.of(defaultNodeId), bhUserOpe);
    defaultNode.setDefault(true);
    connector.getConnectedNode().replace(defaultNode, bhUserOpe);
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

  /** node とその先祖ノードを順にたどり, 親ノードが選択されていないノードを返す. */
  function findNodeWhoseParentIsNotSelected(node) {
    if (node === null) {
      return null;
    }
    let parent = node.findParentNode();
    if (parent === null) {
      return null;
    }
    if (parent.isSelected()) {
      return findNodeWhoseParentIsNotSelected(parent);
    }
    return node;
  }

  /** node から辿れるの外部ノードのうちシンボル名が symbolsToExclude に含まれていないものを全て選択する. */
  function selectToOutermost(node, symbolsToExclude, bhUserOpe) {
    let outerNode = node.findOuterNode(-1);
    while (outerNode !== node) {
      if (!outerNode.isSelected() 
          && symbolsToExclude.indexOf(String(outerNode.getSymbolName())) < 0) {
        outerNode.select(bhUserOpe);
      }
      outerNode = outerNode.findParentNode();
    }
  }

  /**
   * node から [オリジナル - 派生] の関係で推移可能なノードを全て取得する.
   */
  function collectDerivationFamily(node) {
    let original = findOriginalRoot(node);
    if (original === null) {
      return [];
    }
    let derivatives = [];
    collectDerivatives(original, derivatives);
    return derivatives;
  }

  /**
   * node から推移的に辿れるオリジナルノードのルート要素を探す.
   * node のオリジナルノードがない場合は node を返す.
   */
  function findOriginalRoot(node) {
    if (node === null) {
      return null;
    }
    let original = node.getOriginal();
    if (original === null) {
      return node;
    }
    return findOriginalRoot(original);
  }

  /** node から推移的に辿れる全ての派生ノードを dest に格納する. */
  function collectDerivatives(node, dest) {
    if (!isTemplateNode(node)) {
      dest.push(node);
    }
    for (let derivative of node.getDerivatives()) {
      collectDerivatives(derivative, dest);
    }
  }

  /** node がテンプレートノードかどうか調べる. */
  function isTemplateNode(node) {
    if (node === null) {
      return false;
    }
    // node.getView().map(...).orElse(false) は真偽値を返さないので使用しない
    let view = node.getView();
    return view.isPresent() && view.get().isTemplate();
  }

  bhUtility['connectToOuterEnd'] = connectToOuterEnd;
  bhUtility['addNewNodeToWS'] = addNewNodeToWS;
  bhUtility['replaceDescendant'] = replaceDescendant;
  bhUtility['moveDescendant'] = moveDescendant;
  bhUtility['isPrimitiveTypeExp'] = isPrimitiveTypeExp;
  bhUtility['isListTypeExp'] = isListTypeExp;
  bhUtility['moveNodeToAncestor'] = moveNodeToAncestor;
  bhUtility['buildDerivative'] = buildDerivative;
  bhUtility['createBhNode'] = createBhNode;
  bhUtility['changeDefaultNode'] = changeDefaultNode;
  bhUtility['findOuterNotSelected'] = findOuterNotSelected;
  bhUtility['findNodeWhoseParentIsNotSelected'] = findNodeWhoseParentIsNotSelected;
  bhUtility['selectToOutermost'] = selectToOutermost;
  bhUtility['getPosOnWorkspace'] = getPosOnWorkspace;
  bhUtility['collectDerivationFamily'] = collectDerivationFamily;
  bhUtility['isTemplateNode'] = isTemplateNode;
  return bhUtility;
})();
