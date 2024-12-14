
(function () {
  if (!bhMouseEvent.isFromSecondaryButton) {
    return;
  }
  let toReconnect = bhCommon.findOuterNotSelected(bhThis);
  if (toReconnect === null) {
    return;
  }
  if (bhThis.isRootDangling()) {
    bhCommon.reconnect(toReconnect, bhThis.getLastReplaced(), [], bhCmdProxy, bhUserOpe);
  } else {
    bhCommon.reconnect(toReconnect, null, [], bhCmdProxy, bhUserOpe);
  }  
})();
