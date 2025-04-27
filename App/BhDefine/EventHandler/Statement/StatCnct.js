(function() {

  let newNodeName = String(bhNodeToConnect.getSymbolName());
  let section = bhNodeToConnect.findDescendantOf('*');
  let sectionName = null;
  if (section !== null) {
    sectionName = String(section.getSymbolName());
  }

  return sectionName === 'StatSctn' ||
       newNodeName === 'BreakStat'||
       newNodeName === 'ContinueStat';
})();
