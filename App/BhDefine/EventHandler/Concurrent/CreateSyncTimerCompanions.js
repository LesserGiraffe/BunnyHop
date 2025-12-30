function genSyncTimerDeclTemplateNode(nodeId) {
  let timer = bhCommon.buildDerivative(bhThis, 'dervIdSyncTimerVar', bhUserOpe);
  let templateNode = bhCommon.createBhNode(nodeId, bhUserOpe);
  let timerVarArg = templateNode.findDescendantOf('*', 'Arg0', '*');
  timerVarArg.replace(timer, bhUserOpe);
  return templateNode;
}

(function() {
  return [
    genSyncTimerDeclTemplateNode('idSyncTimerCountdownStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerAwaitStat'),
    genSyncTimerDeclTemplateNode('idSyncTimerTimedAwaitExp'),
    genSyncTimerDeclTemplateNode('idResetSyncTimerStat'),
    genSyncTimerDeclTemplateNode('idGetSyncTimerCountExp')];
})();
