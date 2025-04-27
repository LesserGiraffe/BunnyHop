(function() {

  let sectionName = String(bhNodeToConnect.getSymbolName());
  return sectionName === 'NumVar'  ||
       sectionName === 'StrVar'   ||
       sectionName === 'BoolVar'  ||
       sectionName === 'ColorVar' ||
       sectionName === 'SoundVar';
})();
