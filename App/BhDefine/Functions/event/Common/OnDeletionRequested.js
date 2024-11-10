(function() {
  let CauseOfDeletion = net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
  if (bhCauseOfDeletion.equals(CauseOfDeletion.SELECTED_FOR_DELETION)) {
    bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhNodeHandler, bhUserOpe);
  }
  return true;
})();
