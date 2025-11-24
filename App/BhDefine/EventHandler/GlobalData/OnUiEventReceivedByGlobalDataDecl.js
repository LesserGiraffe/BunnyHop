(function () {
  if (!bhIsEventTarget
    || !bhUiEvent.type.isDragDetected
    || !bhUiEvent.isSecondaryButtonDown) {
    return;
  }
  let toReconnect = bhCommon.findOuterNotSelected(bhThis);
  if (toReconnect === null) {
    return;
  }
  bhCommon.reconnect(toReconnect, bhThis, [], bhUserOpe);
})();
