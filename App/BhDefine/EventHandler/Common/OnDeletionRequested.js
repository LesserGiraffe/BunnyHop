(function() {
  if (bhCauseOfDeletion.isSelectedForDeletion() || bhCauseOfDeletion.isCompileError()) {
    bhCommon.moveNodeToAncestor(
        bhThis.findOuterNode(1),
        bhThis,
        bhTargetNodes,
        ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'],
        bhUserOpe);
  }
  return true;
})();
