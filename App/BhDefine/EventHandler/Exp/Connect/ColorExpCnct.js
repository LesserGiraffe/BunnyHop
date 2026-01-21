(function() {

  let section = bhNodeToConnect.findDescendantOf('*');
  let sectionName = null;
  if (section !== null) {
    sectionName = String(section.getSymbolName());
  }

  return sectionName === 'ColorExp';
})();
