(function() {
  if (bhCauseOfDeletion.isSelectedForDeletion()) {
    bhUtil.moveNodeToAncestor(
        bhThis.findOuterNode(1),
        bhThis,
        bhTargetNodes,
        ['VarDeclVoid', 'GlobalDataDeclVoid', 'StatVoid'],
        bhUserOpe);
  }
  return true;
})();
