function genSemaphoreDeclTemplateNode(nodeId) {
  let semaphore = bhUtil.buildDerivative(bhThis, 'dervIdSemaphoreVar', bhUserOpe);
  let templateNode = bhUtil.createBhNode(nodeId, bhUserOpe);
  let timerVarArg = templateNode.findDescendantOf('*', 'Arg0', '*');
  timerVarArg.replace(semaphore, bhUserOpe);
  return templateNode;
}

(function() {
  return [
    genSemaphoreDeclTemplateNode('idSemaphoreAcquireStat'),
    genSemaphoreDeclTemplateNode('idSemaphoreTryAcquireExp'),
    genSemaphoreDeclTemplateNode('idSemaphoreReleaseStat'),
    genSemaphoreDeclTemplateNode('idGetNumSemaphorePermitsExp')];
})();
