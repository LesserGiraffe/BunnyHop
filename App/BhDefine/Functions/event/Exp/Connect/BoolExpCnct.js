(function() {

  let section = bhNodeToConnect.findSymbolInDescendants('*');
  let sectionName = null;
  if (section !== null) {
    sectionName = String(section.getSymbolName());
  }
  return sectionName === 'BoolExpSctn' ||
       sectionName === 'AnyExpSctn';
})();
