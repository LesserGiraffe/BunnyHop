(function() {
  let voidToExp = {
    'NumExp':   'NumExp',
    'StrExp':   'StrExp',
    'BoolExp':  'BoolExp',
    'ColorExp': 'ColorExp',
    'SoundExp': 'SoundExp',

    'NumExpVoid':   'NumExp',
    'StrExpVoid':   'StrExp',
    'BoolExpVoid':  'BoolExp',
    'ColorExpVoid': 'ColorExp',
    'SoundExpVoid': 'SoundExp',

    'NumListExp':   'NumListExp',
    'StrListExp':   'StrListExp',
    'BoolListExp':  'BoolListExp',
    'ColorListExp': 'ColorListExp',
    'SoundListExp': 'SoundListExp',

    'NumListExpVoid':   'NumListExp',
    'StrListExpVoid':   'StrListExp',
    'BoolListExpVoid':  'BoolListExp',
    'ColorListExpVoid': 'ColorListExp',
    'SoundListExpVoid': 'SoundListExp'
  };

  let newNodeSection = bhNodeToConnect.findDescendantOf('*');
  newNodeSection = (newNodeSection === null) ? '' : String(newNodeSection.getSymbolName());
  if (voidToExp[String(bhCurrentNode.getSymbolName())] === newNodeSection) {
    return true;
  }

  let curNodeSection = bhCurrentNode.findDescendantOf('*');
  curNodeSection = (curNodeSection === null) ? '' : String(curNodeSection.getSymbolName());
  return voidToExp[curNodeSection] === newNodeSection;
})();
