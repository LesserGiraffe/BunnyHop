(function() {
  let currentNodeSection = bhCurrentNode.findSymbolInDescendants('*');
  let newNodeSection = bhNodeToConnect.findSymbolInDescendants('*');
  if (currentNodeSection === null || newNodeSection === null) {
    return false;
  }
  let currentNodeSectionName = String(currentNodeSection.getSymbolName());
  let newNodeSectionName = String(newNodeSection.getSymbolName());
  return currentNodeSectionName === newNodeSectionName;
})();
