(function() {

  let newNodeName = String(bhNodeToConnect.getSymbolName());
  let section = bhNodeToConnect.findDescendantOf('*');
  let sectionName = null;
  if (section !== null) {
    sectionName = String(section.getSymbolName());
  }

  return sectionName === 'Statement' ||
       newNodeName === 'BreakStat'||
       newNodeName === 'ContinueStat';
})();
