(function() {
  if (bhCommon.isTemplateNode(bhThis)) {
    return true;
  }
  if (bhCauseOfDeletion.isOriginalDeleted()) {
    return false;
  }
  if (bhCauseOfDeletion.isSelectedForDeletion()) {
    bhCommon.moveNodeToAncestor(
        bhThis.findOuterNode(1),
        bhThis,
        bhTargetNodes,
        ['StatVoid'],
        bhUserOpe);
  }
  return true;
})();
