(function() {
  if (bhCauseOfDeletion.isSelectedForDeletion() || bhCauseOfDeletion.isCompileError()) {
    bhCommon.reconnect(
      bhThis.findOuterNode(1), bhThis, bhCandidateNodeList, bhUserOpe);
  }
  return true;
})();
