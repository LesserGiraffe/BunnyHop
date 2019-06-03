(function() {
	let _jBhNode = net.seapanda.bunnyhop.model.node.BhNode;
	let nextStrChainLinkExp = bhThis.findSymbolInDescendants('*', 'Arg1', '*');

	// 文字列挿入
	if (bhCommon.isStaticTypeExp(bhReplacedNewNode)) {

		// 新文字列リンクノード作成
		let posOnWS = bhMsgService.getPosOnWS(bhThis);
		let newStrChainLinkExp = bhCommon.addNewNodeToWS(
			bhThis.getID(),
			bhThis.getWorkspace(),
			posOnWS,
			bhNodeHandler,
			bhNodeTemplates,
			bhUserOpeCmd);
		bhNodeHandler.exchangeNodes(nextStrChainLinkExp, newStrChainLinkExp, bhUserOpeCmd);
		
		// 演算子の表示を変える
		//let opeLabel = newStrChainLinkExp.findSymbolInDescendants('*', '*', 'StrAppendOpe');
		//bhMsgService.setText(opeLabel, '＋');
		
		// 新ノードの末尾に旧ノードを追加
		let newStrChainLinkExpNext = newStrChainLinkExp.findSymbolInDescendants('*', 'Arg1', '*');
		bhNodeHandler.exchangeNodes(newStrChainLinkExpNext, nextStrChainLinkExp, bhUserOpeCmd);
		bhNodeHandler.deleteNode(newStrChainLinkExpNext, bhUserOpeCmd);
		
		// bhReplacedOldNode が AnyExpVoid の場合この時点で削除されている.
		if (!bhReplacedOldNode.isDeleted()) {
			// 旧文字列ノードを新文字列リンクノードに繋ぎ直し
			let anyExpVoid = newStrChainLinkExp.findSymbolInDescendants('*', 'Arg0', '*');
			bhNodeHandler.exchangeNodes(anyExpVoid, bhReplacedOldNode, bhUserOpeCmd);
			bhNodeHandler.deleteNode(anyExpVoid, bhUserOpeCmd);
		}
	}
	// 文字列取り外し
	else if (String(bhReplacedNewNode.getSymbolName()) === 'AnyExpVoid') {
		if (String(nextStrChainLinkExp.getSymbolName()) === 'StrChainLinkExp') {
			bhNodeHandler.replaceChild(bhThis, nextStrChainLinkExp, bhUserOpeCmd)
			bhNodeHandler.deleteNodeIncompletely(bhThis, true, bhUserOpeCmd);

			// 演算子の表示を変える
			//let strChainExp = nextStrChainLinkExp.findSymbolInAncestors('StrChainExp', 3, false);
			//if (strChainExp != null) {
			//	let opeLabel = nextStrChainLinkExp.findSymbolInDescendants('*', '*', 'StrAppendOpe');
			//	bhMsgService.setText(opeLabel, '⊕');
			//}
		}
	}
})();
