(function() {
  if (bhCauseOfDeletion.isSelectedForDeletion()) {
    bhCommon.reconnect(
      bhThis.findOuterNode(1), bhThis, bhCandidateNodeList, bhUserOpe);
  }
  return true;
})();
