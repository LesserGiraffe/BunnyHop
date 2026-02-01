(function() {
  if (bhUtil.isTemplateNode(bhThis)) {
    return true;
  }
  if (bhCauseOfDeletion.isOriginalDeleted()) {
    return false;
  }
  if (bhCauseOfDeletion.isSelectedForDeletion()) {
    bhUtil.moveNodeToAncestor(
        bhThis.findOuterNode(1),
        bhThis,
        bhTargetNodes,
        ['StatVoid'],
        bhUserOpe);
  }
  return true;
})();
