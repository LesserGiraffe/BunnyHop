function genSemaphoreDeclTemplateNode(nodeId) {
  let semaphore = bhCommon.buildDerivative(bhThis, 'dervIdSemaphoreVar', bhUserOpe);
  let templateNode = bhCommon.createBhNode(nodeId, bhUserOpe);
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
