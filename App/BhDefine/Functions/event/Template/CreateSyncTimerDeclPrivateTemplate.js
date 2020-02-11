let imitIdManual = 'imitIdManual';

function genReusableBarrierDeclTemplateNode(nodeID) {

	let timer = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
	let templateNode = bhCommon.genBhNode(nodeID, bhNodeTemplates, bhUserOpeCmd);
	let timerVarArg = templateNode.findSymbolInDescendants('*', 'Arg0', '*');
	timerVarArg.replace(timer, bhUserOpeCmd);
	return templateNode;
}

(function() {

	let imit = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
	return [
		imit,
		genReusableBarrierDeclTemplateNode('idSyncTimerCountdownStat'),
		genReusableBarrierDeclTemplateNode('idSyncTimerAwaitStat'),
		genReusableBarrierDeclTemplateNode('idSyncTimerAwaitWithTimeoutStat'),
		genReusableBarrierDeclTemplateNode('idSyncTimerCountdownAndAwaitStat'),
		genReusableBarrierDeclTemplateNode('idSyncTimerCountdownAndAwaitWithTimeoutStat'),
		genReusableBarrierDeclTemplateNode('idResetSyncTimerStat'),
		genReusableBarrierDeclTemplateNode('idGetSyncTimerCountExp')];
})();
