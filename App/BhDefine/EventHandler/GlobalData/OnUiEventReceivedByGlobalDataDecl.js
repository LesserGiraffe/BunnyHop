(function () {
  // ノード操作モード 1 でドラッグしたとき, 以下のノードを抜き出す.
  // - ドラッグされたノード (= A)
  // - A から選択済みの外部ノードを辿って到達できるノード群
  if (bhUiEvent.type.isDragDetected
      && bhUiEvent.isDragAndDropTarget
      && bhUiEvent.isPrimaryButtonPressed
      && bhUiEvent.nodeManipMode.isMode1()) {
    bhCommon.moveNodeToAncestor(
        bhCommon.findOuterNotSelected(bhThis), bhThis, [], ['GlobalDataDeclVoid'], bhUserOpe);
    return;
  }

  if (bhUiEvent.type.isMouseReleased
      && bhUiEvent.isDragAndDropTarget
      && bhUiEvent.isPrimaryButtonPressed
      && bhUiEvent.clickCount === 2
      && bhUiEvent.nodeManipMode.isMode0()) {
    // ダブルクリックしたとき, 外部末尾ノードまで選択する.
    bhCommon.selectToOutermost(bhThis, ['GlobalDataDeclVoid'], bhUserOpe);
  }
})();
