function genSyncTimerDeclTemplateNode(nodeId) {
  let timer = bhUtil.buildDerivative(bhThis, 'dervIdSyncTimerVar', bhUserOpe);
  let templateNode = bhUtil.createBhNode(nodeId, bhUserOpe);
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
