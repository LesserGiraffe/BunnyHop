(function() {

  let section = bhNodeToConnect.findSymbolInDescendants('*');
  let sectionName = null;
  if (section !== null) {
    sectionName = String(section.getSymbolName());
  }

  return sectionName === 'SoundExpSctn' ||
       sectionName === 'AnyExpSctn';
})();
