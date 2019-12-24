(function() {

	let NodeMVCBuilder = net.seapanda.bunnyhop.modelprocessor.NodeMVCBuilder;
	let BhNodeID = net.seapanda.bunnyhop.model.node.BhNodeID;
	let BhNodeState = net.seapanda.bunnyhop.model.node.BhNode.State;
	let bhCommon = {};

	// 入れ替わってWSに移ったノードを末尾に再接続する
	function appendRemovedNode(newNode, oldNode, manuallyRemoved, bhNodeHandler, bhUserOpeCmd) {

		let outerEnd = newNode.findOuterNode(-1);
		if ((String(outerEnd.type) === "void") && outerEnd.canBeReplacedWith(oldNode) && !manuallyRemoved) {
			bhNodeHandler.replaceChild(outerEnd, oldNode, bhUserOpeCmd);
			bhNodeHandler.deleteNode(outerEnd, bhUserOpeCmd);
	    }
	}

	/**
	 * 引数で指定したノードを作成し, ワークスペースに追加する
	 * @param bhNodeID 作成するノードのID
	 * @param pos 追加時のワークスペース上の位置
	 * @param bhNodeHandler ノード操作用オブジェクト
	 * @param bhNodeTemplates ノードテンプレート管理オブジェクト
	 * @param bhUserOpeCmd undo/redo用コマンドオブジェクト
	 * @return 新規作成したノード
	 * */
	function addNewNodeToWS(bhNodeID, workspace, pos, bhNodeHandler, bhNodeTemplates, bhUserOpeCmd) {

		let newNode = bhNodeTemplates.genBhNode(BhNodeID.create(bhNodeID), bhUserOpeCmd);
		NodeMVCBuilder.build(newNode);
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

		if (candidates.contains(nodeToReconnect))
			return;
		
		let nodeToReplace = findNodeToBeReplaced(nodeToReconnect, node, candidates);
		
		// 接続先が無い場合は, ワークスペースへ
		if (nodeToReplace === null) {
			let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
			bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpeCmd);
		}
		else {
			let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
			bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, userOpeCmd);
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

		if (candidates.contains(parent))
			return findNodeToBeReplaced(nodeToReconnect, parent, candidates);

		return nodeToCheckReplaceability;
	}

	//  any-typeノードと入れ替えるべき子ノードのパスのマップ
	let anyTypeToPathOfChildToBeMoved = {
		'AnyArrayGetExp' : [['*', 'Arg1', '*']],
		'AnyArrayInsertStat' : [['*', 'Arg1', '*']],
		'AnyArrayRemoveStat' : [['*', 'Arg1', '*']],
		'AnyArraySetStat' : [['*', 'Arg1', '*']]
	}

	/**
	 * any-typeノードの入れ替えるべき子ノードのパスを取得する
	 * @param nodeName any-typeノード名
	 * @return 入れ替えるべき子ノードのパスのリスト. 見つからない場合null
	 */
	function getPathOfAnyTypeChildToBeMoved(nodeName) {
		let path = anyTypeToPathOfChildToBeMoved[nodeName];
		if (!path)
			return null;
		return path;
	}

	// any-typeノードと型決定シンボルからstatic-typeノードを特定するマップ
	let anyTypeToStaticTypeNode = {
		'AnyAssignStat' : {
			'NumberExpSctn' : 'idNumAssignStat',
			'StringExpSctn' : 'idStrAssignStat',
			'BooleanExpSctn': 'idBoolAssignStat',
			'ColorExpSctn'  : 'idColorAssignStat',
			'SoundExpSctn'  : 'idSoundAssignStat'
		},

		'AnyArrayPushStat' : {
			'NumberExpSctn' : 'idNumArrayPushStat',
			'StringExpSctn' : 'idStrArrayPushStat',
			'BooleanExpSctn': 'idBoolArrayPushStat',
			'ColorExpSctn'  : 'idColorArrayPushStat',
			'SoundExpSctn'  : 'idSoundArrayPushStat',
			'NumberListSctn' : 'idNumArrayPushStat',
			'StringListSctn' : 'idStrArrayPushStat',
			'BooleanListSctn': 'idBoolArrayPushStat',
			'ColorListSctn'  : 'idColorArrayPushStat',
			'SoundListSctn'  : 'idSoundArrayPushStat'
		},

		'AnyArrayGetExp' : {
			'NumberListSctn'  : 'idNumArrayGetExp',
			'StringListSctn'  : 'idStrArrayGetExp',
			'BooleanListSctn' : 'idBoolArrayGetExp',
			'ColorListSctn'   : 'idColorArrayGetExp',
			'SoundListSctn'   : 'idSoundArrayGetExp',
			'NumClass'   : 'idNumArrayGetExp',
			'StrClass'   : 'idStrArrayGetExp',
			'BoolClass'  : 'idBoolArrayGetExp',
			'ColorClass' : 'idColorArrayGetExp',
			'SoundClass' : 'idSoundArrayGetExp'
		},

		'AnyArrayGetLastExp' : {
			'NumberListSctn'  : 'idNumArrayGetLastExp',
			'StringListSctn'  : 'idStrArrayGetLastExp',
			'BooleanListSctn' : 'idBoolArrayGetLastExp',
			'ColorListSctn'   : 'idColorArrayGetLastExp',
			'SoundListSctn'   : 'idSoundArrayGetLastExp',
			'NumClass'   : 'idNumArrayGetLastExp',
			'StrClass'   : 'idStrArrayGetLastExp',
			'BoolClass'  : 'idBoolArrayGetLastExp',
			'ColorClass' : 'idColorArrayGetLastExp',
			'SoundClass' : 'idSoundArrayGetLastExp'
		},

		'AnyArrayPopStat' : {
			'NumberListSctn'  : 'idNumArrayPopStat',
			'StringListSctn'  : 'idStrArrayPopStat',
			'BooleanListSctn' : 'idBoolArrayPopStat',
			'ColorListSctn'   : 'idColorArrayPopStat',
			'SoundListSctn'   : 'idSoundArrayPopStat'
		},

		'AnyArrayInsertStat' : {
			'NumberListSctn'  : 'idNumArrayInsertStat',
			'StringListSctn'  : 'idStrArrayInsertStat',
			'BooleanListSctn' : 'idBoolArrayInsertStat',
			'ColorListSctn'   : 'idColorArrayInsertStat',
			'SoundListSctn'   : 'idSoundArrayInsertStat',
			'NumberExpSctn'  : 'idNumArrayInsertStat',
			'StringExpSctn'  : 'idStrArrayInsertStat',
			'BooleanExpSctn' : 'idBoolArrayInsertStat',
			'ColorExpSctn'   : 'idColorArrayInsertStat',
			'SoundExpSctn'   : 'idSoundArrayInsertStat'
		},

		'AnyArrayRemoveStat' : {
			'NumberListSctn'  : 'idNumArrayRemoveStat',
			'StringListSctn'  : 'idStrArrayRemoveStat',
			'BooleanListSctn' : 'idBoolArrayRemoveStat',
			'ColorListSctn'   : 'idColorArrayRemoveStat',
			'SoundListSctn'   : 'idSoundArrayRemoveStat'
		},

		'AnyArraySetStat' : {
			'NumberListSctn'  : 'idNumArraySetStat',
			'StringListSctn'  : 'idStrArraySetStat',
			'BooleanListSctn' : 'idBoolArraySetStat',
			'ColorListSctn'   : 'idColorArraySetStat',
			'SoundListSctn'   : 'idSoundArraySetStat',
			'NumberExpSctn'  : 'idNumArraySetStat',
			'StringExpSctn'  : 'idStrArraySetStat',
			'BooleanExpSctn' : 'idBoolArraySetStat',
			'ColorExpSctn'   : 'idColorArraySetStat',
			'SoundExpSctn'   : 'idSoundArraySetStat'
		},

		'AnyArrayAppendStat' : {
			'NumberListSctn'  : 'idNumArrayAppendStat',
			'StringListSctn'  : 'idStrArrayAppendStat',
			'BooleanListSctn' : 'idBoolArrayAppendStat',
			'ColorListSctn'   : 'idColorArrayAppendStat',
			'SoundListSctn'   : 'idSoundArrayAppendStat'
		},

		'AnyArrayClearStat' : {
			'NumberListSctn'  : 'idNumArrayClearStat',
			'StringListSctn'  : 'idStrArrayClearStat',
			'BooleanListSctn' : 'idBoolArrayClearStat',
			'ColorListSctn'   : 'idColorArrayClearStat',
			'SoundListSctn'   : 'idSoundArrayClearStat'
		}
	};

	/**
	 * any-Type のノード名と型を決定するシンボル名から対応する static-type Node のIDを取得する
	 */
	function getStaticTypeNodeID(anyTypeNodeName, typeDecisiveSymbolName) {

		let sectionNameToStaticType = anyTypeToStaticTypeNode[anyTypeNodeName];
		if (!sectionNameToStaticType)
			return null;

		let staticTypeNodeID = sectionNameToStaticType[typeDecisiveSymbolName];
		if (!staticTypeNodeID)
			return null;

		return staticTypeNodeID;
	}
	
	/**
	 * static-type の式である場合 true を返す.
	 */
	function isStaticTypeExp(node) {
	
		let section = node.findSymbolInDescendants('*');
		let sectionName = null;
		if (section !== null)
			sectionName = String(section.getSymbolName());
		
		return sectionName === 'NumberExpSctn'  ||
			   sectionName === 'StringExpSctn'  ||
			   sectionName === 'BooleanExpSctn' ||
			   sectionName === 'SoundExpSctn'   ||
			   sectionName === 'ColorExpSctn';
	}

	bhCommon['appendRemovedNode'] = appendRemovedNode;
	bhCommon['getStaticTypeNodeID'] = getStaticTypeNodeID;
	bhCommon['addNewNodeToWS'] = addNewNodeToWS;
	bhCommon['replaceDescendant'] = replaceDescendant;
	bhCommon['replaceStatWithNewStat'] = replaceStatWithNewStat;
	bhCommon['moveDescendant'] = moveDescendant;
	bhCommon['getPathOfAnyTypeChildToBeMoved'] = getPathOfAnyTypeChildToBeMoved;
	bhCommon['isStaticTypeExp'] = isStaticTypeExp;
	bhCommon['reconnectOuter'] = reconnectOuter;
	return bhCommon;
})();
