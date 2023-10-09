let imitIdManual = 'imitIdManual';

function genSyncTimerDeclTemplateNode(nodeID) {

  let timer = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
  let templateNode = bhCommon.genBhNode(nodeID, bhNodeTemplates, bhUserOpeCmd);
  let timerVarArg = templateNode.findSymbolInDescendants('*', 'Arg0', '*');
  timerVarArg.replace(timer, bhUserOpeCmd);
  return templateNode;
}

(function() {

  return [
    genSyncTimerDeclTemplateNode('idSyncTimerCountdownStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerAwaitStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerAwaitWithTimeoutStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerCountdownAndAwaitStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerCountdownAndAwaitWithTimeoutStat'),
    genSyncTimerDeclTemplateNode('idResetSyncTimerStat'),
    genSyncTimerDeclTemplateNode('idGetSyncTimerCountExp')];
})();
