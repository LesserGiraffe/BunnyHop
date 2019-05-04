(function() {

	let UnscopedNodeCollector = net.seapanda.bunnyhop.modelprocessor.UnscopedNodeCollector;

	// 移動させる外部ノードを探す
	let nodeToReconnect = bhThis.findOuterNode(1);
	if (nodeToReconnect === null)
		return;

	let outersNotToReconnect = ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'];
	let isOuterNotToReconnect = outersNotToReconnect.some(nodeName => nodeName === String(nodeToReconnect.getSymbolName()));
	if (isOuterNotToReconnect)
		return;

	java.lang.System.out.println(bhThis.getSymbolName());


	let posOnWS = bhMsgService.getPosOnWS(nodeToReconnect);
	bhNodeHandler.moveToWS(nodeToReconnect.getWorkspace(), nodeToReconnect, posOnWS.x, posOnWS.y, bhUserOpeCmd);
	bhNodeHandler.exchangeNodes(nodeToReconnect, bhThis, bhUserOpeCmd);
	let unscopedNodes = UnscopedNodeCollector.collect(nodeToReconnect);
	bhNodeHandler.deleteNodes(unscopedNodes, bhUserOpeCmd);
})();
