(function () {
  // 右クリックでドラッグしたとき, ノードを 1 つだけ抜き出す
  if (!bhUiEvent.isDragAndDropTarget
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
