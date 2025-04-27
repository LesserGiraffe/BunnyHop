function genSyncTimerDeclTemplateNode(nodeID) {
  let timer = bhCommon.buildDerivative(bhThis, 'dervIdSyncTimeVar', bhUserOpe);
  let templateNode = bhCommon.createBhNode(nodeID, bhUserOpe);
  let timerVarArg = templateNode.findDescendantOf('*', 'Arg0', '*');
  timerVarArg.replace(timer, bhUserOpe);
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
