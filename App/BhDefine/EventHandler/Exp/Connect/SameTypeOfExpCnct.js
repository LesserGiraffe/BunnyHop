(function() {
  let currentNodeSection = bhCurrentNode.findDescendantOf('*');
  let newNodeSection = bhNodeToConnect.findDescendantOf('*');
  if (currentNodeSection === null || newNodeSection === null) {
    return false;
  }
  let currentNodeSectionName = String(currentNodeSection.getSymbolName());
  let newNodeSectionName = String(newNodeSection.getSymbolName());
  return currentNodeSectionName === newNodeSectionName;
})();
