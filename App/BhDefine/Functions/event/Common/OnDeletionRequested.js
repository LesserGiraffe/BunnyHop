(function() {
  let CauseOfDeletion = net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
  if (bhCauseOfDeletion.equals(CauseOfDeletion.SELECTED_FOR_DELETION)) {
    bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhUserOpe);
  }
  return true;
})();
