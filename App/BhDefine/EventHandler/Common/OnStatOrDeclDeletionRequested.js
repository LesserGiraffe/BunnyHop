(function() {
  if (bhCauseOfDeletion.isSelectedForDeletion()) {
    bhCommon.moveNodeToAncestor(
        bhThis.findOuterNode(1),
        bhThis,
        bhTargetNodes,
        ['VarDeclVoid', 'GlobalDataDeclVoid', 'StatVoid'],
        bhUserOpe);
  }
  return true;
})();
