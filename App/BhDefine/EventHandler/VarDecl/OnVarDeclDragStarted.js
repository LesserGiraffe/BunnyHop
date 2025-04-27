(function () {
  if (!bhMouseEvent.isFromSecondaryButton) {
    return;
  }
  let toReconnect = bhCommon.findOuterNotSelected(bhThis);
  if (toReconnect === null) {
    return;
  }
  bhCommon.reconnect(toReconnect, bhThis, [], bhUserOpe);
})();
